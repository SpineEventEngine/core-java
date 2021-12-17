/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.commandbus.given;

import io.spine.core.CommandContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.command.AbstractCommandAssignee;
import io.spine.server.command.Assign;
import io.spine.test.commandbus.command.CmdBusAddTask;
import io.spine.test.commandbus.command.CmdBusRemoveTask;
import io.spine.test.commandbus.event.CmdBusTaskAdded;
import io.spine.test.reflect.InvalidProjectName;
import io.spine.test.reflect.ProjectId;

import static io.spine.util.Exceptions.newIllegalStateException;

public class SingleTenantCommandBusTestEnv {

    /** Prevents instantiation of this utility class. */
    private SingleTenantCommandBusTestEnv() {
    }

    /**
     * A {@code CommandAssignee}, which throws a rejection upon a command.
     */
    public static class FaultyAssignee extends AbstractCommandAssignee {

        private FaultyAssignee() {
            super();
        }

        public static FaultyAssignee initializedAssignee() {
            var assignee = new FaultyAssignee();
            assignee.registerWith(BoundedContextBuilder.assumingTests().build());
            return assignee;
        }

        private final InvalidProjectName rejection = InvalidProjectName
                .newBuilder()
                .setProjectId(ProjectId.getDefaultInstance())
                .build();

        @SuppressWarnings("unused")     // does nothing, but throws a rejection.
        @Assign
        CmdBusTaskAdded handle(CmdBusAddTask msg, CommandContext context)
                throws InvalidProjectName {
            throw rejection;
        }

        @SuppressWarnings("unused")     // does nothing, but throws a rejection.
        @Assign
        CmdBusTaskAdded handle(CmdBusRemoveTask msg, CommandContext context) {
            throw newIllegalStateException("Command handling failed with unexpected exception");
        }

        public InvalidProjectName getThrowable() {
            return rejection;
        }
    }
}
