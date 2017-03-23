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

package org.spine3.server.storage;

import org.spine3.base.Command;
import org.spine3.base.CommandId;

/**
 * A tenant-aware operation performed in response to a command or in relation to a command.
 *
 * @author Alexander Yevsyukov
 */
public abstract class CommandOperation extends TenantAwareOperation {

    /**
     * The command because of which the operation is performed.
     */
    private final Command command;

    /**
     * Creates and instance for the operation on the tenant data in
     * response to the passed command.
     *
     * @param command the command from which context to obtain the tenant ID
     */
    protected CommandOperation(Command command) {
        super(command.getContext().getTenantId());
        this.command = command;
    }

    /**
     * Obtains ID of the currently handled command.
     *
     * @throws IllegalStateException of the method is called from non-command handling context
     */
    protected CommandId getCommandId() {
        return command.getContext()
                      .getCommandId();
    }
}
