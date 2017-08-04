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
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.core.ExternalMessageEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.entity.EntityStorageConverter;
import io.spine.server.entity.EventDispatchingRepository;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.route.EventProducers;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.route.EventRouting;
import io.spine.server.route.Producers;
import io.spine.server.stand.Stand;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.EventOperation;
import io.spine.type.MessageClass;
import io.spine.type.TypeName;

import javax.annotation.Nullable;
import java.util.Set;

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
        final Class<? extends Projection> projectionClass = getEntityClass();
        final Set<EventClass> result = Projection.TypeInfo.getEventClasses(projectionClass);
        return result;
    }

    /**
     * Dispatches the passed event to corresponding {@link Projection}s.
     *
     * <p>The ID of the projection must be specified as the first property of the passed event.
     *
     * <p>If there is no stored projection with the ID from the event, a new projection is created
     * and stored after it handles the passed event.
     *
     * <p>If the projection was changed as the result of dispatching the event, the following
     * operations are performed:
     * <ol>
     *     <li>The projection is stored.
     *     <li>The timestamp of the event is stored.
     *     <li>The state of the projection is posted to the {@link Stand}.
     * </ol>
     *
     * @see Projection#handle(Message, EventContext)
     */
    @Override
    protected void dispatchToEntity(I id, EventEnvelope event) {
        final P projection = findOrCreate(id);
        final ProjectionTransaction<I, ?, ?> tx =
                ProjectionTransaction.start((Projection<I, ?, ?>) projection);
        final EventContext context = event.getEventContext();
        projection.handle(event.getMessage(), context);
        tx.commit();

        if (projection.isChanged()) {
            final Timestamp eventTime = context.getTimestamp();
            store(projection);
            projectionStorage().writeLastHandledEventTime(eventTime);
            getStand().post(context.getCommandContext()
                                   .getActorContext()
                                   .getTenantId(), projection);
        }
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

    @Override
    protected ExternalMessageDispatcher<I> getExternalDispatcher() {
        return new ProjectionExternalMessageDispatcher();
    }

    /**
     * An implementation of an external message dispatcher feeding external events
     * to {@code Projection} instances.
     */
    private class ProjectionExternalMessageDispatcher implements ExternalMessageDispatcher<I> {

        @Override
        public Set<MessageClass> getMessageClasses() {
            final Class<? extends Projection> projectionClass = getEntityClass();
            final Set<EventClass> eventClasses =
                    Projection.TypeInfo.getExternalEventClasses(projectionClass);
            final ImmutableSet<MessageClass> messageClasses =
                    ImmutableSet.<MessageClass>copyOf(eventClasses);
            return messageClasses;
        }

        @Override
        public Set<I> dispatch(ExternalMessageEnvelope envelope) {
            final Event event = (Event) envelope.getOuterObject();
            final Message eventMessage = Events.getMessage(event);
            final EventContext context = event.getContext();
            final Set<I> ids = getRouting().apply(eventMessage, context);
            final EventOperation op = new EventOperation(event) {
                @Override
                public void run() {
                    for (I id : ids) {
                        dispatchToEntity(id, eventMessage, context);
                    }
                }
            };
            op.execute();
            return ids;
        }
    }
}
