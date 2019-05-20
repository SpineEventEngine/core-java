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

package io.spine.server.route;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Routes messages to a single target, which ID is the same as the first field of
 * the routed message.
 *
 * <p>It is expected that the types of the first field and the identifier are the same.
 *
 * @param <I>
 *          the type of the identifiers
 * @param <M>
 *          the common supertype for messages
 * @param <C>
 *          the type of contexts of the messages
 */
final class FirstField<I, M extends Message, C extends Message> implements Unicast<I, M, C> {

    private static final long serialVersionUID = 0L;
    private final Class<I> idClass;

    FirstField(Class<I> idClass) {
        this.idClass = checkNotNull(idClass);
    }

    @Override
    public I apply(M message, C context) {
        checkNotNull(message);
        FieldDescriptor field = fieldIn(message);
        I result = getValue(field, message);
        return result;
    }

    /**
     * Obtains a descriptor of the first field of the passed.
     *
     * @throws IllegalStateException
     *          if the passed message does not declare fields, or
     *          the field is a repeated field or a map
     */
    private FieldDescriptor fieldIn(M message) {
        Descriptor type = message.getDescriptorForType();
        List<FieldDescriptor> fields = type.getFields();
        if (fields.isEmpty()) {
            throw error("Cannot use the type `%s` for routing: it does not declare any field.",
                        type.getFullName());
        }
        FieldDescriptor field = fields.get(0);
        if (field.isMapField()) {
            throw error("The field `%s` is a map and cannot be used for routing.",
                        field.getFullName());
        }
        if (field.isRepeated()) {
            throw error("The field `%s` is repeated and cannot be used for routing.",
                    field.getFullName());
        }
        return field;
    }

    private IllegalStateException error(String messageFormat, String firstArg) {
        throw newIllegalStateException(
                messageFormat + " Please declare a field with the type `%s`.",
                firstArg,
                idClass.getCanonicalName());
    }

    /**
     * Obtains the value of first field making sure the value is of the expected type.
     */
    private I getValue(FieldDescriptor field, M message) {
        Object value = message.getField(field);
        Class<?> valueClass = value.getClass();
        if (!idClass.isAssignableFrom(valueClass)) {
            throw newIllegalStateException(
                    "The field `%s` has the type `%s` which is not assignable" +
                    " from the expected ID type `%s`.",
                    field.getFullName(),
                    valueClass.getName(),
                    idClass.getName()
            );
        }
        I result = idClass.cast(value);
        return result;
    }
}
