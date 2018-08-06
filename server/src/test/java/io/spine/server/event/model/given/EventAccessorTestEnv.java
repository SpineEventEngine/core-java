/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.event.model.given;

import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectId;
import io.spine.test.event.Rejections.CannotCreateExistingProject;
import io.spine.test.event.command.CreateProject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;

import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Identifier.pack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Dmytro Dashenkov
 */
public class EventAccessorTestEnv {

    public static final String MESSAGE_ONLY = "messageOnly";

    /**
     * Prevents the utility class instantiation.
     */
    private EventAccessorTestEnv() {
    }

    public static Method findMessageOnly() {
        Method method = findMethod(MESSAGE_ONLY, ProjectCreated.class);
        return method;
    }

    public static Method findMessageAndContext() {
        Method method = findMethod("messageAndContext",
                                   ProjectCreated.class, EventContext.class);
        return method;
    }

    public static Method findMessageAndCmdContext() {
        Method method = findMethod("messageAndCmdContext",
                                   CannotCreateExistingProject.class,
                                   CommandContext.class);
        return method;
    }

    public static Method findMessageAndCommand() {
        Method method = findMethod("messageAndCommand",
                                   CannotCreateExistingProject.class,
                                   CreateProject.class);
        return method;
    }

    public static Method findMessageAndCommandMessageAndContext() {
        Method method = findMethod("messageAndCommandMessageAndContext",
                                   CannotCreateExistingProject.class,
                                   CreateProject.class,
                                   CommandContext.class);
        return method;
    }

    private static Method findMethod(String name, Class<?>... arguments) {
        try {
            Method method = EventReceiver.class.getDeclaredMethod(name, arguments);
            return method;
        } catch (NoSuchMethodException e) {
            return fail(e);
        }
    }

    public static ProjectCreated eventMessage() {
        ProjectId id = ProjectId
                .newBuilder()
                .setId(newUuid())
                .build();
        ProjectCreated result = ProjectCreated
                .newBuilder()
                .setProjectId(id)
                .build();
        return result;
    }

    public static CannotCreateExistingProject rejectionMessage() {
        ProjectId id = ProjectId
                .newBuilder()
                .setId(newUuid())
                .build();
        CannotCreateExistingProject result = CannotCreateExistingProject
                .newBuilder()
                .setProjectId(id)
                .build();
        return result;
    }

    public static CreateProject commandMessage() {
        ProjectId id = ProjectId
                .newBuilder()
                .setId(newUuid())
                .build();
        CreateProject result = CreateProject
                .newBuilder()
                .setProjectId(id)
                .build();
        return result;
    }

    public static EventContext eventContext() {
        EventContext result = EventContext
                .newBuilder()
                .setExternal(true)
                .setProducerId(pack("dummy producer"))
                .build();
        return result;
    }

    public static CommandContext commandContext() {
        CommandContext result = CommandContext
                .newBuilder()
                .setTargetVersion(42)
                .build();
        return result;
    }

    @SuppressWarnings("unused") // Reflective method access.
    public static final class EventReceiver {

        private Message event;
        private Message command;
        private EventContext eventContext;
        private CommandContext commandContext;

        private void messageOnly(ProjectCreated event) {
            this.event = event;
        }

        private void messageAndContext(ProjectCreated event, EventContext eventContext) {
            this.event = event;
            this.eventContext = eventContext;
        }

        private void messageAndCmdContext(CannotCreateExistingProject rejection,
                                          CommandContext commandContext) {
            this.event = rejection;
            this.commandContext = commandContext;
        }

        private void messageAndCommand(CannotCreateExistingProject rejection,
                                       CreateProject command) {
            this.event = rejection;
            this.command = command;
        }

        private void messageAndCommandMessageAndContext(CannotCreateExistingProject rejection,
                                                        CreateProject command,
                                                        CommandContext commandContext) {
            this.event = rejection;
            this.command = command;
            this.commandContext = commandContext;
        }

        public void assertEvent(Message expected) {
            assertEquals(expected, event);
        }

        public void assertCommand(@Nullable Message expected) {
            assertEquals(expected, command);
        }

        public void assertEventContext(@Nullable EventContext expected) {
            assertEquals(expected, eventContext);
        }

        public void assertCommandContext(@Nullable CommandContext expected) {
            assertEquals(expected, commandContext);
        }
    }
}
