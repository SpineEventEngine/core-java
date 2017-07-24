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

import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.ExternalMessageEnvelope;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.server.tenant.EventOperation;
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

    private final EventRouting<I> routing;

    /**
     * Creates new repository instance.
     *
     * @param defaultFunction the default function for getting target entity IDs
     */
    protected EventDispatchingRepository(EventRoute<I, Message> defaultFunction) {
        super();
        this.routing = EventRouting.withDefault(defaultFunction);
    }

    /**
     * Obtains the {@link EventRouting} schema used by the repository for calculating identifiers
     * of event targets.
     */
    protected final EventRouting<I> getRouting() {
        return routing;
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
        getBoundedContext().getEventBus()
                           .register(this);

        getBoundedContext().getIntegrationBus()
                           .register(getExternalDispatcher());

    }

    @Override
    @CheckReturnValue
    public final Set<I> getTargets(EventEnvelope envelope) {
        return getRouting().apply(envelope.getMessage(), envelope.getEventContext());
    }

    /**
     * Dispatches the passed event envelope to entities.
     *
     * @param envelope the event envelope to dispatch
     */
    @Override
    public Set<I> dispatch(EventEnvelope envelope) {
        final Message eventMessage = envelope.getMessage();
        final EventContext context = envelope.getEventContext();
        final Set<I> ids = getTargets(envelope);
        final EventOperation op = new EventOperation(envelope.getOuterObject()) {
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

    /**
     * Dispatches the event to an entity with the passed ID.
     *
     * @param id           the ID of the entity
     * @param eventMessage the event message
     * @param context      the event context
     */
    protected abstract void dispatchToEntity(I id, Message eventMessage, EventContext context);

    /**
     * Obtains an external event dispatcher for this repository.
     *
     * <p>This method should be overridden by the repositories, which are eligible
     * to handle external messages. In this case the implementation would typically delegate
     * the dispatching of external messages to the repository itself.
     *
     * <p>Such a delegate-based approach is chosen, since it's not possible to make
     * {@code EventDispatchingRepository} extend another
     * {@link io.spine.server.bus.MessageDispatcher} interface due to clashes in the class
     * hierarchy.
     *
     * @return the external event dispatcher
     */
    protected ExternalMessageDispatcher getExternalDispatcher() {
        return new ExternalMessageDispatcher() {
            @Override
            public Set<MessageClass> getMessageClasses() {
                return Collections.emptySet();
            }

            @Override
            public void dispatch(ExternalMessageEnvelope envelope) {
                // do nothing by default.
            }
        };
    }
}
