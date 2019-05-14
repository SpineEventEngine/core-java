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

import com.google.protobuf.Descriptors.FieldDescriptor;
import io.spine.base.CommandMessage;
import io.spine.core.CommandContext;
import io.spine.protobuf.MessageFieldException;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Obtains an ID of a command target entity from the first field of the command message.
 *
 * @param <I> the type of target entity IDs
 */
public final class DefaultCommandRoute<I> implements CommandRoute<I, CommandMessage> {

    private static final long serialVersionUID = 0L;

    /**
     * ID of the target entity is the first field of the command message.
     */
    private static final int ID_FIELD_INDEX = 0;

    private final Class<I> idClass;

    private DefaultCommandRoute(Class<I> cls) {
        this.idClass = cls;
    }

    /**
     * Creates a new instance.
     *
     * @param idClass
     *         the class of identifiers used for the routing
     */
    public static <I> DefaultCommandRoute<I> newInstance(Class<I> idClass) {
        checkNotNull(idClass);
        return new DefaultCommandRoute<>(idClass);
    }

    @Override
    public I apply(CommandMessage message, CommandContext ignored) throws MessageFieldException {
        checkNotNull(message);
        FieldDescriptor field = targetFieldFrom(message);
        I result = targetFrom(field, message);
        return result;
    }

    private I targetFrom(FieldDescriptor field, CommandMessage message) {
        Object value = message.getField(field);
        Class<?> valueClass = value.getClass();
        if (!idClass.isAssignableFrom(valueClass)) {
            throw new MessageFieldException(
                    message, "The field `%s` has the type `%s` which is not assignable" +
                    " from the expected ID type `%s`.",
                    field.getName(),
                    valueClass.getName(),
                    idClass.getName()

            );
        }
        I casted = idClass.cast(value);
        return casted;
    }

    /**
     * Obtains the descriptor of the command target field from the passed message.
     */
    private static FieldDescriptor targetFieldFrom(CommandMessage message) {
        List<FieldDescriptor> fields = message.getDescriptorForType()
                                              .getFields();
        if (fields.size() <= ID_FIELD_INDEX) {
            throw new MessageFieldException(
                    message, "There's no field with the index %d.", ID_FIELD_INDEX
            );
        }
        return fields.get(ID_FIELD_INDEX);
    }

    /**
     * Verifies of the passed command message potentially has a field with an entity ID.
     */
    public static boolean exists(CommandMessage commandMessage) {
        boolean hasAtLeastOneField =
                commandMessage.getDescriptorForType()
                              .getFields()
                              .size() > ID_FIELD_INDEX;
        return hasAtLeastOneField;
    }
}
