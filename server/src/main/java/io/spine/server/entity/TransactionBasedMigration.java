/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import io.spine.base.EntityState;
import io.spine.protobuf.ValidatingBuilder;

/**
 * A migration operation that is tied to an entity {@link Transaction}.
 *
 * <p>The operation always ends with a transaction {@linkplain Transaction#commit() commit}.
 *
 * @param <I>
 *         the type of entity identifiers
 * @param <E>
 *         the type of entity
 * @param <S>
 *         the type of entity state objects
 * @param <B>
 *         the type of a {@code ValidatingBuilder} for the entity state
 */
public abstract class TransactionBasedMigration<I,
                                                E extends TransactionalEntity<I, S, B>,
                                                S extends EntityState,
                                                B extends ValidatingBuilder<S>>
        implements Migration<E> {

    @Override
    public void accept(E entity) {
        Transaction<I, E, S, B> tx = startTransaction(entity);
        tx.commit();
    }

    /**
     * Opens a new {@code Transaction} on the entity.
     */
    protected abstract Transaction<I, E, S, B> startTransaction(E entity);
}
