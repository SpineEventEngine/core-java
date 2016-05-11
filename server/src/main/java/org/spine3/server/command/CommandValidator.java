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
import org.spine3.validate.options.ConstraintViolation;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.spine3.base.Identifiers.*;
import static org.spine3.validate.Validate.checkTimestamp;
import static org.spine3.validate.Validate.checkValid;

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

    /**
     * Validates a command checking that its required fields are valid and
     * validates a command message according to Spine custom protobuf options.
     *
     * @param command a command to validate
     * @return constraint violations found
     */
    public List<ConstraintViolation> validate(Command command) {
        final ImmutableList.Builder<ConstraintViolation> result = ImmutableList.builder();
        if (!command.hasMessage()) {
            result.add(newConstraintViolation(COMMAND_MESSAGE_MUST_BE_SET));
        }
        if (!command.hasContext()) {
            result.add(newConstraintViolation(COMMAND_CONTEXT_MUST_BE_SET));
        }
        final Message commandMessage = Commands.getMessage(command);
        final Object targetId = GetTargetIdFromCommand.asNullableObject(commandMessage);
        if (targetId != null) {
            final String targetIdString = idToString(targetId);
            if (targetIdString.equals(EMPTY_ID)) {
                result.add(newConstraintViolation(COMMAND_TARGET_ENTITY_ID_CANNOT_BE_EMPTY_OR_BLANK));
            }
        }
        final List<ConstraintViolation> messageViolations = new MessageValidator().validate(commandMessage);
        result.addAll(messageViolations);
        final CommandContext context = command.getContext();
        final String commandId = idToString(context.getCommandId());
        if (commandId.equals(EMPTY_ID)) {
            result.add(newConstraintViolation(COMMAND_ID_CANNOT_BE_EMPTY_OR_BLANK));
        }
        return result.build();
    }

    private static ConstraintViolation newConstraintViolation(String msgFormat) {
        final ConstraintViolation violation = ConstraintViolation.newBuilder()
                .setMsgFormat(msgFormat)
                .build();
        return violation;
    }

    /**
     * Checks required fields of a command.
     *
     * <p>Does not validate a command message, only checks that it is set.
     *
     * @param command a command to check
     * @throws IllegalArgumentException if any command field is invalid
     */
    public static void checkCommand(Command command) {
        checkArgument(command.hasMessage(), COMMAND_MESSAGE_MUST_BE_SET);
        checkArgument(command.hasContext(), COMMAND_CONTEXT_MUST_BE_SET);
        final CommandContext context = command.getContext();
        checkValid(context.getCommandId());
        checkTimestamp(context.getTimestamp(), "Command time");
        final Message commandMessage = Commands.getMessage(command);
        final Object targetId = GetTargetIdFromCommand.asNullableObject(commandMessage);
        if (targetId != null) { // else - consider the command is not for an entity
            final String targetIdString = idToString(targetId);
            checkArgument(!targetIdString.equals(EMPTY_ID), "Target ID must not be an empty string.");
        }
    }

    /**
     * Returns a validator instance.
     */
    public static CommandValidator getInstance() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final CommandValidator value = new CommandValidator();
    }
}
