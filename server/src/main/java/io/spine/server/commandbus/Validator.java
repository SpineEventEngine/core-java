/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.commandbus;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.server.route.DefaultCommandRoute;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.MessageValidator;

import java.util.List;

import static io.spine.Identifier.EMPTY_ID;
import static io.spine.validate.Validate.isDefault;

/**
 * The validator for {@code Command} instances.
 *
 * @author Alexander Yevsyukov
 */
class Validator {

    private Validator() {
        // Prevent direct instantiation of this singleton class.
    }

    /** Returns a validator instance. */
    static Validator getInstance() {
        return Singleton.INSTANCE.value;
    }

    /**
     * Validates a command checking that its required fields are valid and
     * validates a command message according to Spine custom protobuf options.
     *
     * @param envelope a command to validate
     * @return constraint violations found
     */
    List<ConstraintViolation> validate(CommandEnvelope envelope) {
        final ImmutableList.Builder<ConstraintViolation> result = ImmutableList.builder();
        final Message message = envelope.getMessage();
        final CommandContext context = envelope.getCommandContext();
        final CommandId id = envelope.getId();
        validateCommandId(id, result);
        validateMessage(message, result);
        validateContext(context, result);
        validateTargetId(message, result);
        return result.build();
    }

    private static void validateMessage(Message message,
                                        ImmutableList.Builder<ConstraintViolation> result) {
        if (isDefault(message)) {
            result.add(violation("Non-default command message must be set."));
        }
        final List<ConstraintViolation> messageViolations = MessageValidator.newInstance()
                                                                            .validate(message);
        result.addAll(messageViolations);
    }

    private static void validateContext(CommandContext context,
                                        ImmutableList.Builder<ConstraintViolation> result) {
        if (isDefault(context)) {
            result.add(violation("Non-default command context must be set."));
        }
    }

    private static void validateCommandId(CommandId id,
                                          ImmutableList.Builder<ConstraintViolation> result) {
        final String commandId = Identifier.toString(id);
        if (commandId.equals(EMPTY_ID)) {
            result.add(violation("Command ID cannot be empty or blank."));
        }
    }

    private static void validateTargetId(Message message,
                                         ImmutableList.Builder<ConstraintViolation> result) {
        final Optional targetId = DefaultCommandRoute.asOptional(message);
        if (targetId.isPresent()) {
            final String targetIdString = Identifier.toString(targetId.get());
            if (targetIdString.equals(EMPTY_ID)) {
                result.add(violation("Command target entity ID cannot be empty or blank."));
            }
        }
    }

    private static ConstraintViolation violation(String msg) {
        return ConstraintViolation.newBuilder()
                                  .setMsgFormat(msg)
                                  .build();
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Validator value = new Validator();
    }
}
