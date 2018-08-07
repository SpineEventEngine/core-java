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

package io.spine.server.command.given;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.core.CommandContext;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.Assign;
import io.spine.server.entity.rejection.EntityAlreadyArchived;
import io.spine.server.procman.ProcessManager;
import io.spine.test.reflect.ProjectId;
import io.spine.test.reflect.command.RefCreateProject;
import io.spine.test.reflect.event.RefProjectCreated;
import io.spine.validate.EmptyVBuilder;

import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.server.model.given.Given.EventMessage.projectCreated;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.util.Collections.emptyList;

public class CommandHandlerMethodTestEnv {

    /** Prevents instantiation of this utility class. */
    private CommandHandlerMethodTestEnv() {
    }

    public static class ValidHandlerOneParam extends TestCommandHandler {
        @Assign
        RefProjectCreated handleTest(RefCreateProject cmd) {
            return projectCreated(cmd.getProjectId());
        }
    }

    public static class ValidHandlerOneParamReturnsList extends TestCommandHandler {
        @SuppressWarnings("UnusedReturnValue")
        @Assign
        @VisibleForTesting
        public List<Message> handleTest(RefCreateProject cmd) {
            List<Message> result = newLinkedList();
            result.add(projectCreated(cmd.getProjectId()));
            return result;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class ValidHandlerTwoParams extends TestCommandHandler {
        @Assign
        @VisibleForTesting
        public RefProjectCreated handleTest(RefCreateProject cmd, CommandContext context) {
            return projectCreated(cmd.getProjectId());
        }
    }

    public static class ValidHandlerTwoParamsReturnsList extends TestCommandHandler {
        @Assign
        @VisibleForTesting
        public List<Message> handleTest(RefCreateProject cmd, CommandContext context) {
            List<Message> result = newLinkedList();
            result.add(projectCreated(cmd.getProjectId()));
            return result;
        }
    }

    public static class ValidHandlerButPrivate extends TestCommandHandler {
        @Assign
        @VisibleForTesting
        public RefProjectCreated handleTest(RefCreateProject cmd) {
            return projectCreated(cmd.getProjectId());
        }
    }

    @SuppressWarnings("unused")
    // because the method is not annotated, which is the purpose of this test class.
    public static class InvalidHandlerNoAnnotation extends TestCommandHandler {
        public RefProjectCreated handleTest(RefCreateProject cmd, CommandContext context) {
            return projectCreated(cmd.getProjectId());
        }
    }

    public static class InvalidHandlerNoParams extends TestCommandHandler {
        @Assign
        RefProjectCreated handleTest() {
            return RefProjectCreated.getDefaultInstance();
        }
    }

    public static class InvalidHandlerTooManyParams extends TestCommandHandler {
        @Assign
        RefProjectCreated handleTest(RefCreateProject cmd,
                                     CommandContext context,
                                     Object redundant) {
            return projectCreated(cmd.getProjectId());
        }
    }

    public static class InvalidHandlerOneNotMsgParam extends TestCommandHandler {
        @Assign
        RefProjectCreated handleTest(Exception invalid) {
            return RefProjectCreated.getDefaultInstance();
        }
    }

    public static class InvalidHandlerTwoParamsFirstInvalid extends TestCommandHandler {
        @Assign
        RefProjectCreated handleTest(Exception invalid, CommandContext context) {
            return RefProjectCreated.getDefaultInstance();
        }
    }

    public static class InvalidHandlerTwoParamsSecondInvalid extends TestCommandHandler {
        @Assign
        RefProjectCreated handleTest(RefCreateProject cmd, Exception invalid) {
            return projectCreated(cmd.getProjectId());
        }
    }

    public static class InvalidHandlerReturnsVoid extends TestCommandHandler {
        @Assign
        void handleTest(RefCreateProject cmd, CommandContext context) {
        }
    }

    /**
     * A command handler which always rejects the passed command.
     */
    public static class RejectingHandler extends TestCommandHandler {
        @Assign
        RefProjectCreated handleTest(RefCreateProject cmd) throws EntityAlreadyArchived {
            throw new EntityAlreadyArchived(Identifier.pack(cmd.getProjectId()));
        }
    }

    /**
     * An aggregate which always rejects the passed command.
     */
    public static class RejectingAggregate extends Aggregate<ProjectId, Empty, EmptyVBuilder> {
        public RejectingAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        RefProjectCreated on(RefCreateProject cmd) throws EntityAlreadyArchived {
            throw new EntityAlreadyArchived(Identifier.pack(cmd.getProjectId()));
        }

        @Apply
        void event(RefProjectCreated evt) {
            // Do nothing.
        }
    }

    public static class ProcessManagerDoingNothing
            extends ProcessManager<ProjectId, Empty, EmptyVBuilder> {

        public ProcessManagerDoingNothing(ProjectId id) {
            super(id);
        }

        @Assign
        Empty handle(RefCreateProject cmd) {
            return Empty.getDefaultInstance();
        }
    }

    public static class HandlerReturnsEmptyList extends TestCommandHandler {
        @Assign
        List<Message> handleTest(RefCreateProject cmd) {
            return emptyList();
        }
    }

    public static class HandlerReturnsEmpty extends TestCommandHandler {
        @Assign
        Empty handleTest(RefCreateProject cmd) {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * Abstract base for test environment command handlers.
     */
    public abstract static class TestCommandHandler extends AbstractCommandHandler {

        private static final String HANDLER_METHOD_NAME = "handleTest";

        protected TestCommandHandler() {
            super(BoundedContext.newBuilder()
                                .setMultitenant(true)
                                .build()
                                .getEventBus());
        }

        public Method getHandler() {
            Method[] methods = getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName()
                          .equals(HANDLER_METHOD_NAME)) {
                    return method;
                }
            }
            throw newIllegalStateException("No command handler method found: %s",
                                           HANDLER_METHOD_NAME);
        }
    }
}
