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

package io.spine.server.model;

import io.spine.core.CommandClass;
import io.spine.server.command.Assign;
import io.spine.server.reflect.CommandHandlerMethod;
import io.spine.server.reflect.DuplicateHandlerMethodException;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.command.CreateProject;
import org.junit.Test;

public class MessageHandlerMapShould {

    @Test(expected = DuplicateHandlerMethodException.class)
    public void not_allow_duplicating_message_classes() {
        new MessageHandlerMap<CommandClass, CommandHandlerMethod>
                (HandlerWithDuplicatingMethods.class, CommandHandlerMethod.factory());
    }

    private static class HandlerWithDuplicatingMethods {

        @Assign
        public ProjectCreated on(CreateProject cmd) {
            return ProjectCreated.getDefaultInstance();
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd) {
            return ProjectCreated.getDefaultInstance();
        }
    }
}
