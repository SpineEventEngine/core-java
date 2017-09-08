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

package io.spine.server.entity;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.core.ExternalMessageEnvelope;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.server.tenant.EventOperation;
import io.spine.server.tenant.TenantAwareFunction0;
import io.spine.type.MessageClass;

import javax.annotation.CheckReturnValue;
import java.util.Collections;
import java.util.Set;

/**
 * Abstract base for repositories that deliver events to entities they manage.
 *
 * @param <I> the type of IDs of entities
 * @param <E> the type of entities
 * @param <S> the type of entity state messages
 * @author Alexander Yevsyukov
 */
public abstract class EventDispatchingRepository<I,
                                                 E extends AbstractVersionableEntity<I, S>,
                                                 S extends Message>
        extends DefaultRecordBasedRepository<I, E, S>
        implements EntityEventDispatcher<I> {

    private final EventRouting<I> eventRouting;

    /**
     * Creates new repository instance.
     *
     * @param defaultFunction the default function for getting target entity IDs
     */
    protected EventDispatchingRepository(EventRoute<I, Message> defaultFunction) {
        super();
        this.eventRouting = EventRouting.withDefault(defaultFunction);
    }

    /**
     * Obtains the {@link EventRouting} schema used by the repository for calculating identifiers
     * of event targets.
     */
    protected final EventRouting<I> getEventRouting() {
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
        if(!getMessageClasses().isEmpty()) {
            registerAsEventDispatcher();
        }

        final ExternalMessageDispatcher<I> thisAsExternal = getExternalEventDispatcher();
        if(!thisAsExternal.getMessageClasses().isEmpty()) {
            getBoundedContext().getIntegrationBus()
                               .register(getExternalEventDispatcher());
        }
    }

    /**
     * Register itself as {@link io.spine.server.event.EventDispatcher EventDispatcher} in
     * the {@linkplain BoundedContext#getEventBus() EventBus}.
     */
    protected void registerAsEventDispatcher() {
        getBoundedContext().getEventBus()
                           .register(this);
    }

    @Override
    @CheckReturnValue
    public final Set<I> getTargets(EventEnvelope envelope) {
        return getEventRouting().apply(envelope.getMessage(), envelope.getEventContext());
    }

    /**
     * Dispatches the passed event envelope to entities.
     *
     * @param envelope the event envelope to dispatch
     * @return the set of IDs of entities that consumed the event
     */
    @Override
    public Set<I> dispatch(final EventEnvelope envelope) {
        final Set<I> targets = getTargets(envelope);
        final TenantId tenantId = envelope.getTenantId();
        // Since dispatching involves stored data, perform in the context of the tenant.
        final TenantAwareFunction0<Set<I>> op = new TenantAwareFunction0<Set<I>>(tenantId) {
            @Override
            public Set<I> apply() {
                final ImmutableSet.Builder<I> consumed = ImmutableSet.builder();
                for (I id : targets) {
                    try {
                        dispatchToEntity(id, envelope);
                        consumed.add(id);
                    } catch (RuntimeException exception) {
                        onError(envelope, exception);
                        // Do not re-throw letting other subscribers to consume the event.
                    }
                }
                return consumed.build();
            }
        };
        final Set<I> consumed = op.execute();
        return consumed;
    }

    /**
     * Logs error into the repository {@linkplain #log() log}.
     *
     * @param envelope  the message which caused the error
     * @param exception the error
     */
    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        logError("Error dispatching event (class: %s, id: %s", envelope, exception);
    }

    /**
     * Dispatches the event to an entity with the passed ID.
     *
     * @param id    the ID of the entity
     * @param event the envelope with the event
     */
    protected abstract void dispatchToEntity(I id, EventEnvelope event);

    /**
     * Obtains an external event dispatcher for this repository.
     *
     * <p>This method should be overridden by the repositories, which are eligible
     * to handle external events. In this case the implementation would typically delegate
     * the dispatching of external events to the repository itself.
     *
     * <p>Such a delegate-based approach is chosen, since it's not possible to make
     * {@code EventDispatchingRepository} extend another
     * {@link io.spine.server.bus.MessageDispatcher} interface due to clashes in the class
     * hierarchy.
     *
     * @return the external event dispatcher
     */
    protected ExternalMessageDispatcher<I> getExternalEventDispatcher() {
        return new AbstractExternalEventDispatcher() {
            @Override
            public Set<MessageClass> getMessageClasses() {
                return Collections.emptySet();
            }

            @Override
            public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
                logError("Error dispatching external event (class: %s, id: %s", envelope, exception);
            }
        };
    }

    /**
     * An abstract base for the external message dispatchers, enabling
     * the {@code EventDispatchingRepository} instances to handle external events.
     */
    protected abstract class AbstractExternalEventDispatcher
            implements ExternalMessageDispatcher<I> {

        @Override
        public Set<I> dispatch(ExternalMessageEnvelope envelope) {
            final Event event = (Event) envelope.getOuterObject();
            final Message eventMessage = Events.getMessage(event);
            final EventContext context = event.getContext();
            final Set<I> ids = getEventRouting().apply(eventMessage, context);
            final EventOperation op = new EventOperation(event) {
                @Override
                public void run() {
                    for (I id : ids) {
                        dispatchToEntity(id, EventEnvelope.of(event));
                    }
                }
            };
            op.execute();
            return ids;
        }
    }
}
