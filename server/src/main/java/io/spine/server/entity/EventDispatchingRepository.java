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

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.spine.base.EventContext;
import io.spine.envelope.EventEnvelope;
import io.spine.server.entity.idfunc.EventTargetsFunction;
import io.spine.server.entity.idfunc.Producers;
import io.spine.server.tenant.EventOperation;

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

    private final CompositeEventTargetsFunction<I> idSetFunctions;

    /**
     * Creates new repository instance.
     *
     * @param defaultFunction the default function for getting an target entity IDs
     */
    protected EventDispatchingRepository(EventTargetsFunction<I, Message> defaultFunction) {
        super();
        this.idSetFunctions = new CompositeEventTargetsFunction<>(defaultFunction);
    }

    /**
     * Obtains the {@link EventTargetsFunction} used by the repository for calculating identifiers of
     * event targets.
     */
    protected EventTargetsFunction<I, Message> getIdSetFunction() {
        return idSetFunctions;
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
    }

    /**
     * Adds {@code IdSetFunction} for the repository.
     *
     * <p>Typical usage for this method would be in a constructor of a {@code ProjectionRepository}
     * (derived from this class) to provide mapping between events to projection identifiers.
     *
     * <p>Such a mapping may be required when...
     * <ul>
     *     <li>An event should be matched to more than one projection.
     *     <li>The type of an event producer ID (stored in {@code EventContext})
     *     differs from {@code <I>}.
     * </ul>
     *
     * <p>If there is no function for the class of the passed event message,
     * the repository will use the event producer ID from an {@code EventContext} passed
     * with the event message.
     *
     * @param func the function instance
     * @param <M> the type of the event message handled by the function
     */
    public <M extends Message> void addIdSetFunction(Class<M> eventClass,
                                                     EventTargetsFunction<I, M> func) {
        idSetFunctions.put(eventClass, func);
    }

    /**
     * Removes {@code IdSetFunction} from the repository.
     *
     * @param eventClass the class of the event message
     * @param <M> the type of the event message handled by the function we want to remove
     */
    public <M extends Message> void removeIdSetFunction(Class<M> eventClass) {
        idSetFunctions.remove(eventClass);
    }

    public <M extends Message>
           Optional<EventTargetsFunction<I, M>> getIdSetFunction(Class<M> eventClass) {
        return idSetFunctions.get(eventClass);
    }

    @Override
    @CheckReturnValue
    public Set<I> getTargetIds(EventEnvelope envelope) {
        return idSetFunctions.apply(envelope.getMessage(), envelope.getEventContext());
    }

    /**
     * Dispatches the passed event envelope to entities.
     *
     * @param envelope the event envelope to dispatch
     */
    @Override
    public void dispatch(EventEnvelope envelope) {
        final Message eventMessage = envelope.getMessage();
        final EventContext context = envelope.getEventContext();
        final Set<I> ids = getTargetIds(envelope);
        final EventOperation op = new EventOperation(envelope.getOuterObject()) {
            @Override
            public void run() {
                for (I id : ids) {
                    dispatchToEntity(id, eventMessage, context);
                }
            }
        };
        op.execute();
    }

    /**
     * Dispatches the event to an entity with the passed ID.
     *
     * @param id the ID of the entity
     * @param eventMessage the event message
     * @param context the event context
     */
    protected abstract void dispatchToEntity(I id, Message eventMessage, EventContext context);

    /**
     * Obtains default {@code IdSetFunction} that retrieves an event producer
     * from the event context.
     *
     * @param <I> the type of the event producer
     * @return {@code IdSetFunction} instance that returns a set with a single element
     */
    protected static <I> EventTargetsFunction<I, Message> producerFromContext() {
        return Producers.fromContext();
    }

    protected static <I> EventTargetsFunction<I, Message> producerFromFirstMessageField() {
        return Producers.fromFirstMessageField();
    }
}
