/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.common.base.Predicate;

import javax.annotation.CheckReturnValue;
import java.util.Iterator;

/**
 * A view on a repository.
 *
 * <p>A {@link Repository} may have a view that provides a different set of entities.
 * For example, there can be views that represent archived or deleted entities (that are
 * not "visible" by default).
 *
 * <p>{@code Repository} itself is also a {@code RepositoryView}, which loads only
 * "visible" entities.
 *
 * @param <I> the type of IDs of entities returned by the view
 * @param <E> the entity type
 * @author Alexander Yevsyukov
 */
public interface RepositoryView<I, E extends Entity<I, ?>> {

    /**
     * Finds an entity with the passed ID.
     *
     * @param id the ID of the entity to load
     * @return the entity or {@link Optional#absent()} if there's no entity with such ID
     */
    @CheckReturnValue
    Optional<E> find(I id);

    /**
     * Returns an iterator over the entities exposed by the view
     * that match the passed filter.
     *
     * @param filter the {@linkplain Predicate#apply(Object) filtering} predicate
     * @return new iterator
     */
    @CheckReturnValue
    Iterator<E> iterator(Predicate<E> filter);
}
