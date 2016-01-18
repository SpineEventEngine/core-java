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
package org.spine3.server;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * Base interface for repositories.
 *
 * @param <I> the type of the IDs of entity objects
 * @param <E> the type of the stored object
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public interface Repository<I, E extends Entity<I, ?>> {

    /**
     * Assigns the storage to the repository.
     *
     * <p>The type of the storage depends on and should be checked by the implementations.
     *
     * <p>This method should be normally called once during registration of the repository with {@link BoundedContext}.
     * An attempt to call this method twice with different parameters will cause {@link IllegalStateException}.
     *
     * <p>{@link BoundedContext} will call this method with {@code null} argument to request performing all necessary
     * clean-up operations before the context is closed.
     *
     * <p>Another storage can be assigned after this method is called with {@code null} parameter.
     *
     * @param storage a storage instance or {@code null} if the current storage should be dismissed.
     *                If there is no storage assigned, passing {@code null} does not have effect
     * @throws ClassCastException    if the passed storage is not of the required type
     * @throws IllegalStateException on attempt to assign a storage if another storage is already assigned
     */
    void assignStorage(@Nullable Object storage);

    /**
     * Create a new entity instance with its default state.
     *
     * @param id the id of the entity
     * @return new entity instance
     */
    @CheckReturnValue
    E create(I id);

    /**
     * Stores the passed object.
     *
     * @param obj an instance to store
     */
    void store(E obj);

    /**
     * Loads the entity with the passed ID.
     *
     * @param id the id of the entity to load
     * @return the entity or {@code null} if there's no entity with such id
     */
    @CheckReturnValue
    @Nullable
    E load(I id);

}
