/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.protobuf.TypeToClassMap;
import org.spine3.server.BoundedContext;
import org.spine3.server.entity.EntityRepository;
import org.spine3.server.event.EventDispatcher;
import org.spine3.server.event.EventFilter;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.ProjectionStorage;
import org.spine3.server.storage.Storage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.type.EventClass;
import org.spine3.type.TypeName;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Abstract base for repositories managing {@link Projection}s.
 *
 * @param <I> the type of IDs of projections
 * @param <P> the type of projections
 * @param <M> the type of projection state messages
 * @author Alexander Yevsyukov
 */
public abstract class ProjectionRepository<I, P extends Projection<I, M>, M extends Message>
        extends EntityRepository<I, P, M> implements EventDispatcher {

    /** The enumeration of statuses in which a Projection Repository can be during its lifecycle. */
    private enum Status {

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
    private EntityStorage<I> entityStorage;

    protected ProjectionRepository(BoundedContext boundedContext) {
        super(boundedContext);
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
    @SuppressWarnings("RefusedBequest") /* We do not call super.createStorage() because we create a specific
                                           type of a storage, not regular entity storage created in the parent. */
    protected Storage createStorage(StorageFactory factory) {
        final Class<P> projectionClass = getEntityClass();
        final ProjectionStorage<I> projectionStorage = factory.createProjectionStorage(projectionClass);
        this.entityStorage = projectionStorage.getEntityStorage();
        return projectionStorage;
    }

    /** {@inheritDoc} */
    @Override
    public void initStorage(StorageFactory factory) {
        super.initStorage(factory);
        setStatus(Status.STORAGE_ASSIGNED);
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
    @SuppressWarnings("RefusedBequest")
    protected EntityStorage<I> entityStorage() {
        return checkStorage(entityStorage);
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
        final Set<Class<? extends Message>> eventClasses = Projection.getEventClasses(projectionClass);
        final Set<EventClass> result = EventClass.setOf(eventClasses);
        return result;
    }

    /**
     * Obtains the ID of the event producer from the passed event context and
     * casts it to the type of index used by this repository.
     *
     * @param event the event message. This parameter is not used by default implementation.
     *              Override to provide custom logic of ID generation.
     * @param context the event context
     */
    @SuppressWarnings("UnusedParameters") // Overriding methods may want to use the `event` parameter.
    protected I getEntityId(Message event, EventContext context) {
        final I id = Events.getProducer(context);
        return id;
    }

    /**
     * Loads or creates a projection by the passed ID.
     *
     * <p>The projection is created if there was no projection with such ID stored before.
     *
     * @param id the ID of the projection to load
     * @return loaded or created projection instance
     */
    @Nonnull
    @Override
    public P load(I id) {
        P projection = super.load(id);
        if (projection == null) {
            projection = create(id);
        }
        return projection;
    }

    /**
     * Dispatches the passed event to corresponding {@link Projection} if the repository is
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
    @Override
    public void dispatch(Event event) {
        if (!isOnline()) {
            if (log().isTraceEnabled()) {
                log().trace("Ignoring event {} while repository is not in {} status", event, Status.ONLINE);
            }
            return;
        }

        internalDispatch(event);
    }

    /**
     * Dispatches event to a projection without checking the status of the repository.
     *
     * @param event the event to dispatch
     */
    /* package */ void internalDispatch(Event event) {
        final Message eventMessage = Events.getMessage(event);
        final EventContext context = event.getContext();
        final I id = getEntityId(eventMessage, context);
        final P projection = load(id);
        projection.handle(eventMessage, context);
        store(projection);
        final ProjectionStorage<I> storage = projectionStorage();
        final Timestamp eventTime = context.getTimestamp();
        storage.writeLastHandledEventTime(eventTime);
    }

    /** Updates projections from the event stream obtained from {@code EventStore}. */
    public void catchUp() {
        // Get the timestamp of the last event. This also ensures we have the storage.
        final Timestamp timestamp = projectionStorage().readLastHandledEventTime();
        final EventStore eventStore = getBoundedContext().getEventBus().getEventStore();

        final Set<EventFilter> eventFilters = getEventFilters();

        final EventStreamQuery query = EventStreamQuery.newBuilder()
               .setAfter(timestamp == null ? Timestamp.getDefaultInstance() : timestamp)
               .addAllFilter(eventFilters)
               .build();

        setStatus(Status.CATCHING_UP);
        eventStore.read(query, new EventStreamObserver(this));
    }

    /** Obtains event filters for event classes handled by projections of this repository. */
    private Set<EventFilter> getEventFilters() {
        final ImmutableSet.Builder<EventFilter> builder = ImmutableSet.builder();
        final Set<EventClass> eventClasses = getEventClasses();
        for (EventClass eventClass : eventClasses) {
            final TypeName typeName = TypeToClassMap.get(eventClass.getClassName());
            builder.add(EventFilter.newBuilder()
                                   .setEventType(typeName.value())
                                   .build());
        }
        return builder.build();
    }

    /** Sets the repository only bypassing updating from {@code EventStore}. */
    public void setOnline() {
        setStatus(Status.ONLINE);
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ProjectionRepository.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    /**
     * The stream observer passed to Event Store, which passes obtained events
     * to the associated Projection Repository.
     */
    private static class EventStreamObserver implements StreamObserver<Event> {

        private final ProjectionRepository projectionRepository;

        /* package */ EventStreamObserver(ProjectionRepository projectionRepository) {
            this.projectionRepository = projectionRepository;
        }

        @Override
        public void onNext(Event event) {
            projectionRepository.internalDispatch(event);
        }

        @Override
        public void onError(Throwable throwable) {
            log().error("Error obtaining events from EventStore.", throwable);
        }

        @Override
        public void onCompleted() {
            projectionRepository.setStatus(Status.ONLINE);
            if (log().isInfoEnabled()) {
                final Class<? extends ProjectionRepository> repositoryClass = projectionRepository.getClass();
                log().info("{} catch-up complete", repositoryClass.getName());
            }
        }
    }
}
