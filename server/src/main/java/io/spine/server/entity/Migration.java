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

import java.util.function.Consumer;

/**
 * A stored {@link Entity} transformation done to account for the domain model changes.
 *
 * <p>Being a {@link Consumer}, the migration is expected to occur in place, on a given
 * {@link Entity} instance.
 *
 * <p>The performed migration is always preceded by an {@link Entity} load by ID and is finalized
 * by {@linkplain Repository#store(Entity) saving} the transformed entity back into the storage.
 *
 * @param <E>
 *         the entity type
 */
public interface Migration<E extends Entity<?, ?>> extends Consumer<E> {

    /**
     * Performs an entity migration.
     *
     * <p>This method is a syntactic sugar for more natural looking API.
     */
    default void apply(E entity) {
        accept(entity);
    }
}
