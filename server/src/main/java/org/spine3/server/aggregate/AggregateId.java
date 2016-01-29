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

package org.spine3.server.aggregate;

import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.client.Commands;
import org.spine3.protobuf.MessageField;
import org.spine3.server.EntityId;
import org.spine3.server.aggregate.error.MissingAggregateIdException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.server.Identifiers.ID_PROPERTY_SUFFIX;

/**
 * Value object for aggregate IDs.
 *
 * @param <I> the type of aggregate IDs
 * @author Alexander Yevsyukov
 */
public final class AggregateId<I> extends EntityId<I> {

    /**
     * The aggregate ID must be the first field in events/commands.
     */
    public static final int ID_FIELD_INDEX = 0;

    private AggregateId(I value) {
        super(value);
    }

    /**
     * Creates a new non-null id of an aggregate root.
     *
     * @param value id value
     * @return new instance
     */
    public static <I> AggregateId<I> of(I value) {
        return new AggregateId<>(value);
    }

    /**
     * Obtains an aggregate id from the passed command message.
     *
     * <p>The id value must be the first field of the proto message. Its name must end with "id".
     *
     * @param message the command message to get id from
     * @return value of the id
     */
    public static AggregateId fromCommandMessage(Message message) {
        final Object value = FIELD.getValue(checkNotNull(message));
        return of(value);
    }

    /**
     * Obtains an aggregate id from the passed command.
     *
     * <p>The id value must be the first field of the proto message. Its name must end with "id".
     *
     * @param command the command request
     * @return value of the id
     */
    public static AggregateId fromCommand(Command command) {
        final Message message = Commands.getMessage(command);
        return fromCommandMessage(message);
    }

    /**
     * Accessor object for aggregate ID fields in commands.
     * <p/>
     * <p>An aggregate ID must be the first field declared in a message and its
     * name must end with {@code "id"} suffix.
     */
    private static final MessageField FIELD = new MessageField(ID_FIELD_INDEX) {

        @Override
        protected RuntimeException createUnavailableFieldException(Message message, String fieldName) {
            return new MissingAggregateIdException(message.getClass().getName(), fieldName);
        }

        @Override
        protected boolean isFieldAvailable(Message message) {
            final String fieldName = MessageField.getFieldName(message, getIndex());
            final boolean result = fieldName.endsWith(ID_PROPERTY_SUFFIX);
            return result;
        }
    };
}
