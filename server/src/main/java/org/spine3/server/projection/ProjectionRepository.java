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

package org.spine3.server.projection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.server.BoundedContext;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.EventDispatchingRepository;
import org.spine3.server.event.EventFilter;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.server.stand.StandFunnel;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.storage.Storage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.type.EventClass;
import org.spine3.type.TypeName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Abstract base for repositories managing {@link Projection}s.
 *
 * @param <I> the type of IDs of projections
 * @param <P> the type of projections
 * @param <S> the type of projection state messages
 * @author Alexander Yevsyukov
 */
public abstract class ProjectionRepository<I, P extends Projection<I, S>, S extends Message>
        extends EventDispatchingRepository<I, P, S> {

    /** The enumeration of statuses in which a Projection Repository can be during its lifecycle. */
    protected enum Status {

        /**
         * The repository instance has been created.
         *
         * <p>In this status the storage is not yet assigned.
         */
        CREATED,

        /** A storage has been assigned to the repository. */
        STORAGE_ASSIGNED,

        /** The repository is getting events from EventStore and builds projections. */
        CATCHING_UP,

        /** The repository completed the catch-up process. */
        ONLINE,

        /** The repository is closed and no longer accept events. */
        CLOSED
    }

    /** The current status of the repository. */
    private Status status = Status.CREATED;

    /** An underlying entity storage used to store projections. */
    private RecordStorage<I> recordStorage;

    /** An instance of {@link StandFunnel} to be informed about state updates */
    private final StandFunnel standFunnel;

    /** If {@code true} the projection will {@link #catchUp()} after initialization. */
    private final boolean catchUpAfterStorageInit;

    private final Duration catchUpMaxDuration;

    @Nullable
    private BulkWriteOperation<I, P> ongoingOperation;

    /**
     * Creates a {@code ProjectionRepository} for the given {@link BoundedContext} instance and
     * enables catching up after the storage initialization.
     *
     * <p>NOTE: The {@link #catchUp()} will be called automatically after
     * the storage is {@linkplain #initStorage(StorageFactory) initialized}.
     * To override this behavior, please use
     * {@link #ProjectionRepository(BoundedContext, boolean, Duration)} constructor.
     *
     * @param boundedContext the target {@code BoundedContext}
     */
    protected ProjectionRepository(BoundedContext boundedContext) {
        this(boundedContext, true);
    }

    /**
     * Creates a {@code ProjectionRepository} for the given {@link BoundedContext} instance.
     *
     * <p>If {@code catchUpAfterStorageInit} is set to {@code true},
     * the {@link #catchUp()} will be called automatically after the
     * {@link #initStorage(StorageFactory)} call is performed.
     *
     * @param boundedContext          the target {@code BoundedContext}
     * @param catchUpAfterStorageInit whether the automatic catch-up should be performed after
     *                                storage initialization
     */
    @SuppressWarnings("MethodParameterNamingConvention")
    protected ProjectionRepository(BoundedContext boundedContext,
                                   boolean catchUpAfterStorageInit) {
        this(boundedContext, catchUpAfterStorageInit, Duration.getDefaultInstance());
    }

    /**
     * Creates a {@code ProjectionRepository} for the given {@link BoundedContext} instance.
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
     *
     * @param boundedContext          the target {@code BoundedContext}
     * @param catchUpAfterStorageInit whether the automatic catch-up should be performed after
     *                                storage initialization
     * @param catchUpMaxDuration      the maximum duration of the catch-up
     */
    @SuppressWarnings("MethodParameterNamingConvention")
    protected ProjectionRepository(BoundedContext boundedContext,
                                   boolean catchUpAfterStorageInit,
                                   Duration catchUpMaxDuration) {
        super(boundedContext, EventDispatchingRepository.<I>producerFromContext());
        this.standFunnel = boundedContext.getStandFunnel();
        this.catchUpAfterStorageInit = catchUpAfterStorageInit;
        this.catchUpMaxDuration = checkNotNull(catchUpMaxDuration);
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

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod" /* We do not call super.createStorage() because
                       we create a specific type of a storage, not a regular entity storage created in the parent. */)
    protected Storage<I, ?> createStorage(StorageFactory factory) {
        final Class<P> projectionClass = getEntityClass();
        final ProjectionStorage<I> projectionStorage =
                factory.createProjectionStorage(projectionClass);
        this.recordStorage = projectionStorage.recordStorage();
        return projectionStorage;
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
    public void close() throws Exception {
        super.close();
        setStatus(Status.CLOSED);
    }

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException if the storage is null
     */
    @Override
    @Nonnull
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    protected RecordStorage<I> recordStorage() {
        return checkStorage(recordStorage);
    }

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException if the storage is null
     */
    @Nonnull
    protected ProjectionStorage<I> projectionStorage() {
        @SuppressWarnings("unchecked") // It is safe to cast as we control the creation in createStorage().
        final ProjectionStorage<I> storage = (ProjectionStorage<I>) getStorage();
        return checkStorage(storage);
    }

    /** {@inheritDoc} */
    @Override
    public Set<EventClass> getEventClasses() {
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
     * @param event the event to dispatch
     * @see #catchUp()
     * @see Projection#handle(Message, EventContext)
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod") // We call indirectly via `internalDispatch()`.
    @Override
    public void dispatch(Event event) {
        if (!isOnline()) {
            log().trace("Ignoring event {} while repository is not in {} status",
                        event,
                        Status.ONLINE);
            return;
        }

        internalDispatch(event);
    }

    /**
     * Dispatches the passed event to projections without checking the status.
     */
    private void internalDispatch(Event event) {
        super.dispatch(event);
    }

    @Override
    protected void dispatchToEntity(I id, Message eventMessage, EventContext context) {
        final P projection = loadOrCreate(id);
        projection.handle(eventMessage, context);

        final Timestamp eventTime = context.getTimestamp();

        if (isBulkWriteInProgress()) {
            storePostponed(projection, eventTime);
        } else {
            storeNow(projection, eventTime);
        }

        standFunnel.post(projection);
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
        final Map<I, EntityRecord> records = Maps.newHashMapWithExpectedSize(projections.size());
        for (P projection : projections) {
            final I id = projection.getId();
            final EntityRecord record = toRecord(projection);
            records.put(id, record);
        }
        storage.write(records);
    }

    /**
     * Updates projections from the event stream obtained from {@code EventStore}.
     */
    public void catchUp() {
        // Get the timestamp of the last event. This also ensures we have the storage.
        final Timestamp timestamp = nullToDefault(projectionStorage().readLastHandledEventTime());
        final EventStore eventStore = getBoundedContext().getEventBus()
                                                         .getEventStore();

        final Set<EventFilter> eventFilters = getEventFilters();

        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .setAfter(timestamp)
                                                       .addAllFilter(eventFilters)
                                                       .build();

        setStatus(Status.CATCHING_UP);
        eventStore.read(query, new EventStreamObserver(this));
    }

    private static Timestamp nullToDefault(@Nullable Timestamp timestamp) {
        return timestamp == null ? Timestamp.getDefaultInstance() : timestamp;
    }

    /**
     * Obtains event filters for event classes handled by projections of this repository.
     */
    private Set<EventFilter> getEventFilters() {
        final ImmutableSet.Builder<EventFilter> builder = ImmutableSet.builder();
        final Set<EventClass> eventClasses = getEventClasses();
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

    private void completeCatchUp() {
        setOnline();
    }

    private boolean isBulkWriteInProgress() {
        return ongoingOperation != null
                && ongoingOperation.isInProgress();
    }

    private boolean isBulkWriteRequired() {
        return !catchUpMaxDuration.equals(
                catchUpMaxDuration.getDefaultInstanceForType());
    }

    /**
     * The stream observer which redirects events from {@code EventStore} to
     * the associated {@code ProjectionRepository}.
     */
    private static class EventStreamObserver implements StreamObserver<Event> {

        private final ProjectionRepository projectionRepository;
        private BulkWriteOperation operation;

        EventStreamObserver(ProjectionRepository projectionRepository) {
            this.projectionRepository = projectionRepository;
        }

        @Override
        public void onNext(Event event) {
            if (projectionRepository.status != Status.CATCHING_UP) {
                // The repository is catching up.
                // Skip all the events and perform {@code #onCompleted}.
                log().info("The catch-up is completing due to an overtime. Skipping event {}.",
                           event);
                return;
            }

            if (projectionRepository.isBulkWriteRequired()) {
                if (!projectionRepository.isBulkWriteInProgress()) {
                    operation = projectionRepository.startBulkWrite(
                            projectionRepository.catchUpMaxDuration);
                }
                // `flush` the data if the operation expires.
                operation.checkExpiration();

                if (!operation.isInProgress()) { // Expired. End now
                    return;
                }
            }

            projectionRepository.internalDispatch(event);
        }

        @Override
        public void onError(Throwable throwable) {
            log().error("Error obtaining events from EventStore.", throwable);
        }

        @Override
        public void onCompleted() {
            if (projectionRepository.isBulkWriteInProgress()) {
                operation.complete();
            }
            projectionRepository.completeCatchUp();
            logCatchUpComplete();
        }

        private void logCatchUpComplete() {
            if (log().isInfoEnabled()) {
                final Class<? extends ProjectionRepository> repositoryClass =
                        projectionRepository.getClass();
                log().info("{} catch-up complete", repositoryClass.getName());
            }
        }
    }

    private BulkWriteOperation startBulkWrite(Duration expirationTime) {
        final BulkWriteOperation<I, P> bulkWriteOperation = new BulkWriteOperation<>(
                expirationTime,
                new PendingDataFlushTask<>(this));
        this.ongoingOperation = bulkWriteOperation;
        return bulkWriteOperation;
    }

    /**
     * Implementation of the {@link BulkWriteOperation.FlushCallback} for storing
     * the projections and the last handled event time into the {@link ProjectionRepository}.
     */
    private static class PendingDataFlushTask<P extends Projection<?, ?>>
            implements BulkWriteOperation.FlushCallback<P> {

        private final ProjectionRepository<?, P, ?> projectionRepository;

        private PendingDataFlushTask(ProjectionRepository<?, P, ?> projectionRepository) {
            this.projectionRepository = projectionRepository;
        }

        @Override
        public void onFlushResults(Set<P> projections, Timestamp lastHandledEventTime) {
            projectionRepository.store(projections);
            projectionRepository.projectionStorage()
                                .writeLastHandledEventTime(lastHandledEventTime);
            projectionRepository.completeCatchUp();
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ProjectionRepository.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
