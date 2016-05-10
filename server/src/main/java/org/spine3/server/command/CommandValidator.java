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

package org.spine3.server.command;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.server.entity.GetTargetIdFromCommand;
import org.spine3.server.validate.MessageValidator;
import org.spine3.type.TypeName;
import org.spine3.validate.options.ConstraintViolation;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.spine3.base.Identifiers.NULL_OR_EMPTY_ID;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.validate.Validate.*;

/**
 * The validator for {@code Command} instances.
 *
 * @author Alexander Yevsyukov
 */
public class CommandValidator {

    private static final String COMMAND_MESSAGE_MUST_BE_SET = "Command message must be set.";
    private static final String COMMAND_CONTEXT_MUST_BE_SET = "Command context must be set.";
    private static final String COMMAND_ID_CANNOT_BE_EMPTY_OR_BLANK = "Command ID cannot be empty or blank.";
    private static final String COMMAND_TARGET_ENTITY_ID_CANNOT_BE_EMPTY_OR_BLANK =
            "Command target entity ID cannot be empty or blank.";

    private CommandValidator() {
    }

    public List<ConstraintViolation> validate(Command command) {
        final ImmutableList.Builder<ConstraintViolation> result = ImmutableList.builder();
        if (!command.hasMessage()) {
            result.add(ConstraintViolation.newBuilder().setMsgFormat(COMMAND_MESSAGE_MUST_BE_SET).build());
        }
        if (!command.hasContext()) {
            result.add(ConstraintViolation.newBuilder().setMsgFormat(COMMAND_CONTEXT_MUST_BE_SET).build());
        }
        final Message commandMessage = Commands.getMessage(command);
        final Object targetId = GetTargetIdFromCommand.asNullableObject(commandMessage);
        if (targetId != null) {
            final String targetIdString = idToString(targetId);
            if (targetIdString.equals(NULL_OR_EMPTY_ID)) {
                result.add(ConstraintViolation.newBuilder()
                                              .setMsgFormat(COMMAND_TARGET_ENTITY_ID_CANNOT_BE_EMPTY_OR_BLANK)
                                              .build());
            }
        }
        final List<ConstraintViolation> messageViolations = new MessageValidator().validate(commandMessage);
        result.addAll(messageViolations);
        final CommandContext context = command.getContext();
        final String commandId = idToString(context.getCommandId());
        if (commandId.equals(NULL_OR_EMPTY_ID)) {
            result.add(ConstraintViolation.newBuilder()
                                          .setMsgFormat(COMMAND_ID_CANNOT_BE_EMPTY_OR_BLANK)
                                          .build());
        }
        return result.build();
    }

    public static void checkCommand(Command command) {
        checkArgument(command.hasMessage(), COMMAND_MESSAGE_MUST_BE_SET);
        checkArgument(command.hasContext(), COMMAND_CONTEXT_MUST_BE_SET);
        final CommandContext context = command.getContext();
        checkValid(context.getCommandId());
        checkTimestamp(context.getTimestamp(), "Command time");
        final Message commandMessage = Commands.getMessage(command);
        //TODO:2016-05-09:alexander.yevsyukov: Why do we do it?
        final String commandType = TypeName.of(commandMessage).nameOnly();
        checkNotEmptyOrBlank(commandType, "command type");
        final Object targetId = GetTargetIdFromCommand.asNullableObject(commandMessage);
        if (targetId != null) {
            final String targetIdString = idToString(targetId);
            checkNotEmptyOrBlank(targetIdString, "command target ID");
            final String targetIdType = targetId.getClass().getName();
            //TODO:2016-05-09:alexander.yevsyukov: Is this possible?
            checkNotEmptyOrBlank(targetIdType, "command target ID type");
        }
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final CommandValidator value = new CommandValidator();
    }

    public static CommandValidator instance() {
        return LogSingleton.INSTANCE.value;
    }
}
