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
import io.spine.core.EventEnvelope;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.server.tenant.TenantAwareFunction0;

import javax.annotation.CheckReturnValue;
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
        registerAsEventDispatcher();
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
        final TenantId tenantId = envelope.getActorContext()
                                          .getTenantId();
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
}
