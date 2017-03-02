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

package org.spine3.server.command;

import io.grpc.stub.StreamObserver;
import org.spine3.base.Command;
import org.spine3.base.Response;
import org.spine3.server.Statuses;
import org.spine3.server.command.error.CommandException;
import org.spine3.server.command.error.InvalidCommandException;
import org.spine3.users.TenantId;
import org.spine3.validate.ConstraintViolation;

import java.util.List;

import static org.spine3.validate.Validate.isDefault;

/**
 * Helper class for filtering invalid commands.
 *
 * @author Alexander Yevsyukov
 */
class Filter {

    private final CommandBus commandBus;

    Filter(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    /**
     * Returns {@code true} if a command is valid, {@code false} otherwise.
     */
    boolean handleValidation(Command command, StreamObserver<Response> responseObserver) {
        final TenantId tenantId = command.getContext()
                                         .getTenantId();
        final boolean tenantSpecified = !isDefault(tenantId);

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

        final List<ConstraintViolation> violations = CommandValidator.getInstance()
                                                                     .validate(command);
        if (!violations.isEmpty()) {
            final CommandException invalidCommand = InvalidCommandException.onConstraintViolations(command, violations);
            commandBus.commandStore().storeWithError(command, invalidCommand);
            responseObserver.onError(Statuses.invalidArgumentWithCause(invalidCommand));
            return false;
        }

        return true;
    }

    private void reportMissingTenantId(Command command, StreamObserver<Response> responseObserver) {
        final CommandException noTenantDefined = InvalidCommandException.onMissingTenantId(command);
        commandBus.commandStore().storeWithError(command, noTenantDefined);
        responseObserver.onError(Statuses.invalidArgumentWithCause(noTenantDefined));
    }

    private void reportTenantIdInapplicable(Command command, StreamObserver<Response> responseObserver) {
        final CommandException tenantIdInapplicable = InvalidCommandException.onInapplicableTenantId(command);
        commandBus.commandStore().storeWithError(command, tenantIdInapplicable);
        responseObserver.onError(Statuses.invalidArgumentWithCause(tenantIdInapplicable));
    }
}
