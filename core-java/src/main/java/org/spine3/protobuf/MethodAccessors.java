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

package org.spine3.protobuf;

import com.google.protobuf.Message;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Is used for method accessors caching for {@link Messages#getFieldValue(Message, int)}.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
class MethodAccessors {
    private static final Map<MethodAccessorKey, Method> methodsMap = new HashMap<>();

    private MethodAccessors() {
    }

    /**
     * Get cached {@link Method} for class by field index.
     *
     * @param clazz      class which contains the method
     * @param fieldIndex method's field index
     * @return instance of (@link Method}. Returns null if nothing is found
     */
    protected static Method get(Class<? extends Message> clazz, int fieldIndex) {
        final MethodAccessorKey methodAccessorKey = new MethodAccessorKey(clazz, fieldIndex);

        final Method method = methodsMap.get(methodAccessorKey);
        return method;
    }

    /**
     * Puts {@link Method} into cache for further usage.
     *
     * @param clazz      class which contains the method
     * @param fieldIndex method's field index
     * @param method     instance of (@link Method}
     */
    protected static void put(Class<? extends Message> clazz, int fieldIndex, Method method) {
        final MethodAccessorKey methodAccessorKey = new MethodAccessorKey(clazz, fieldIndex);
        methodsMap.put(methodAccessorKey, method);
    }

    private static class MethodAccessorKey {
        private final Class clazz;
        private final int fieldIndex;

        protected MethodAccessorKey(Class clazz, int fieldIndex) {
            this.clazz = clazz;
            this.fieldIndex = fieldIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, fieldIndex);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final MethodAccessorKey other = (MethodAccessorKey) obj;
            return Objects.equals(this.clazz, other.clazz)
                    && Objects.equals(this.fieldIndex, other.fieldIndex);
        }
    }
}
