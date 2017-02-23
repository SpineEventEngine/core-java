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

import org.spine3.base.CommandId;
import org.spine3.base.FailureThrowable;

/**
 * The service for updating a status of a command.
 *
 * @author Alexander Yevsyukov
 */
public class CommandStatusService {

    private final CommandStore commandStore;

    CommandStatusService(CommandStore commandStore) {
        this.commandStore = commandStore;
    }

    public void setOk(CommandId commandId) {
        commandStore.setCommandStatusOk(commandId);
    }

    public void setToError(CommandId commandId, Exception exception) {
        commandStore.updateStatus(commandId, exception);
    }

    public void setToFailure(CommandId commandId, FailureThrowable failure) {
        commandStore.updateStatus(commandId, failure.toFailure());
    }

    public void setToError(CommandId commandId, org.spine3.base.Error error) {
        commandStore.updateStatus(commandId, error);
    }
}
