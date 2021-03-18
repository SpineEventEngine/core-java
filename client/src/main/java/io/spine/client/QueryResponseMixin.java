/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.client;

import com.google.common.collect.ImmutableList;
import io.spine.annotation.GeneratedMixin;
import io.spine.base.EntityState;
import io.spine.core.Version;
import io.spine.protobuf.AnyPacker;
import io.spine.type.UnexpectedTypeException;

import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.AnyPacker.unpackFunc;

/**
 * Extends {@link QueryResponse} with useful methods.
 */
@GeneratedMixin
interface QueryResponseMixin extends QueryResponseOrBuilder {

    /**
     * Obtains the size of the response.
     */
    default int size() {
        int result = getMessageList().size();
        return result;
    }

    /**
     * Verifies if the response is empty.
     */
    default boolean isEmpty() {
        boolean result = size() == 0;
        return result;
    }

    /**
     * Obtains immutable list of entity states returned in this query response.
     */
    default ImmutableList<? extends EntityState<?, ?, ?>> states() {
        ImmutableList<EntityState<?, ?, ?>> result = getMessageList()
                .stream()
                .map(EntityStateWithVersion::getState)
                .map(AnyPacker::unpack)
                .map((m) -> (EntityState<?, ?, ?>) m)
                .collect(toImmutableList());
        return result;
    }

    /**
     * Obtains immutable list of the queried entity states of the given type.
     *
     * @throws UnexpectedTypeException if the {@code type} does not match the actual result type
     */
    default <S extends EntityState<?, ?, ?>> ImmutableList<S> states(Class<S> type)
            throws UnexpectedTypeException {
        ImmutableList<S> result = getMessageList()
                .stream()
                .map(EntityStateWithVersion::getState)
                .map(unpackFunc(type))
                .collect(toImmutableList());
        return result;
    }

    /**
     * Obtains entity state at the given index.
     *
     * @throws IndexOutOfBoundsException
     *         if the index is out of the range of entities returned by this query response
     */
    default EntityState<?, ?, ?> state(int index) {
        EntityStateWithVersion stateWithVersion = getMessageList().get(index);
        EntityState<?, ?, ?> result = (EntityState<?, ?, ?>) unpack(stateWithVersion.getState());
        return result;
    }

    /**
     * Obtains immutable list of entity states returned in this query response.
     */
    default List<Version> versions() {
        ImmutableList<Version> result = getMessageList()
                .stream()
                .map(EntityStateWithVersion::getVersion)
                .collect(toImmutableList());
        return result;
    }

    /**
     * Obtains the version of the entity at the given index.
     *
     * @throws IndexOutOfBoundsException
     *         if the index is out of the range of entities returned by this query response
     */
    default Version version(int index) {
        EntityStateWithVersion stateWithVersion = getMessageList().get(index);
        Version result = stateWithVersion.getVersion();
        return result;
    }
}
