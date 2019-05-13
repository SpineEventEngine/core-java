/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.server.entity.EventDispatchingRepository;
import io.spine.server.entity.StorageConverter;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.event.model.SubscriberMethod;
import io.spine.server.event.store.EventStore;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.projection.model.ProjectionClass;
import io.spine.server.stand.Stand;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.option.EntityOption.Kind.PROJECTION;
import static io.spine.server.projection.model.ProjectionClass.asProjectionClass;
import static io.spine.server.route.EventRoute.byProducerId;
import static io.spine.server.route.EventRoute.ignoreEntityUpdates;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Abstract base for repositories managing {@link Projection}s.
 *
 * @param <I> the type of IDs of projections
 * @param <P> the type of projections
 * @param <S> the type of projection state messages
 */
public abstract class ProjectionRepository<I, P extends Projection<I, S, ?>, S extends Message>
        extends EventDispatchingRepository<I, P, S> {

    /** An underlying entity storage used to store projections. */
    private RecordStorage<I> recordStorage;

    /**
     * Creates a new {@code ProjectionRepository}.
     */
    protected ProjectionRepository() {
        super(ignoreEntityUpdates(byProducerId()));
    }

    @VisibleForTesting
    static Timestamp nullToDefault(@Nullable Timestamp timestamp) {
        return timestamp == null
               ? Timestamp.getDefaultInstance()
               : timestamp;
    }

    /** Obtains {@link EventStore} from which to get events during catch-up. */
    EventStore eventStore() {
        return boundedContext()
                .eventBus()
                .eventStore();
    }

    /** Obtains class information of projection managed by this repository. */
    private ProjectionClass<P> projectionClass() {
        return (ProjectionClass<P>) entityModelClass();
    }

    @Internal
    @Override
    protected final ProjectionClass<P> toModelClass(Class<P> cls) {
        return asProjectionClass(cls);
    }

    @Override
    public P create(I id) {
        P projection = super.create(id);
        lifecycleOf(id).onEntityCreated(PROJECTION);
        return projection;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ensures there is at least one event subscriber method (external or domestic) declared
     * by the class of the projection. Throws an {@code IllegalStateException} otherwise.
     */
    @Override
    @OverridingMethodsMustInvokeSuper
    public void onRegistered() {
        super.onRegistered();
        ensureDispatchesEvents();
        subscribeToSystemEvents();
    }

    private void ensureDispatchesEvents() {
        boolean noEventSubscriptions = !dispatchesEvents();
        if (noEventSubscriptions) {
            boolean noExternalSubscriptions = !dispatchesExternalEvents();
            if (noExternalSubscriptions) {
                throw newIllegalStateException(
                        "Projections of the repository `%s` have neither domestic nor external " +
                                "event subscriptions.", this);
            }
        }
    }

    private void subscribeToSystemEvents() {
        ProjectionSystemEventWatcher<I> systemSubscriber =
                new ProjectionSystemEventWatcher<>(this);
        systemSubscriber.registerIn(boundedContext());
    }

    @Override
    public Optional<ExternalMessageDispatcher<I>> createExternalDispatcher() {
        if (!dispatchesExternalEvents()) {
            return Optional.empty();
        }
        return Optional.of(new ProjectionExternalEventDispatcher());
    }

    /**
     * Obtains event filters for event classes handled by projections of this repository.
     */
    private Set<EventFilter> createEventFilters() {
        ImmutableSet.Builder<EventFilter> builder = ImmutableSet.builder();
        Set<EventClass> eventClasses = messageClasses();
        for (EventClass eventClass : eventClasses) {
            String typeName = TypeName.of(eventClass.value())
                                      .value();
            builder.add(EventFilter.newBuilder()
                                   .setEventType(typeName)
                                   .build());
        }
        return builder.build();
    }

    /**
     * Obtains the {@code Stand} from the {@code BoundedContext} of this repository.
     */
    protected final Stand stand() {
        return boundedContext().stand();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to open the method to the package.
     */
    @Override
    protected StorageConverter<I, P, S> storageConverter() {
        return super.storageConverter();
    }

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException if the storage is null
     */
    @Override
    protected RecordStorage<I> recordStorage() {
        return checkStorage(recordStorage);
    }

    @Override
    protected RecordStorage<I> createStorage(StorageFactory factory) {
        Class<P> projectionClass = entityClass();
        ProjectionStorage<I> projectionStorage = factory.createProjectionStorage(projectionClass);
        this.recordStorage = projectionStorage.recordStorage();
        return projectionStorage;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose the method to the package.
     */
    @Override
    protected P findOrCreate(I id) {
        return super.findOrCreate(id);
    }

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException if the storage is null
     */
    protected ProjectionStorage<I> projectionStorage() {
        @SuppressWarnings("unchecked") /* OK as we control the creation in createStorage(). */
        ProjectionStorage<I> storage = (ProjectionStorage<I>) storage();
        return storage;
    }

    @Override
    public Set<EventClass> messageClasses() {
        return projectionClass().domesticEvents();
    }

    @Override
    public Set<EventClass> externalEventClasses() {
        return projectionClass().externalEvents();
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public boolean canDispatch(EventEnvelope event) {
        Optional<SubscriberMethod> subscriber = projectionClass().subscriberOf(event);
        return subscriber.isPresent();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends a system command to dispatch the given event to a subscriber.
     */
    @Override
    protected void dispatchTo(I id, Event event) {
        lifecycleOf(id).onDispatchEventToSubscriber(event);
    }

    /**
     * Dispatches the given event to the projection with the given ID.
     *
     * @param id
     *         the ID of the target projection
     * @param event
     *         the event to dispatch
     */
    @Internal
    protected void dispatchNowTo(I id, EventEnvelope event) {
        ProjectionEndpoint<I, P> endpoint = ProjectionEndpoint.of(this, event);
        endpoint.dispatchTo(id);
    }

    @Internal
    public void writeLastHandledEventTime(Timestamp timestamp) {
        checkNotNull(timestamp);
        projectionStorage().writeLastHandledEventTime(timestamp);
    }

    @Internal
    public Timestamp readLastHandledEventTime() {
        Timestamp timestamp = projectionStorage().readLastHandledEventTime();
        return nullToDefault(timestamp);
    }

    @VisibleForTesting
    EventStreamQuery createStreamQuery() {
        Set<EventFilter> eventFilters = createEventFilters();

        // Gets the timestamp of the last event. This also ensures we have the storage.
        Timestamp timestamp = readLastHandledEventTime();
        EventStreamQuery.Builder builder = EventStreamQuery
                .newBuilder()
                .setAfter(timestamp)
                .addAllFilter(eventFilters);
        return builder.build();
    }

    /**
     * An implementation of an external message dispatcher feeding external events
     * to {@code Projection} instances.
     */
    private class ProjectionExternalEventDispatcher extends AbstractExternalEventDispatcher {

        @Override
        public Set<ExternalMessageClass> messageClasses() {
            Set<EventClass> eventClasses = projectionClass().externalEvents();
            return ExternalMessageClass.fromEventClasses(eventClasses);
        }

        @Override
        public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
            checkNotNull(envelope);
            checkNotNull(exception);
            logError("Error dispatching external event (class: `%s`, id: `%s`)" +
                             " to projection with state `%s`.",
                     envelope, exception);
        }
    }
}
