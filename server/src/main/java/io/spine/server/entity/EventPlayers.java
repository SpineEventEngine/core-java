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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The factory of {@link EventPlayer} instances.
 *
 * @author Dmytro Dashenkov
 */
@Internal
public final class EventPlayers {

    /**
     * Prevents the utility class instantiation.
     */
    private EventPlayers() {
    }

    /**
     * Creates a transactional {@link EventPlayer} for the given
     * {@linkplain TransactionalEntity entity}.
     *
     * <p>It is expected that the given entity is currently in a transaction. If this condition is
     * not met, an {@code IllegalStateException} is {@linkplain TransactionalEntity#tx() thrown}.
     *
     * @param entity the entity to create the player for
     * @return new instance on {@code EventPlayer}
     */
    public static EventPlayer forTransactionOf(TransactionalEntity<?, ?, ?> entity) {
        checkNotNull(entity);
        return new TransactionalEventPlayer(entity.tx());
    }
}
