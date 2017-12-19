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
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.annotation.SPI;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.entity.EntityStorageConverter;
import io.spine.server.entity.EventDispatchingRepository;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.model.Model;
import io.spine.server.route.EventProducers;
import io.spine.server.route.EventRouting;
import io.spine.server.stand.Stand;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.type.TypeName;

import javax.annotation.Nullable;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

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

    /** An underlying entity storage used to store projections. */
    private RecordStorage<I> recordStorage;

    /**
     * Creates a new {@code ProjectionRepository}.
     */
    protected ProjectionRepository() {
        super(EventProducers.<I>fromContext());
    }

    /** Obtains {@link EventStore} from which to get events during catch-up. */
    EventStore getEventStore() {
        return getBoundedContext().getEventBus()
                                  .getEventStore();
    }

    /** Obtains class information of projection managed by this repository. */
    @SuppressWarnings("unchecked") // The cast is ensured by generic parameters of the repository.
    private ProjectionClass<P> projectionClass() {
        return (ProjectionClass<P>) entityClass();
    }

    @SuppressWarnings("unchecked") // The cast is ensured by generic parameters of the repository.
    @Override
    protected final ProjectionClass<P> getModelClass(Class<P> cls) {
        return (ProjectionClass<P>) Model.getInstance()
                                         .asProjectionClass(cls);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ensures there is at least one event subscriber method (external or domestic) declared
     * by the class of the projection. Throws an {@code IllegalStateException} otherwise.
     */
    @Override
    public void onRegistered() {
        super.onRegistered();

        final boolean noEventSubscriptions = getMessageClasses().isEmpty();
        if (noEventSubscriptions) {
            final boolean noExternalSubscriptions = getExternalEventDispatcher().getMessageClasses()
                                                                                .isEmpty();
            if (noExternalSubscriptions) {
                throw newIllegalStateException(
                        "Projections of the repository %s have neither domestic nor external " +
                                "event subscriptions.", this);
            }
        }
    }

    /**
     * Obtains event filters for event classes handled by projections of this repository.
     */
    private Set<EventFilter> createEventFilters() {
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

    /**
     * {@inheritDoc}
     *
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
        return projectionClass().getEventSubscriptions();
    }

    @Override
    public Set<I> dispatch(EventEnvelope envelope) {
        return ProjectionEndpoint.handle(this, envelope);
    }

    @Internal
    public void writeLastHandledEventTime(Timestamp timestamp) {
        projectionStorage().writeLastHandledEventTime(timestamp);
    }

    @Internal
    public Timestamp readLastHandledEventTime() {
        return projectionStorage().readLastHandledEventTime();
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

    /**
     * Defines a strategy of event delivery applied to the projections managed by this repository.
     *
     * <p>By default uses direct delivery.
     *
     * <p>Descendants may override this method to redefine the strategy. In particular,
     * it is possible to postpone dispatching of a certain event to a particular projection
     * instance at runtime.
     *
     * @return delivery strategy for events applied to the instances managed by this repository
     */
    @SPI
    protected ProjectionEventDelivery<I, P> getEndpointDelivery() {
        return ProjectionEventDelivery.directDelivery(this);
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
     * {@inheritDoc}
     *
     * <p>Overrides to expose the method to the package.
     */
    @Override
    protected P findWithAnyVisibilityOrCreate(I id) {
        return super.findWithAnyVisibilityOrCreate(id);
    }

    /** Exposes routing to the package. */
    EventRouting<I> eventRouting() {
        return getEventRouting();
    }

    @Override
    protected ExternalMessageDispatcher<I> getExternalEventDispatcher() {
        return new ProjectionExternalEventDispatcher();
    }

    /**
     * An implementation of an external message dispatcher feeding external events
     * to {@code Projection} instances.
     */
    private class ProjectionExternalEventDispatcher extends AbstractExternalEventDispatcher {

        @Override
        public Set<ExternalMessageClass> getMessageClasses() {
            final Set<EventClass> eventClasses = projectionClass().getExternalEventSubscriptions();
            return ExternalMessageClass.fromEventClasses(eventClasses);
        }

        @Override
        public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
            checkNotNull(envelope);
            checkNotNull(exception);
            logError("Error dispatching external event to projection (class: %s, id: %s)",
                     envelope, exception);
        }
    }
}
