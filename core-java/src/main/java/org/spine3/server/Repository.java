/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Message;
import org.spine3.server.aggregate.AggregateRoot;
import org.spine3.util.Classes;

/**
 * Base interface for repositories.
 *
 * @param <E> the type of the stored object
 * @param <I> the type of the IDs of stored objects
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public interface Repository<I extends Message, E extends Entity<I, ?>> {
    /**
     * Stores the passed object.
     *
     * @param obj an instance to store
     */
    void store(E obj);

    /**
     * Loads the an aggregate by given id.
     *
     * @param objectId id of the aggregate to load
     * @return the loaded object
     */
    E load(I objectId);

    @SuppressWarnings("UtilityClass")
    class TypeInfo {

        public static final int STORED_OBJECT_ID_CLASS_GENERIC_INDEX = 0;
        public static final int STORED_OBJECT_CLASS_GENERIC_INDEX = 1;

        private TypeInfo() {
        }

        /**
         * Returns {@link Class} object representing the aggregate id type of the given repository.
         *
         * @return the aggregate id {@link Class}
         */
        public static <I extends Message> Class<I> getStoredObjectIdClass(Repository repository) {
            return Classes.getGenericParameterType(repository, STORED_OBJECT_ID_CLASS_GENERIC_INDEX);
        }

        /**
         * Returns {@link Class} object representing the aggregate root type of the given repository.
         *
         * @return the aggregate root {@link Class}
         */
        public static <R extends AggregateRoot> Class<R> getStoredObjectClass(Repository repository) {
            return Classes.getGenericParameterType(repository, STORED_OBJECT_CLASS_GENERIC_INDEX);
        }
    }
}
