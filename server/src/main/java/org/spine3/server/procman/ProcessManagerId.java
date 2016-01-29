/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.procman;

import com.google.protobuf.Message;
import org.spine3.protobuf.MessageField;
import org.spine3.server.EntityId;
import org.spine3.server.procman.error.MissingProcessManagerIdException;

import static org.spine3.server.Identifiers.ID_PROPERTY_SUFFIX;

/**
 * A value object for process manager IDs.
 *
 * @param <I> the type of process manager IDs
 * @author Alexander Litus
 */
public class ProcessManagerId<I> extends EntityId<I> {

    /**
     * The process manager ID must be the first field in events/commands.
     */
    public static final int ID_FIELD_INDEX = 0;

    private ProcessManagerId(I value) {
        super(value);
    }

    /**
     * Creates a new non-null ID of a process manager.
     *
     * @param value id value
     * @return new manager instance
     */
    public static <I> ProcessManagerId<I> of(I value) {
        return new ProcessManagerId<>(value);
    }

    /**
     * Obtains a process manager ID from the passed command/event instance.
     *
     * <p>The ID value must be the first field of the proto file. Its name must end with the "id" suffix.
     *
     * @param message the command/event to get id from
     * @return value of the id
     */
    public static ProcessManagerId from(Message message) {
        final Object value = FIELD.getValue(message);
        return of(value);
    }

    /**
     * Accessor object for process manager ID fields.
     *
     * <p>A process manager ID must be the first field defined in a message type.
     * Its name must end with {@code "id"} suffix.
     */
    private static final MessageField FIELD = new MessageField(ID_FIELD_INDEX) {
        @Override
        protected RuntimeException createUnavailableFieldException(Message message, String fieldName) {
            return new MissingProcessManagerIdException(message.getClass().getName(), fieldName);
        }

        @Override
        protected boolean isFieldAvailable(Message message) {
            final String fieldName = MessageField.getFieldName(message, getIndex());
            final boolean result = fieldName.endsWith(ID_PROPERTY_SUFFIX);
            return result;
        }
    };
}
