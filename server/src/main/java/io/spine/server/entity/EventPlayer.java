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

import io.spine.server.event.EventStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Plays events upon a certain entity.
 *
 * @author Dmytro Dashenkov
 * @see TransactionalEventPlayer
 */
public interface EventPlayer {

    /**
     * Plays the given {@link EventStream} upon the underlying entity.
     *
     * <p>Typically, the entity state is changed upon this operation.
     *
     * @param events the event steam to play
     */
    void play(EventStream events);

    /**
     * Creates a transactional {@code EventPlayer} for the given
     * {@linkplain TransactionalEntity entity}.
     *
     * <p>It is expected that the given entity is currently in a transaction.
     *
     * @param entity the entity to create the player for
     * @return new instance on {@code EventPlayer}
     */
    static EventPlayer forTransactionOf(TransactionalEntity<?, ?, ?> entity) {
        checkNotNull(entity);
        return new TransactionalEventPlayer(entity.tx());
    }
}
