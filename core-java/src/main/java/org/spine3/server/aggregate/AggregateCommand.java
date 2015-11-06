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

package org.spine3.server.aggregate;

import com.google.protobuf.Message;
import org.spine3.base.CommandRequest;
import org.spine3.protobuf.MessageFields;
import org.spine3.server.aggregate.error.MissingAggregateIdException;
import org.spine3.util.Identifiers;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A command issued for an aggregate.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters") // is OK as we want many factory methods.
public class AggregateCommand extends AbstractCommand {

    private final AggregateId aggregateId;

    protected AggregateCommand(Message value) {
        super(value);
        this.aggregateId = getAggregateId(value);
    }

    public static AggregateCommand of(Message value) {
        return new AggregateCommand(checkNotNull(value));
    }

    @SuppressWarnings("TypeMayBeWeakened") // This level of API works with already built values.
    public static AggregateCommand of(CommandRequest cr) {
        return new AggregateCommand(getCommandValue(cr));
    }

    public AggregateId getAggregateId() {
        return this.aggregateId;
    }

    /**
     * Obtains an aggregate id from the passed command instance.
     * <p>
     * The id value must be the first field of the proto file. Its name must end with "id".
     *
     * @param command the command to get id from
     * @return value of the id
     */
    public static AggregateId getAggregateId(Message command) {
        final String fieldName = MessageFields.getFieldName(command, Identifiers.AGGREGATE_ID_FIELD_INDEX_IN_COMMANDS);
        if (!fieldName.endsWith(Identifiers.ID_PROPERTY_SUFFIX)) {
            throw new MissingAggregateIdException(command.getClass().getName(), fieldName);
        }

        try {
            final Message value = (Message) MessageFields.getFieldValue(command, Identifiers.AGGREGATE_ID_FIELD_INDEX_IN_COMMANDS);
            return AggregateId.of(value);
        } catch (RuntimeException e) {
            throw new MissingAggregateIdException(command, MessageFields.toAccessorMethodName(fieldName), e);
        }
    }

}
