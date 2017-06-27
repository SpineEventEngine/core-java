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
import io.spine.base.Error;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.TenantId;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.validate.ConstraintViolation;

import java.util.List;

import static com.google.common.base.Optional.of;
import static io.spine.server.commandbus.InvalidCommandException.onConstraintViolations;
import static io.spine.server.commandbus.InvalidCommandException.onInapplicableTenantId;
import static io.spine.server.commandbus.InvalidCommandException.onMissingTenantId;
import static io.spine.validate.Validate.isDefault;

/**
 * A validator for a command.
 *
 * <p>Checks if the tenant ID is valid and {@link Validator#validate(CommandEnvelope) validates}
 * the command message.
 *
 * @author Dmytro Dashenkov
 */
final class CommandValidator implements EnvelopeValidator<CommandEnvelope> {

    private final CommandBus commandBus;

    CommandValidator(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @Override
    public Optional<Error> validate(CommandEnvelope envelope) {
        final Optional<Error> tenantCheckResult = isTenantIdValid(envelope);
        if (tenantCheckResult.isPresent()) {
            return tenantCheckResult;
        }
        final Optional<Error> commandValid = isCommandValid(envelope);
        return commandValid;
    }

    private Optional<Error> isTenantIdValid(CommandEnvelope envelope) {
        final TenantId tenantId = envelope.getTenantId();
        final boolean tenantSpecified = !isDefault(tenantId);
        final Command command = envelope.getCommand();
        if (commandBus.isMultitenant()) {
            if (!tenantSpecified) {
                final CommandException exception = missingTenantId(command);
                final Error error = exception.asError();
                return of(error);
            }
        } else {
            if (tenantSpecified) {
                final CommandException exception = tenantIdInapplicable(command);
                final Error error = exception.asError();
                return of(error);
            }
        }
        return Optional.absent();
    }

    private Optional<Error> isCommandValid(CommandEnvelope envelope) {
        final Command command = envelope.getCommand();
        final List<ConstraintViolation> violations = Validator.getInstance()
                                                              .validate(envelope);
        Error result = null;
        if (!violations.isEmpty()) {
            final CommandException invalidCommand = onConstraintViolations(command, violations);
            commandBus.commandStore().storeWithError(command, invalidCommand);
            result = invalidCommand.asError();
        }
        return Optional.fromNullable(result);
    }

    private CommandException missingTenantId(Command command) {
        final CommandException noTenantDefined = onMissingTenantId(command);
        commandBus.commandStore()
                  .storeWithError(command, noTenantDefined);
        return noTenantDefined;
    }

    private CommandException tenantIdInapplicable(Command command) {
        final CommandException tenantIdInapplicable = onInapplicableTenantId(command);
        commandBus.commandStore()
                  .storeWithError(command, tenantIdInapplicable);
        return tenantIdInapplicable;
    }
}
