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

package org.spine3.server.entity;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.server.BoundedContext;

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
public abstract class EventDispatchingRepository<I, E extends Entity<I, S>, S extends Message>
        extends RecordBasedRepository<I, E, S>
                implements EntityEventDispatcher<I> {

    private final IdSetFunctions<I> idSetFunctions;

    /**
     * Creates new repository instance.
     *
     * @param boundedContext the {@code BoundedContext} in which the repository works
     * @param defaultFunction the default function for getting an target entity IDs
     */
    protected EventDispatchingRepository(BoundedContext boundedContext,
                                         IdSetEventFunction<I, Message> defaultFunction) {
        super(boundedContext);
        this.idSetFunctions = new IdSetFunctions<>(defaultFunction);
    }

    /**
     * Adds {@code IdSetFunction} for the repository.
     *
     * <p>Typical usage for this method would be in a constructor of a {@code ProjectionRepository}
     * (derived from this class) to provide mapping between events to projection identifiers.
     *
     * <p>Such a mapping may be required when...
     * <ul>
     *     <li>An event should be matched to more than one projection.</li>
     *     <li>The type of an event producer ID (stored in {@code EventContext}) differs from {@code <I>}.</li>
     * </ul>
     *
     * <p>If there is no function for the class of the passed event message,
     * the repository will use the event producer ID from an {@code EventContext} passed with the event message.
     *
     * @param func the function instance
     * @param <M> the type of the event message handled by the function
     */
    public <M extends Message> void addIdSetFunction(Class<M> eventClass, IdSetEventFunction<I, M> func) {
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

    @Override
    public <M extends Message> Optional<IdSetEventFunction<I, M>> getIdSetFunction(Class<M> eventClass) {
        return idSetFunctions.get(eventClass);
    }

    protected Set<I> findIds(Message event, EventContext context) {
        return idSetFunctions.findAndApply(event, context);
    }

    /**
     * Loads or creates an entity by the passed ID.
     *
     * <p>The entity is created if it was not found.
     *
     * @param id the ID of the entity to load
     * @return loaded or created projection instance
     */
    @Override
    @CheckReturnValue
    public Optional<E> load(I id) {
        final Optional<E> loaded = super.load(id);
        if (loaded.isPresent()) {
            return loaded;
        }
        final E result = create(id);
        return Optional.of(result);
    }

    /**
     * Dispatches the passed event to entities.
     *
     * @param event the event to dispatch
     */
    @Override
    public void dispatch(Event event) {
        final Message eventMessage = Events.getMessage(event);
        final EventContext context = event.getContext();
        final Set<I> ids = findIds(eventMessage, context);
        for (I id : ids) {
            dispatchToEntity(id, eventMessage, context);
        }
    }

    /**
     * Dispatches the event to an entity with the passed ID.
     *
     * @param id the ID of the entity
     * @param eventMessage the event message
     * @param context the event context
     */
    protected abstract void dispatchToEntity(I id, Message eventMessage, EventContext context);
}
