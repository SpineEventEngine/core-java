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
import io.spine.core.Event;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singleton;

/**
 * Plays events upon a certain entity.
 *
 * @author Dmytro Dashenkov
 */
@Internal
public interface EventPlayer {

    /**
     * Plays the given events against the underlying entity.
     *
     * <p>Typically, the entity state is changed during this operation.
     *
     * @param events the event stream to play
     */
    void play(Iterable<Event> events);

    /**
     * Plays the given event.
     *
     * <p>This is a convenience method. See {@link #play(Iterable)} for more info.
     *
     * @param event the event to play
     */
    default void play(Event event) {
        Collection<Event> events = singleton(event);
        play(events);
    }

    /**
     * Creates a transactional {@code EventPlayer} for the given
     * {@linkplain TransactionalEntity entity}.
     *
     * <p>It is expected that the given entity is currently in a transaction.
     * If this condition is not met, an {@code IllegalStateException} is
     * {@linkplain TransactionalEntity#tx() thrown}.
     *
     * @param entity the entity for which to create the player
     * @return new instance on {@code EventPlayer}
     */
    static EventPlayer forTransactionOf(TransactionalEntity<?, ?, ?> entity) {
        checkNotNull(entity);
        Transaction<?, ?, ?, ?> tx = entity.tx();
        checkArgument(tx instanceof EventPlayingTransaction,
                      "Please specify entity type with EventPlayingTransaction");
        EventPlayingTransaction<?, ?, ?, ?> cast = (EventPlayingTransaction<?, ?, ?, ?>) tx;
        return new TransactionalEventPlayer(cast);
    }
}
