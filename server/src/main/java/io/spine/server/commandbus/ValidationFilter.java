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
import io.spine.base.MessageAcked;
import io.spine.base.Status;
import io.spine.base.TenantId;
import io.spine.envelope.CommandEnvelope;
import io.spine.validate.ConstraintViolation;

import java.util.List;

import static com.google.common.base.Optional.of;
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

    /**
     * Returns {@code true} if a command is valid, {@code false} otherwise.
     */
    @Override
    public Optional<MessageAcked> accept(CommandEnvelope envelope) {
        final Optional<MessageAcked> tenantCheckResult = isTenantIdValid(envelope);
        if (tenantCheckResult.isPresent()) {
            return tenantCheckResult;
        }
        final Optional<MessageAcked> commandValid = isCommandValid(envelope);
        return commandValid;
    }

    private Optional<MessageAcked> isTenantIdValid(CommandEnvelope envelope) {
        final TenantId tenantId = envelope.getTenantId();
        final boolean tenantSpecified = !isDefault(tenantId);
        final Command command = envelope.getCommand();
        if (commandBus.isMultitenant()) {
            if (!tenantSpecified) {
                final Status status = reportMissingTenantId(command);
                return of(envelope.acknowledge(status));
            }
        } else {
            if (tenantSpecified) {
                final Status status = reportTenantIdInapplicable(command);
                return of(envelope.acknowledge(status));
            }
        }
        return Optional.absent();
    }

    private Optional<MessageAcked> isCommandValid(CommandEnvelope envelope) {
        final Command command = envelope.getCommand();
        final List<ConstraintViolation> violations = Validator.getInstance()
                                                              .validate(envelope);
        if (!violations.isEmpty()) {
            final CommandException invalidCommand =
                    InvalidCommandException.onConstraintViolations(command, violations);
            commandBus.commandStore()
                      .storeWithError(command, invalidCommand);
            final Throwable exception = invalidArgumentWithCause(invalidCommand,
                                                                 invalidCommand.getError());
            final Error error = toError(exception);
            final Status status = Status.newBuilder()
                                        .setError(error)
                                        .build();
            final Optional<MessageAcked> result = of(envelope.acknowledge(status));
            return result;
        }
        return Optional.absent();
    }

    @Override
    public void onClose(CommandBus commandBus) {
        // Do nothing.
    }

    // TODO:2017-06-20:dmytro.dashenkov: Rename these methods.
    private Status reportMissingTenantId(Command command) {
        final CommandException noTenantDefined =
                InvalidCommandException.onMissingTenantId(command);
        commandBus.commandStore().storeWithError(command, noTenantDefined);
        final StatusRuntimeException exception =
                invalidArgumentWithCause(noTenantDefined, noTenantDefined.getError());
        final Error error = toError(exception);
        final Status status = Status.newBuilder()
                                    .setError(error)
                                    .build();
        return status;
    }

    private Status reportTenantIdInapplicable(Command command) {
        final CommandException tenantIdInapplicable =
                InvalidCommandException.onInapplicableTenantId(command);
        commandBus.commandStore()
                  .storeWithError(command, tenantIdInapplicable);
        final StatusRuntimeException exception =
                invalidArgumentWithCause(tenantIdInapplicable, tenantIdInapplicable.getError());
        final Error error = toError(exception);
        final Status status = Status.newBuilder()
                                    .setError(error)
                                    .build();
        return status;
    }
}
