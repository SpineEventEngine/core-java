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

import com.google.common.collect.ImmutableSet;
import io.spine.base.EntityState;
import io.spine.query.EntityQuery;
import io.spine.query.EntityQueryBuilder;
import io.spine.query.IdParameter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Alex Tymchenko
 */
public class DefaultEntityQuery <I, S extends EntityState<I>>
        extends EntityQuery<I, S, DefaultEntityQuery.Builder<I, S>> {

    private DefaultEntityQuery(DefaultEntityQuery.Builder<I, S> builder) {
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
    public static <I, S extends EntityState<I>> Builder<I, S> newBuilder(Class<S> stateType) {
        return new Builder<>(stateType);
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
            extends EntityQueryBuilder<I, S, Builder<I, S>, DefaultEntityQuery<I, S>> {

        /**
         * Prevents this builder from a direct instantiation.
         */
        private Builder(Class<S> stateType) {
            super(stateType);
        }

        @Override
        protected Builder<I, S> thisRef() {
            return this;
        }

        Builder<I, S> withId(I id) {
            checkNotNull(id);
            this.setIdParameter(IdParameter.is(id));
            return this;
        }

        Builder<I, S> withIds(Iterable<I> ids) {
            checkNotNull(ids);
            ImmutableSet<I> idSet = ImmutableSet.copyOf(ids);
            this.setIdParameter(IdParameter.in(idSet));
            return this;
        }

        @Override
        public DefaultEntityQuery<I, S> build() {
            return new DefaultEntityQuery(this);
        }
    }
}
