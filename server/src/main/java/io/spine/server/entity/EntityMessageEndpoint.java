/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import io.spine.annotation.Internal;
import io.spine.core.ActorMessageEnvelope;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.server.delivery.Delivery;
import io.spine.string.Stringifiers;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Abstract base for endpoints handling messages sent to entities.
 *
 * <p>Loading and storing an entity is a tenant-sensitive operation,
 * which must be performed under the context of the tenant ID in which
 * the message we dispatch was originated.
 *
 * @param <I> the type of entity IDs
 * @param <E> the type of entities
 * @param <M> the type of message envelopes
 * @author Alexander Yevsyukov
 */
@Internal
public abstract class EntityMessageEndpoint<I,
                                            E extends Entity<I, ?>,
                                            M extends ActorMessageEnvelope<?, ?, ?>> {

    /** The repository which created this endpoint. */
    private final Repository<I, E> repository;

    /** The message which needs to handled. */
    private final M envelope;

    protected EntityMessageEndpoint(Repository<I, E> repository, M envelope) {
        this.repository = repository;
        this.envelope = envelope;
    }

    /**
     * Attempts the message to the entity with the passed ID according to the delivery strategy.
     *
     * @param entityId the ID of the entity for which to dispatch the message
     */
    public void dispatchTo(I entityId) {
        checkNotNull(entityId);
        try {
            doDispatchToOne(entityId);
        } catch (RuntimeException exception) {
            onError(envelope(), exception);
        }
    }

    private void doDispatchToOne(I entityId) {
        M envelope = envelope();
        Delivery<I, E, M, ?, ?> delivery = getEndpointDelivery();
        delivery.getSender()
                .send(entityId, envelope);
    }

    /**
     * Dispatches the message to the entity with the passed ID, providing transactional work
     * and storage of the entity.
     *
     * <p>Performs the delivery directly to the entity not taking
     * the delivery strategy into account.
     *
     * @param entityId the ID of the entity for which to dispatch the message
     */
    protected abstract void deliverNowTo(I entityId);

    /**
     * Obtains an instance of endpoint delivery.
     *
     * @return the instance of endpoint delivery
     */
    protected abstract Delivery<I, E, M, ?, ?> getEndpointDelivery();

    /**
     * Invokes entity-specific method for dispatching the message.
     */
    protected abstract List<Event> doDispatch(E entity, M envelope);

    /**
     * Stores the entity if it was modified during message dispatching.
     *
     * @param entity the entity to store
     */
    protected final void store(E entity) {
        boolean isModified = isModified(entity);
        if (isModified) {
            onModified(entity);
        } else {
            onEmptyResult(entity, envelope());
        }
    }

    /**
     * Verifies whether the entity was modified during the message dispatching.
     */
    protected abstract boolean isModified(E entity);

    /**
     * Callback to perform operations if the entity was modified during message dispatching.
     */
    protected abstract void onModified(E entity);

    /**
     * Allows derived classes to handle empty list of uncommitted events returned by
     * the entity in response to the message.
     */
    protected abstract void onEmptyResult(E entity, M envelope);

    /**
     * Processes the exception thrown during dispatching the message.
     */
    protected abstract void onError(M envelope, RuntimeException exception);

    /**
     * Obtains the envelope of the message processed by this endpoint.
     */
    protected final M envelope() {
        return envelope;
    }

    /**
     * Obtains the parent repository of this endpoint.
     */
    protected Repository<I, E> repository() {
        return repository;
    }

    /**
     * Throws {@link IllegalStateException} with the diagnostics message on the unhandled command.
     *
     * @param  entity the entity which failed to handle the command
     * @param  cmd    the envelope with the command
     * @param  format the format string with the following parameters
     *                <ol>
     *                   <li>the name of the entity class
     *                   <li>the ID of the entity
     *                   <li>the name of the command class
     *                   <li>the ID of the command
     *                </ol>
     * @throws IllegalStateException always
     */
    protected void onUnhandledCommand(Entity<I, ?> entity, CommandEnvelope cmd, String format) {
        String entityId = Stringifiers.toString(entity.getId());
        String entityClass = entity.getClass().getName();
        String commandId = Stringifiers.toString(cmd.getId());
        CommandClass commandClass = cmd.getMessageClass();
        throw newIllegalStateException(format, entityClass, entityId, commandClass, commandId);
    }
}
