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

import com.google.protobuf.Message;
import io.spine.protobuf.MessageField;
import io.spine.protobuf.MessageFieldException;

/**
 * Obtains an entity ID based on an event/command message.
 *
 * @param <I> the type of entity IDs
 * @param <M> the type of messages to get IDs from
 * @param <C> the type of the message context
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
abstract class FieldAtIndex<I, M extends Message, C extends Message> implements Unicast<I, M, C> {

    private static final long serialVersionUID = 0L;
    private final EntityIdField idField;

    /**
     * Creates a new instance.
     *
     * @param idIndex a zero-based index of an ID field in this type of messages
     */
    FieldAtIndex(int idIndex) {
        this.idField = new EntityIdField(idIndex);
    }

    /**
     * {@inheritDoc}
     *
     * @throws MessageFieldException if there is no field with required index
     * @throws ClassCastException if the field type is not of the required type
     */
    @Override
    public I apply(M message, C ignored) throws MessageFieldException {
        @SuppressWarnings("unchecked") // we expect that the field is of this type
        I id = (I) idField.getValue(message);
        return id;
    }

    /** Accessor object for entity ID fields. */
    private static class EntityIdField extends MessageField {

        private static final long serialVersionUID = 0L;

        private EntityIdField(int index) {
            super(index);
        }

        @Override
        protected MessageFieldException createUnavailableFieldException(Message message) {
            return new MessageFieldException(message, "There's no field with index %d", getIndex());
        }

        @Override
        protected boolean isFieldAvailable(Message message) {
            boolean result = MessageField.getFieldCount(message) > getIndex();
            return result;
        }
    }
}
