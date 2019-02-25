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

package io.spine.server.entity;

import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.server.event.EventDispatcher;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.server.type.EventEnvelope;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.tenant.TenantAwareRunner.with;

/**
 * Abstract base for repositories that deliver events to entities they manage.
 */
public abstract class EventDispatchingRepository<I,
                                                 E extends AbstractEntity<I, S>,
                                                 S extends Message>
        extends DefaultRecordBasedRepository<I, E, S>
        implements EventDispatcher<I> {

    private final EventRouting<I> eventRouting;

    /**
     * Creates new repository instance.
     *
     * @param defaultFunction the default function for getting target entity IDs
     */
    protected EventDispatchingRepository(EventRoute<I, EventMessage> defaultFunction) {
        super();
        this.eventRouting = EventRouting.withDefault(defaultFunction);
    }

    /**
     * Obtains the {@link EventRouting} schema used by the repository for calculating identifiers
     * of event targets.
     */
    protected final EventRouting<I> eventRouting() {
        return eventRouting;
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@linkplain io.spine.server.event.EventBus#register(io.spine.server.bus.MessageDispatcher)
     * Registers} itself with the {@code EventBus} of the parent {@code BoundedContext}.
     */
    @Override
    public void onRegistered() {
        super.onRegistered();
        boundedContext().registerEventDispatcher(this);
    }

    /**
     * Dispatches the event to the corresponding entities.
     *
     * <p>If there is no stored entity with such an ID, a new one is created and stored after it
     * handles the passed event.
     *
     * @param envelope the event to dispatch
     */
    @Override
    public final Set<I> dispatch(EventEnvelope envelope) {
        checkNotNull(envelope);
        Set<I> targets = with(envelope.tenantId())
                .evaluate(() -> doDispatch(envelope));
        return targets;
    }

    private Set<I> doDispatch(EventEnvelope envelope) {
        Set<I> targets = route(envelope);
        Event event = envelope.outerObject();
        targets.forEach(id -> dispatchTo(id, event));
        return targets;
    }

    /**
     * Dispatches the given event to an entity with the given ID.
     *
     * @param id
     *         the target entity ID
     * @param event
     *         the event to dispatch
     */
    protected abstract void dispatchTo(I id, Event event);

    /**
     * Determines the targets of the given event.
     *
     * @param envelope the event to find targets for
     * @return a set of IDs of projections to dispatch the given event to
     */
    private Set<I> route(EventEnvelope event) {
        EventRouting<I> routing = eventRouting();
        Set<I> targets = routing.apply(event.message(), event.context());
        return targets;
    }

    /**
     * Logs error into the repository {@linkplain #log() log}.
     *
     * @param envelope  the message which caused the error
     * @param exception the error
     */
    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        logError("Error dispatching event (class: `%s`, id: `%s`) to entity of type `%s`.",
                 envelope, exception);
    }

    /**
     * An abstract base for the external message dispatchers, enabling
     * the {@code EventDispatchingRepository} instances to handle external events.
     */
    protected abstract class AbstractExternalEventDispatcher
            implements ExternalMessageDispatcher<I> {

        @Override
        public Set<I> dispatch(ExternalMessageEnvelope envelope) {
            EventEnvelope event = envelope.toEventEnvelope();
            return EventDispatchingRepository.this.dispatch(event);
        }

        @Override
        public boolean canDispatch(ExternalMessageEnvelope envelope) {
            EventEnvelope event = envelope.toEventEnvelope();
            return EventDispatchingRepository.this.canDispatch(event);
        }
    }
}
