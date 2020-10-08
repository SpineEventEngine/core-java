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

package io.spine.server.entity.storage;

import io.spine.base.EntityState;
import io.spine.query.EntityQuery;
import io.spine.query.EntityQueryBuilder;

import static io.spine.server.entity.storage.EntityRecordColumn.archived;
import static io.spine.server.entity.storage.EntityRecordColumn.deleted;

/**
 * Finds the non-deleted and non-archived entities in the {@linkplain EntityRecordStorage storage}.
 */
final class FindActiveEntites<I, S extends EntityState<I>>
        extends EntityQuery<I, S, FindActiveEntites.Builder<I, S>> {

    private FindActiveEntites(Builder<I, S> builder) {
        super(builder);
    }

    /**
     * Creates a new builder for {@code FindActiveEntites}.
     *
     * @param <I>
     *         the type of identifiers of the queries entities
     * @param <S>
     *         the type of entity states
     * @return a new builder instance
     */
    public static <I, S extends EntityState<I>> Builder<I, S>
    newBuilder(Class<I> idType, Class<S> stateType,
               boolean hasArchivedColumn, boolean hasDeletedColumn) {
        return new Builder<>(idType, stateType, hasArchivedColumn, hasDeletedColumn);
    }

    /**
     * A builder for {@link FindActiveEntites} query.
     *
     * @param <I>
     *         the type of the queried entity identifiers
     * @param <S>
     *         the type of the entity states
     */
    static final class Builder<I, S extends EntityState<I>>
            extends EntityQueryBuilder<I, S, Builder<I, S>, FindActiveEntites<I, S>> {

        private final boolean hasArchivedColumn;
        private final boolean hasDeletedColumn;

        /**
         * Prevents this builder from a direct instantiation.
         */
        private Builder(Class<I> idType, Class<S> stateType,
                        boolean hasArchivedColumn, boolean hasDeletedColumn) {
            super(idType, stateType);
            this.hasArchivedColumn = hasArchivedColumn;
            this.hasDeletedColumn = hasDeletedColumn;
            setLifecycle();
        }

        private void setLifecycle() {
            if(hasArchivedColumn) {
                where(archived.lifecycle(), false);
            }
            if(hasDeletedColumn) {
                where(deleted.lifecycle(), false);
            }
        }

        @Override
        protected Builder<I, S> thisRef() {
            return this;
        }

        @Override
        public FindActiveEntites<I, S> build() {
            return new FindActiveEntites<>(this);
        }
    }
}
