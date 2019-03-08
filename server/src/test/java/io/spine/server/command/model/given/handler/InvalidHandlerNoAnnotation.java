/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.command.model.given.handler;

import io.spine.core.CommandContext;
import io.spine.test.reflect.command.RefCreateProject;
import io.spine.test.reflect.event.RefProjectCreated;

import static io.spine.server.model.given.Given.EventMessage.projectCreated;

/**
 * Provides a method which is not annotated.
 *
 * @implNote The "unused" warning is suppressed because the following. There are no calls to this
 * method since all handler methods are called indirectly. Regular handler methods have annotations
 * and IDEA is configured to ignore unused methods with those annotations.
 * Since the method does not have the annotation (which is the purpose of this test dummy class),
 * it is deemed unused. We suppress the annotation to avoid accidental removal of the method.
 */
@SuppressWarnings("unused") // See Javadoc
public class InvalidHandlerNoAnnotation extends TestCommandHandler {

    public RefProjectCreated handleTest(RefCreateProject cmd, CommandContext context) {
        return projectCreated(cmd.getProjectId());
    }
}
