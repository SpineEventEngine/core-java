/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.projection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.base.Event;
import io.spine.base.EventContext;
import io.spine.envelope.EventEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.entity.EntityStorageConverter;
import io.spine.server.entity.EventDispatchingRepository;
import io.spine.server.entity.idfunc.EventTargetsFunction;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.stand.Stand;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.AllTenantOperation;
import io.spine.type.EventClass;
import io.spine.type.TypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.validate.Validate.isDefault;

/**
 * Abstract base for repositories managing {@link Projection}s.
 *
 * @param <I> the type of IDs of projections
 * @param <P> the type of projections
 * @param <S> the type of projection state messages
 * @author Alexander Yevsyukov
 */
public abstract class ProjectionRepository<I, P extends Projection<I, S, ?>, S extends Message>
        extends EventDispatchingRepository<I, P, S> {

    /**
     * If {@code true} the projection repository will start the {@linkplain #catchUp() catch up}
     * process after {@linkplain #initStorage(StorageFactory) initialization}.
     */
    private final boolean catchUpAfterStorageInit;

    private final Duration catchUpMaxDuration;

    /** The current status of the repository. */
    private Status status = Status.CREATED;

    /** An underlying entity storage used to store projections. */
    private RecordStorage<I> recordStorage;

    @Nullable
    private BulkWriteOperation<I, P> ongoingOperation;

    /**
     * Creates a {@code ProjectionRepository} and enables catching up after the storage
     * initialization.
     *
     * <p>NOTE: The {@link #catchUp()} will be called automatically after
     * the storage is {@linkplain #initStorage(StorageFactory) initialized}.
     * To override this behavior, please use
     * {@link #ProjectionRepository(boolean, Duration)} constructor.
     *
     */
    protected ProjectionRepository() {
        this(true);
    }

    /**
     * Creates a {@code ProjectionRepository} with catch-up controlled by the passed flag.
     *
     * <p>If {@code catchUpAfterStorageInit} is set to {@code true},
     * the {@link #catchUp()} will be called automatically after the
     * {@link #initStorage(StorageFactory)} call is performed.
     *
     * @param catchUpAfterStorageInit whether the automatic catch-up should be performed after
     *                                storage initialization
     */
    @SuppressWarnings("MethodParameterNamingConvention")
    protected ProjectionRepository(boolean catchUpAfterStorageInit) {
        this(catchUpAfterStorageInit, Duration.getDefaultInstance());
    }

    /**
     * Creates a {@code ProjectionRepository} with the catch-up control flag and maximum duration.
     *
     * <p>If {@code catchUpAfterStorageInit} is set to {@code true}, the {@link #catchUp()}
     * will be called automatically after the storage is {@linkplain #initStorage(StorageFactory)
     * initialized}.
     *
     * <p>If the passed {@code catchUpMaxDuration} is not default, the repository uses a
     * {@link BulkWriteOperation} to accumulate the read projections and then save them as
     * a single bulk of records.
     *
     * <p>The {@code catchUpMaxDuration} represents the maximum time span for the catch-up to read
     * the events and apply them to corresponding projections. If the reading process takes longer
     * then this time, the the read will be automatically finished and all the result projections
     * will be flushed to the storage as a bulk.
     *  @param catchUpAfterStorageInit whether the automatic catch-up should be performed after
     *                                storage initialization
     * @param catchUpMaxDuration      the maximum duration of the catch-up
     */
    @SuppressWarnings("MethodParameterNamingConvention")
    protected ProjectionRepository(boolean catchUpAfterStorageInit,
                                   Duration catchUpMaxDuration) {
        super(EventDispatchingRepository.<I>producerFromContext());
        this.catchUpAfterStorageInit = catchUpAfterStorageInit;
        this.catchUpMaxDuration = checkNotNull(catchUpMaxDuration);
    }

    /** Obtains {@link EventStore} from which to get events during catch-up. */
    EventStore getEventStore() {
        return getBoundedContext().getEventBus()
                                  .getEventStore();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to open the method to the {@code beam} package.
     */
    @Override
    protected EventTargetsFunction<I, Message> getIdSetFunction() {
        return super.getIdSetFunction();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to open the method to the package.
     */
    @Override
    protected Class<I> getIdClass() {
        return super.getIdClass();
    }

    /**
     * Obtains event filters for event classes handled by projections of this repository.
     */
    Set<EventFilter> createEventFilters() {
        final ImmutableSet.Builder<EventFilter> builder = ImmutableSet.builder();
        final Set<EventClass> eventClasses = getMessageClasses();
        for (EventClass eventClass : eventClasses) {
            final String typeName = TypeName.of(eventClass.value())
                                            .value();
            builder.add(EventFilter.newBuilder()
                                   .setEventType(typeName)
                                   .build());
        }
        return builder.build();
    }

    @VisibleForTesting
    static Timestamp nullToDefault(@Nullable Timestamp timestamp) {
        return timestamp == null
               ? Timestamp.getDefaultInstance()
               : timestamp;
    }

    /** Opens access to the {@link BoundedContext} of the repository to the package. */
    BoundedContext boundedContext() {
        return getBoundedContext();
    }

    /**
     * Obtains the {@code Stand} from the {@code BoundedContext} of this repository.
     */
    protected final Stand getStand() {
        return getBoundedContext().getStand();
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    protected Status getStatus() {
        return status;
    }

    protected void setStatus(Status status) {
        this.status = status;
    }

    protected boolean isOnline() {
        return this.status == Status.ONLINE;
    }

    /**
     * {@inheritDoc}
     * <p>Overrides to open the method to the package.
     */
    @Override
    protected EntityStorageConverter<I, P, S> entityConverter() {
        return super.entityConverter();
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod" /* We do not call super.createStorage() because
                       we create a specific type of a storage, not a regular entity storage created
                       in the parent. */)
    protected RecordStorage<I> createStorage(StorageFactory factory) {
        final Class<P> projectionClass = getEntityClass();
        final ProjectionStorage<I> projectionStorage =
                factory.createProjectionStorage(projectionClass);
        this.recordStorage = projectionStorage.recordStorage();
        return projectionStorage;
    }

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException if the storage is null
     */
    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    protected RecordStorage<I> recordStorage() {
        return checkStorage(recordStorage);
    }

    /** {@inheritDoc} */
    @Override
    public void initStorage(StorageFactory factory) {
        super.initStorage(factory);
        setStatus(Status.STORAGE_ASSIGNED);

        if (catchUpAfterStorageInit) {
            log().debug("Storage assigned. {} is starting to catch-up", getClass());
            catchUp();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        super.close();
        setStatus(Status.CLOSED);
    }

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException if the storage is null
     */
    protected ProjectionStorage<I> projectionStorage() {
        @SuppressWarnings("unchecked") // OK as we control the creation in createStorage().
        final ProjectionStorage<I> storage = (ProjectionStorage<I>) getStorage();
        return storage;
    }

    /** {@inheritDoc} */
    @Override
    public Set<EventClass> getMessageClasses() {
        final Class<? extends Projection> projectionClass = getEntityClass();
        final Set<EventClass> result = Projection.TypeInfo.getEventClasses(projectionClass);
        return result;
    }

    /**
     * Dispatches the passed event to corresponding {@link Projection}s if the repository is
     * in {@link Status#ONLINE}.
     *
     * <p>If the repository in another status the event is not dispatched. This is needed to
     * preserve the chronological sequence of events delivered to the repository, while it's
     * updating projections using the {@link #catchUp()} call.
     *
     * <p>The ID of the projection must be specified as the first property of the passed event.
     *
     * <p>If there is no stored projection with the ID from the event, a new projection is created
     * and stored after it handles the passed event.
     *
     * @param eventEnvelope the event to dispatch packed into an envelope
     * @see #catchUp()
     * @see Projection#handle(Message, EventContext)
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod") // We call indirectly via `internalDispatch()`.
    @Override
    public void dispatch(EventEnvelope eventEnvelope) {
        if (!isOnline()) {
            log().trace("Ignoring event {} while repository is not in {} status",
                        eventEnvelope.getOuterObject(),
                        Status.ONLINE);
            return;
        }

        internalDispatch(eventEnvelope);
    }

    @Override
    protected void dispatchToEntity(I id, Message eventMessage, EventContext context) {
        final P projection = findOrCreate(id);
        final ProjectionTransaction<I, ?, ?> tx =
                ProjectionTransaction.start((Projection<I, ?, ?>) projection);
        projection.handle(eventMessage, context);
        tx.commit();

        if (projection.isChanged()) {
            final Timestamp eventTime = context.getTimestamp();

            if (isBulkWriteInProgress()) {
                storePostponed(projection, eventTime);
            } else {
                storeNow(projection, eventTime);
            }

            getStand().post(projection, context.getCommandContext());
        }
    }

    /**
     * Dispatches the passed event to projections without checking the status.
     */
    private void internalDispatch(EventEnvelope envelope) {
        super.dispatch(envelope);
    }

    private void storeNow(P projection, Timestamp eventTime) {
        store(projection);
        projectionStorage().writeLastHandledEventTime(eventTime);
    }

    private void storePostponed(P projection, Timestamp eventTime) {
        checkState(ongoingOperation != null,
                   "Unable to store postponed projection: ongoingOperation is null.");
        ongoingOperation.storeProjection(projection);
        ongoingOperation.storeLastHandledEventTime(eventTime);
    }

    /**
     * Store a number of projections at a time.
     *
     * @param projections {@link Projection} bulk to store
     */
    @VisibleForTesting
    void store(Collection<P> projections) {
        final RecordStorage<I> storage = recordStorage();
        final Map<I, EntityRecordWithColumns> records =
                Maps.newHashMapWithExpectedSize(projections.size());
        for (P projection : projections) {
            final I id = projection.getId();
            final EntityRecordWithColumns record = toRecord(projection);
            records.put(id, record);
        }
        storage.write(records);
    }

    /**
     * Updates projections from the event stream obtained from {@code EventStore}.
     */
    public void catchUp() {
        setStatus(Status.CATCHING_UP);

        /*
            Uncomment the below line and comment the one after to switch between regular and
            Beam-based catch-up procedures.
        */
//               allTenantOpCatchup();
        BeamCatchUp.forAllTenants(this);

        completeCatchUp();
        logCatchUpComplete();
    }

    @Internal
    public void writeLastHandledEventTime(Timestamp timestamp) {
        projectionStorage().writeLastHandledEventTime(timestamp);
    }

    @Internal
    public Timestamp readLastHandledEventTime() {
        return projectionStorage().readLastHandledEventTime();
    }

    @SuppressWarnings("unused")
        // A direct catch-up impl. Do not delete until Beam-based catch-up is finished.
    private void allTenantOpCatchup() {
        final AllTenantOperation op = new AllTenantOperation(getBoundedContext().getTenantIndex()) {

            private final EventStore eventStore = getBoundedContext().getEventBus()
                                                                     .getEventStore();
            private final Set<EventFilter> eventFilters = getEventFilters();

            @Override
            public void run() {
                // Get the timestamp of the last event. This also ensures we have the storage.
                final Timestamp timestamp = nullToDefault(
                        projectionStorage().readLastHandledEventTime());

                final EventStreamQuery.Builder builder = EventStreamQuery.newBuilder();
                if (!isDefault(timestamp)) {
                    builder.setAfter(timestamp);
                }
                builder.addAllFilter(eventFilters)
                       .build();
                final EventStreamQuery query = builder.build();
                eventStore.read(query, new EventStreamObserver(ProjectionRepository.this));
            }
        };
        op.execute();
    }

    EventStreamQuery createStreamQuery() {
        final Set<EventFilter> eventFilters = createEventFilters();

        // Get the timestamp of the last event. This also ensures we have the storage.
        final Timestamp timestamp = nullToDefault(
                projectionStorage().readLastHandledEventTime());
        return EventStreamQuery.newBuilder()
                               .setAfter(timestamp)
                               .addAllFilter(eventFilters)
                               .build();
    }

    private void completeCatchUp() {
        setOnline();
    }

    private void logCatchUpComplete() {
        if (log().isInfoEnabled()) {
            final Class<? extends ProjectionRepository> repositoryClass = getClass();
            log().info("{} catch-up complete", repositoryClass.getName());
        }
    }

    /**
     * Obtains event filters for event classes handled by projections of this repository.
     */
    private Set<EventFilter> getEventFilters() {
        final ImmutableSet.Builder<EventFilter> builder = ImmutableSet.builder();
        final Set<EventClass> eventClasses = getMessageClasses();
        for (EventClass eventClass : eventClasses) {
            final String typeName = TypeName.of(eventClass.value())
                                            .value();
            builder.add(EventFilter.newBuilder()
                                   .setEventType(typeName)
                                   .build());
        }
        return builder.build();
    }

    /**
     * Sets the repository online bypassing the catch-up from the {@code EventStore}.
     */
    public void setOnline() {
        setStatus(Status.ONLINE);
    }

    private boolean isBulkWriteInProgress() {
        return ongoingOperation != null
               && ongoingOperation.isInProgress();
    }

    private boolean isBulkWriteRequired() {
        return !catchUpMaxDuration.equals(
                catchUpMaxDuration.getDefaultInstanceForType());
    }

    private BulkWriteOperation startBulkWrite(Duration expirationTime) {
        final BulkWriteOperation<I, P> bulkWriteOperation = new BulkWriteOperation<>(
                expirationTime,
                new PendingDataFlushTask<>(this));
        this.ongoingOperation = bulkWriteOperation;
        return bulkWriteOperation;
    }

    /**
     * The enumeration of statuses in which a Projection Repository can be during its lifecycle.
     */
    protected enum Status {

        /**
         * The repository instance has been created.
         *
         * <p>In this status the storage is not yet assigned.
         */
        CREATED,

        /**
         * A storage has been assigned to the repository.
         */
        STORAGE_ASSIGNED,

        /**
         * The repository is getting events from EventStore and builds projections.
         */
        CATCHING_UP,

        /**
         * The repository completed the catch-up process.
         */
        ONLINE,

        /**
         * The repository is closed and no longer accept events.
         */
        CLOSED
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ProjectionRepository.class);
    }

    /**
     * The stream observer which redirects events from {@code EventStore} to
     * the associated {@code ProjectionRepository}.
     */
    private static class EventStreamObserver implements StreamObserver<Event> {

        private final ProjectionRepository repository;
        private BulkWriteOperation operation;

        private EventStreamObserver(ProjectionRepository repository) {
            this.repository = repository;
        }

        @Override
        public void onNext(Event event) {
            if (repository.status != Status.CATCHING_UP) {
                // The repository is catching up.
                // Skip all the events and perform {@code #onCompleted}.
                log().info("The catch-up is completing due to an overtime. Skipping event {}.",
                           event);
                return;
            }

            if (repository.isBulkWriteRequired()) {
                if (!repository.isBulkWriteInProgress()) {
                    operation = repository.startBulkWrite(repository.catchUpMaxDuration);
                }
                // `flush` the data if the operation expires.
                operation.checkExpiration();

                if (!operation.isInProgress()) { // Expired. End now
                    return;
                }
            }

            repository.internalDispatch(EventEnvelope.of(event));
        }

        @Override
        public void onError(Throwable throwable) {
            log().error("Error obtaining events from EventStore.", throwable);
        }

        @Override
        public void onCompleted() {
            if (repository.isBulkWriteInProgress()) {
                operation.complete();
            }
        }
    }

    /**
     * Implementation of the {@link BulkWriteOperation.FlushCallback} for storing
     * the projections and the last handled event time into the {@link ProjectionRepository}.
     */
    private static class PendingDataFlushTask<I, P extends Projection<I, S, ?>, S extends Message>
            implements BulkWriteOperation.FlushCallback<P> {

        private final ProjectionRepository<I, P, S> repository;

        private PendingDataFlushTask(ProjectionRepository<I, P, S> repository) {
            this.repository = repository;
        }

        @Override
        public void onFlushResults(Set<P> projections, Timestamp lastHandledEventTime) {
            repository.store(projections);
            repository.projectionStorage()
                      .writeLastHandledEventTime(lastHandledEventTime);
            repository.completeCatchUp();
        }
    }

    /*
     * Beam support
     *************************/

    @Override
    public ProjectionRepositoryIO<I, P, S> getIO() {
        return new ProjectionRepositoryIO<>(projectionStorage().getIO(getIdClass()),
                                            entityConverter());
    }
}
