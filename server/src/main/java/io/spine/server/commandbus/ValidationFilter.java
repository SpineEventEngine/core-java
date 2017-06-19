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

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.spine.base.Command;
import io.spine.base.MessageAcked;
import io.spine.envelope.CommandEnvelope;
import io.spine.users.TenantId;
import io.spine.validate.ConstraintViolation;

import java.util.List;

import static io.spine.server.transport.Statuses.invalidArgumentWithCause;
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
    public boolean accept(CommandEnvelope envelope, StreamObserver<MessageAcked> responseObserver) {
        if (!isTenantIdValid(envelope, responseObserver)) {
            return false;
        }
        final boolean commandValid = isCommandValid(envelope, responseObserver);
        return commandValid;
    }

    private boolean isTenantIdValid(CommandEnvelope envelope,
                                    StreamObserver<?> responseObserver) {
        final TenantId tenantId = envelope.getTenantId();
        final boolean tenantSpecified = !isDefault(tenantId);
        final Command command = envelope.getCommand();
        if (commandBus.isMultitenant()) {
            if (!tenantSpecified) {
                reportMissingTenantId(command, responseObserver);
                return false;
            }
        } else {
            if (tenantSpecified) {
                reportTenantIdInapplicable(command, responseObserver);
                return false;
            }
        }
        return true;
    }

    private boolean isCommandValid(CommandEnvelope envelope,
                                   StreamObserver<?> responseObserver) {
        final Command command = envelope.getCommand();
        final List<ConstraintViolation> violations = Validator.getInstance()
                                                              .validate(envelope);
        if (!violations.isEmpty()) {
            final CommandException invalidCommand =
                    InvalidCommandException.onConstraintViolations(command, violations);
            commandBus.commandStore()
                      .storeWithError(command, invalidCommand);
            responseObserver.onError(invalidArgumentWithCause(invalidCommand,
                                                              invalidCommand.getError()));
            return false;
        }
        return true;
    }

    @Override
    public void onClose(CommandBus commandBus) {
        // Do nothing.
    }

    private void reportMissingTenantId(Command command,
                                       StreamObserver<?> responseObserver) {
        final CommandException noTenantDefined =
                InvalidCommandException.onMissingTenantId(command);
        commandBus.commandStore().storeWithError(command, noTenantDefined);
        final StatusRuntimeException exception =
                invalidArgumentWithCause(noTenantDefined, noTenantDefined.getError());
        responseObserver.onError(exception);
    }

    private void reportTenantIdInapplicable(Command command,
                                            StreamObserver<?> responseObserver) {
        final CommandException tenantIdInapplicable =
                InvalidCommandException.onInapplicableTenantId(command);
        commandBus.commandStore()
                  .storeWithError(command, tenantIdInapplicable);
        final StatusRuntimeException exception =
                invalidArgumentWithCause(tenantIdInapplicable, tenantIdInapplicable.getError());
        responseObserver.onError(exception);
    }
}
