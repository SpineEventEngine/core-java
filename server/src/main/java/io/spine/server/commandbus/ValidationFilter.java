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
import io.grpc.StatusRuntimeException;
import io.spine.base.Command;
import io.spine.base.Error;
import io.spine.base.IsSent;
import io.spine.base.TenantId;
import io.spine.envelope.CommandEnvelope;
import io.spine.validate.ConstraintViolation;

import java.util.List;

import static com.google.common.base.Optional.of;
import static io.spine.base.CommandValidationError.TENANT_INAPPLICABLE;
import static io.spine.base.CommandValidationError.TENANT_UNKNOWN;
import static io.spine.server.bus.Buses.reject;
import static io.spine.server.transport.Statuses.invalidArgumentWithCause;
import static io.spine.util.Exceptions.toError;
import static io.spine.validate.Validate.isDefault;

/**
 * Helper class for filtering invalid commands.
 *
 * @author Alexander Yevsyukov
 */
class ValidationFilter implements CommandBusFilter {

    private final CommandBus commandBus;

    ValidationFilter(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @Override
    public Optional<IsSent> accept(CommandEnvelope envelope) {
        final Optional<IsSent> tenantCheckResult = isTenantIdValid(envelope);
        if (tenantCheckResult.isPresent()) {
            return tenantCheckResult;
        }
        final Optional<IsSent> commandValid = isCommandValid(envelope);
        return commandValid;
    }

    private Optional<IsSent> isTenantIdValid(CommandEnvelope envelope) {
        final TenantId tenantId = envelope.getTenantId();
        final boolean tenantSpecified = !isDefault(tenantId);
        final Command command = envelope.getCommand();
        if (commandBus.isMultitenant()) {
            if (!tenantSpecified) {
                final Error error = missingTenantIdStatus(command);
                return of(reject(envelope.getId(), error));
            }
        } else {
            if (tenantSpecified) {
                final Error error = tenantIdInapplicableStatus(command);
                return of(reject(envelope.getId(), error));
            }
        }
        return Optional.absent();
    }

    private Optional<IsSent> isCommandValid(CommandEnvelope envelope) {
        final Command command = envelope.getCommand();
        final List<ConstraintViolation> violations = Validator.getInstance()
                                                              .validate(envelope);
        if (!violations.isEmpty()) {
            final CommandException invalidCommand =
                    InvalidCommandException.onConstraintViolations(command, violations);
            commandBus.commandStore()
                      .storeWithError(command, invalidCommand);
            final Optional<IsSent> result = of(reject(envelope.getId(), invalidCommand.getError()));
            return result;
        }
        return Optional.absent();
    }

    @Override
    public void onClose(CommandBus commandBus) {
        // Do nothing.
    }

    private Error missingTenantIdStatus(Command command) {
        final CommandException noTenantDefined =
                InvalidCommandException.onMissingTenantId(command);
        commandBus.commandStore().storeWithError(command, noTenantDefined);
        final StatusRuntimeException exception =
                invalidArgumentWithCause(noTenantDefined, noTenantDefined.getError());
        final Error error = toError(exception, TENANT_UNKNOWN.getNumber());
        return error;
    }

    private Error tenantIdInapplicableStatus(Command command) {
        final CommandException tenantIdInapplicable =
                InvalidCommandException.onInapplicableTenantId(command);
        commandBus.commandStore()
                  .storeWithError(command, tenantIdInapplicable);
        final StatusRuntimeException exception =
                invalidArgumentWithCause(tenantIdInapplicable, tenantIdInapplicable.getError());
        final Error error = toError(exception, TENANT_INAPPLICABLE.getNumber());
        return error;
    }
}
