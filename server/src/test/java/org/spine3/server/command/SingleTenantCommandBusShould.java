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

import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.server.command.error.InvalidCommandException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.CommandValidationError.INVALID_COMMAND;
import static org.spine3.base.CommandValidationError.TENANT_INAPPLICABLE;
import static org.spine3.server.command.Given.Command.createProject;
import static org.spine3.server.storage.TenantDataOperation.isTenantSet;

/**
 * @author Alexander Yevsyukov
 */
public class SingleTenantCommandBusShould extends AbstractCommandBusTestSuite {

    public SingleTenantCommandBusShould() {
        super(false);
    }

    @Override
    @Test
    public void setUp() {
        super.setUp();
        commandBus.register(createProjectHandler);
    }

    @Test
    public void post_command_and_do_not_set_current_tenant() {
        commandBus.post(newCommandWithoutTenantId(), responseObserver);

        assertFalse(isTenantSet());
    }

    @Test
    public void reject_invalid_command() {
        final Command cmd = newCommandWithoutContext();

        commandBus.post(cmd, responseObserver);

        checkCommandError(responseObserver.getThrowable(),
                          INVALID_COMMAND,
                          InvalidCommandException.class,
                          cmd);
        assertTrue(responseObserver.getResponses().isEmpty());
    }

    @Test
    public void reject_multitenant_command_in_single_tenant_context() {
        // Create a multi-tenant command.
        final Command cmd = createProject();

        commandBus.post(cmd, responseObserver);

        checkCommandError(responseObserver.getThrowable(),
                          TENANT_INAPPLICABLE,
                          InvalidCommandException.class,
                          cmd);
        assertTrue(responseObserver.getResponses().isEmpty());
    }
}
