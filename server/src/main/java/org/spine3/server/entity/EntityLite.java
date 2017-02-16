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

/**
 * A server-side object with an identity.
 *
 * <p>An entity identifier can be of one of the following types:
 *   <ul>
 *      <li>String
 *      <li>Long
 *      <li>Integer
 *      <li>A class implementing {@link Message}
 *   </ul>
 *
 * <p>Consider using {@code Message}-based IDs if you want to have typed IDs in your code, and/or
 * if you need to have IDs with some structure inside. Examples of such structural IDs are:
 *   <ul>
 *      <li>EAN value used in bar codes
 *      <li>ISBN
 *      <li>Phone number
 *      <li>email address as a couple of local-part and domain
 *   </ul>
 *
 * <p>A state of an entity is defined as a protobuf message.
 *
 * @param <I> the type of entity identifier
 * @param <S> the type of entity state
 * @author Alexander Yevsyukov
 */
public interface EntityLite<I, S extends Message> {

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
     * Enumeration of generic type index of this interface.
     */
    enum GenericType {

        /**
         * The index of the declaration of the generic parameter type {@code <I>}
         * in {@link EntityLite}.
         */
        ID(0),

        /**
         * The index of the declaration of the generic parameter type {@code <S>}
         * in {@link EntityLite}
         */
        STATE(1);

        private final int index;

        GenericType(int index) {
            this.index = index;
        }

        int getIndex() {
            return this.index;
        }
    }
}
