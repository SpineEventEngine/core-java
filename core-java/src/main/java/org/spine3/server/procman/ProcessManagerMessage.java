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

package org.spine3.server.procman;

import com.google.protobuf.Message;
import org.spine3.protobuf.MessageFields;
import org.spine3.server.procman.error.MissingProcessManagerIdException;
import org.spine3.util.MessageValue;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.util.Identifiers.ID_PROPERTY_SUFFIX;

/**
 * A command or event message handled by the process manager.
 *
 * @author Alexander Litus
 */
@SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters") // is OK as we want many factory methods.
public class ProcessManagerMessage extends MessageValue {

    /**
     * The process manager ID must be the first field in events/commands.
     */
    public static final int PROCESS_MANAGER_ID_FIELD_INDEX_IN_MESSAGES = 0;

    private final ProcessManagerId id;

    protected ProcessManagerMessage(Message value) {
        super(value);
        this.id = getProcessManagerId(value);
    }

    public ProcessManagerId getId() {
        return id;
    }

    public static ProcessManagerMessage of(Message value) {
        return new ProcessManagerMessage(checkNotNull(value));
    }

    /**
     * Obtains a process manager ID from the passed command/event instance.
     *
     * <p>The ID value must be the first field of the proto file. Its name must end with the "id" suffix.
     *
     * @param message the command/event to get id from
     * @return value of the id
     */
    public static ProcessManagerId getProcessManagerId(Message message) {
        final String fieldName = MessageFields.getFieldName(message, PROCESS_MANAGER_ID_FIELD_INDEX_IN_MESSAGES);
        if (!fieldName.endsWith(ID_PROPERTY_SUFFIX)) {
            throw new MissingProcessManagerIdException(message.getClass().getName(), fieldName);
        }
        try {
            final Message value = (Message) MessageFields.getFieldValue(message, PROCESS_MANAGER_ID_FIELD_INDEX_IN_MESSAGES);
            return ProcessManagerId.of(value);
        } catch (RuntimeException e) {
            throw new MissingProcessManagerIdException(message, MessageFields.toAccessorMethodName(fieldName), e);
        }
    }
}
