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

package io.spine.server.entity;

import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.util.GenericTypeIndex;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A server-side object with an {@link Identifier#checkSupported(Class) identity}.
 *
 * <p>A state of an entity is defined as a Protobuf message.
 *
 * @param <I> the type of entity identifier
 * @param <S> the type of entity state
 * @author Alexander Yevsyukov
 * @see VersionableEntity
 */
public interface Entity<I, S extends Message> {

    /**
     * Obtains the identifier of the entity.
     */
    I getId();

    /**
     * Obtains the entity state.
     *
     * <p>This method returns the current state of the entity or,
     * if the entity does not have state yet, the value produced by {@link #getDefaultState()}.
     *
     * @return the current state of default state value
     */
    S getState();

    /**
     * Obtains the default entity state.
     *
     * @return an empty instance of the entity state
     */
    S getDefaultState();

    /**
     * Enumeration of generic type parameters of this interface.
     */
    enum GenericParameter implements GenericTypeIndex<Entity> {

        /**
         * The index of the declaration of the generic parameter type {@code <I>}
         * in {@link Entity}.
         */
        ID(0),

        /**
         * The index of the declaration of the generic parameter type {@code <S>}
         * in {@link Entity}
         */
        STATE(1);

        private final int index;

        GenericParameter(int index) {
            this.index = index;
        }

        @Override
        public Class<?> getArgumentIn(Class<? extends Entity> entityClass) {
            return Default.getArgument(this, entityClass);
        }

        @Override
        public int getIndex() {
            return index;
        }
    }

    /**
     * Provides type information on classes implementing {@link Entity}.
     */
    class TypeInfo {

        private TypeInfo() {
            // Prevent construction from outside.
        }

        /**
         * Retrieves the ID class of the entities of the given class.
         *
         * @param entityClass the entity class to inspect
         * @param <I>         the entity ID type
         * @return the entity ID class
         */
        public static <I> Class<I> getIdClass(Class<? extends Entity> entityClass) {
            checkNotNull(entityClass);
            @SuppressWarnings("unchecked") // The type is preserved by the Entity type declaration.
            final Class<I> result = (Class<I>) GenericParameter.ID.getArgumentIn(entityClass);
            return result;
        }

        /**
         * Retrieves the state class of the passed entity class.
         *
         * @param <S>         the entity state type
         * @param entityClass the entity class to inspect
         * @return the entity state class
         */
        public static <S extends Message> Class<S>
        getStateClass(Class<? extends Entity> entityClass) {
            @SuppressWarnings("unchecked") // The type is preserved by the Entity type declaration.
            final Class<S> result = (Class<S>) GenericParameter.STATE.getArgumentIn(entityClass);
            return result;
        }
    }
}
