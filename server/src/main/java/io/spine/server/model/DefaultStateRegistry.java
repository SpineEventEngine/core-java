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

package io.spine.server.model;

import com.google.protobuf.Message;
import io.spine.protobuf.Messages;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityClass;

import javax.annotation.CheckReturnValue;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.collect.Maps.newConcurrentMap;

/**
 * A wrapper for the map from entity classes to entity default states.
 *
 * @author Alexander Yevsyukov
 * @author Dmitry Ganzha
 */
class DefaultStateRegistry {

    /**
     * The lock for DefaultStateRegistry's accessor methods.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

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
        lock.readLock().lock();
        try {
            final boolean result = defaultStates.containsKey(entityClass);
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * If {@link #defaultStates} does not contain state then save state and return it otherwise
     * return saved state.
     *
     * @param entityClass an entity class
     */
    Message putOrGet(Class<? extends Entity> entityClass) {
        lock.writeLock().lock();
        try {
            if (!contains(entityClass)) {
                final Message defaultState = createDefaultState(entityClass);
                defaultStates.put(entityClass, defaultState);
            }
            final Message result = get(entityClass);
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Obtains a state for the passed class.
     *
     * @param entityClass an entity class
     */
    @CheckReturnValue
    Message get(Class<? extends Entity> entityClass) {
        lock.readLock().lock();
        try {
            final Message state = defaultStates.get(entityClass);
            return state;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Creates default state by entity class.
     *
     * @return default state
     */
    private Message createDefaultState(Class<? extends Entity> entityClass) {
        final Class<? extends Message> stateClass = getStateClass(entityClass);
        final Message result = Messages.newInstance(stateClass);
        return result;
    }

    /**
     * Obtains the class of the entity state.
     */
    private Class<? extends Message> getStateClass(Class<? extends Entity> entityClass) {
        return EntityClass.getStateClass(entityClass);
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
