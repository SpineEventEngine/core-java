/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import com.google.protobuf.Message;
import io.spine.annotation.GeneratedMixin;
import io.spine.core.Version;
import io.spine.protobuf.AnyPacker;

import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * Extends {@link QueryResponse} with useful methods.
 */
@GeneratedMixin
public interface QueryResponseMixin {

    @SuppressWarnings("override") // in generated code
    List<EntityStateWithVersion> getMessageList();

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
    default List<? extends Message> states() {
        ImmutableList<Message> result = getMessageList()
                .stream()
                .map(EntityStateWithVersion::getState)
                .map(AnyPacker::unpack)
                .collect(toImmutableList());
        return result;
    }

    /**
     * Obtains entity state at the given index.
     *
     * @throws IndexOutOfBoundsException
     *         if there is index is out of the range of entities returned by this query response
     */
    default Message state(int index) {
        EntityStateWithVersion stateWithVersion = getMessageList().get(index);
        Message result = AnyPacker.unpack(stateWithVersion.getState());
        return result;
    }

    /**
     * Obtains the version of the entity at the given index.
     *
     * @throws IndexOutOfBoundsException
     *         if there is index is out of the range of entities returned by this query response
     */
    default Version version(int index) {
        EntityStateWithVersion stateWithVersion = getMessageList().get(index);
        Version result = stateWithVersion.getVersion();
        return result;
    }
}
