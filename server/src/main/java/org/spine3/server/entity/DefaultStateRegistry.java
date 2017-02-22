/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.entity;

import com.google.protobuf.Message;

import javax.annotation.CheckReturnValue;
import java.util.Map;

import static com.google.common.collect.Maps.newConcurrentMap;
import static java.lang.String.format;

/**
 * A wrapper for the map from entity classes to entity default states.
 *
 * @author Alexander Yevsyukov
 */
class DefaultStateRegistry {

    /**
     * The map from class of entity to its default state.
     *
     * <p>NOTE: The implementation is not customized with
     * {@link com.google.common.collect.MapMaker#makeMap() makeMap()} options,
     * as it is difficult to predict which work best within the real
     * end-user application scenarios.
     */
    private final Map<Class<? extends Entity>, Message> defaultStates = newConcurrentMap();

    /**
     * Specifies if the entity state of this class is already registered.
     *
     * @param entityClass the class to check
     * @return {@code true} if there is a state for the passed class, {@code false} otherwise
     */
    @CheckReturnValue
    boolean contains(Class<? extends Entity> entityClass) {
        final boolean result = defaultStates.containsKey(entityClass);
        return result;
    }

    /**
     * Saves a state.
     *
     * @param entityClass an entity class
     * @param state a default state of the entity
     * @throws IllegalArgumentException if the state of this class is already registered
     */
    void put(Class<? extends Entity> entityClass, Message state) {
        if (contains(entityClass)) {
            final String msg = format("This class is registered already: %s", entityClass);
            throw new IllegalArgumentException(msg);
        }
        defaultStates.put(entityClass, state);
    }

    /**
     * Obtains a state for the passed class.
     *
     * @param entityClass an entity class
     */
    @CheckReturnValue
    Message get(Class<? extends Entity> entityClass) {
        final Message state = defaultStates.get(entityClass);
        return state;
    }

    static DefaultStateRegistry getInstance() {
        return Singleton.INSTANCE.value;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final DefaultStateRegistry value = new DefaultStateRegistry();
    }
}
