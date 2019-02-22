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

package io.spine.server.commandbus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.spine.base.CommandMessage;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.MessageInvalid;
import io.spine.core.TenantId;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.route.DefaultCommandRoute;
import io.spine.server.type.CommandEnvelope;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.MessageValidator;

import java.util.List;
import java.util.Optional;

import static io.spine.base.Identifier.EMPTY_ID;
import static io.spine.server.commandbus.InvalidCommandException.inapplicableTenantId;
import static io.spine.server.commandbus.InvalidCommandException.missingTenantId;
import static io.spine.server.commandbus.InvalidCommandException.onConstraintViolations;
import static io.spine.validate.Validate.isDefault;

/**
 * Validates a command.
 *
 * <p>Ensures that:
 * <ol>
 *     <li>The command has a valid tenant ID set in a multi-tenant context, or no tenant in a
 *     single-tenant context.
 *     <li>The command message {@linkplain CommandValidator#inspect(CommandEnvelope) conforms} to
 *     the options specified in the proto declaration of the message.
 *     <li>The command ID is populated.
 *     <li>The command context is not blank.
 * </ol>
 */
final class CommandValidator implements EnvelopeValidator<CommandEnvelope> {

    private final CommandBus commandBus;

    CommandValidator(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @Override
    public Optional<MessageInvalid> validate(CommandEnvelope envelope) {
        Optional<MessageInvalid> tenantCheckResult = isTenantIdValid(envelope);
        if (tenantCheckResult.isPresent()) {
            return tenantCheckResult;
        }
        Optional<MessageInvalid> commandValid = isCommandValid(envelope);
        return commandValid;
    }

    private Optional<MessageInvalid> isTenantIdValid(CommandEnvelope envelope) {
        TenantId tenantId = envelope.getTenantId();
        boolean tenantSpecified = !isDefault(tenantId);
        Command command = envelope.getCommand();
        if (commandBus.isMultitenant()) {
            if (!tenantSpecified) {
                MessageInvalid report = missingTenantId(command);
                return Optional.of(report);
            }
        } else {
            if (tenantSpecified) {
                MessageInvalid report = inapplicableTenantId(command);
                return Optional.of(report);
            }
        }
        return Optional.empty();
    }

    private static Optional<MessageInvalid> isCommandValid(CommandEnvelope envelope) {
        Command command = envelope.getCommand();
        List<ConstraintViolation> violations = inspect(envelope);
        InvalidCommandException exception = null;
        if (!violations.isEmpty()) {
            exception = onConstraintViolations(command, violations);
        }
        return Optional.ofNullable(exception);
    }

    /**
     * Validates a command checking that its required fields are valid and
     * validates a command message according to Spine custom protobuf options.
     *
     * @param envelope a command to validate
     * @return constraint violations found
     */
    @VisibleForTesting
    static List<ConstraintViolation> inspect(CommandEnvelope envelope) {
        ViolationCheck result = new ViolationCheck(envelope);
        return result.build();
    }

    /**
     * Performs the command instance validation.
     */
    private static final class ViolationCheck {

        private final CommandEnvelope command;
        private final ImmutableList.Builder<ConstraintViolation> result = ImmutableList.builder();

        private ViolationCheck(CommandEnvelope commandEnvelope) {
            this.command = commandEnvelope;
        }

        private List<ConstraintViolation> build() {
            validateId();
            validateMessage();
            validateContext();
            validateTargetId();
            return result.build();
        }

        private void validateId() {
            String commandId = Identifier.toString(command.getId());
            if (commandId.equals(EMPTY_ID)) {
                addViolation("Command ID cannot be empty or blank.");
            }
        }

        private void validateMessage() {
            CommandMessage message = command.message();
            if (isDefault(message)) {
                addViolation("Non-default command message must be set.");
            }
            List<ConstraintViolation> messageViolations = MessageValidator.newInstance(message)
                                                                          .validate();
            result.addAll(messageViolations);
        }

        private void validateContext() {
            if (isDefault(command.getCommandContext())) {
                addViolation("Non-default command context must be set.");
            }
        }

        private void validateTargetId() {
            CommandMessage message = command.message();
            Optional<?> targetId = DefaultCommandRoute.asOptional(message);
            if (targetId.isPresent()) {
                String targetIdString = Identifier.toString(targetId.get());
                if (targetIdString.equals(EMPTY_ID)) {
                    addViolation("Command target entity ID cannot be empty or blank.");
                }
            }
        }

        private void addViolation(String message) {
            result.add(ConstraintViolation.newBuilder()
                                          .setMsgFormat(message)
                                          .build());
        }
    }
}
