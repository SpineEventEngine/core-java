/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.reflect;

import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.server.command.Assign;
import org.spine3.server.command.CommandHandler;
import org.spine3.server.event.EventBus;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.testdata.TestEventFactory;
import org.spine3.testdata.TestEventMessageFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.testdata.TestCommands.createProjectMsg;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "TypeMayBeWeakened"})
public class CommandHandlerMethodShould {

    private final EventBus eventBus = TestEventFactory.newEventBus();


    @Test
    public void scan_target_for_handlers() {
        final TestCommandHandler handlerObject = new ValidHandlerOneParam();

        final MethodMap<CommandHandlerMethod> handlerMap = CommandHandlerMethod.scan(handlerObject);

        assertEquals(1, handlerMap.values().size());
        //noinspection ConstantConditions
        assertEquals(handlerObject.getHandler(), handlerMap.get(CreateProject.class).getMethod());
    }

    @Test
    public void log_warning_if_not_public_handler_found() {
        final TestCommandHandler handlerObject = new ValidHandlerButPrivate();
        final Method handlerMethod = handlerObject.getHandler();

        final MethodMap<CommandHandlerMethod> handlerMap = CommandHandlerMethod.scan(handlerObject);

        assertEquals(1, handlerMap.values().size());
        //noinspection ConstantConditions
        assertEquals(handlerMethod, handlerMap.get(CreateProject.class).getMethod());
        // TODO:2016-04-25:alexander.litus: check that a warning is logged (may require some refactoring)
    }

    @Test
    public void invoke_handler_method_which_returns_one_message() throws InvocationTargetException {
        final ValidHandlerTwoParams handlerObject = spy(new ValidHandlerTwoParams());
        final CommandHandlerMethod handler = new CommandHandlerMethod(handlerObject.getHandler());
        final CreateProject cmd = createProjectMsg(newUuid());

        final List<? extends Message> events = handler.invoke(handlerObject, cmd, CommandContext.getDefaultInstance());

        verify(handlerObject, times(1)).handleTest(cmd, CommandContext.getDefaultInstance());
        assertEquals(1, events.size());
        final ProjectCreated event = (ProjectCreated) events.get(0);
        assertEquals(cmd.getProjectId(), event.getProjectId());
    }

    @Test
    public void invoke_handler_method_and_return_message_list() throws InvocationTargetException {
        final ValidHandlerOneParamReturnsList handlerObject = spy(new ValidHandlerOneParamReturnsList());
        final CommandHandlerMethod handler = new CommandHandlerMethod(handlerObject.getHandler());
        final CreateProject cmd = createProjectMsg(newUuid());

        final List<? extends Message> events = handler.invoke(handlerObject, cmd, CommandContext.getDefaultInstance());

        verify(handlerObject, times(1)).handleTest(cmd);
        assertEquals(1, events.size());
        final ProjectCreated event = (ProjectCreated) events.get(0);
        assertEquals(cmd.getProjectId(), event.getProjectId());
    }

    @Test
    public void consider_handler_with_one_msg_param_valid() {
        final Method handler = new ValidHandlerOneParam().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    public void consider_handler_with_one_msg_param_which_returns_list_valid() {
        final Method handler = new ValidHandlerOneParamReturnsList().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    public void consider_handler_with_msg_and_context_params_valid() {
        final Method handler = new ValidHandlerTwoParams().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    public void consider_handler_with_msg_and_context_params_which_returns_list_valid() {
        final Method handler = new ValidHandlerTwoParamsReturnsList().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    public void consider_not_public_handler_valid() {
        final Method method = new ValidHandlerButPrivate().getHandler();

        assertIsCommandHandler(method, true);
    }

    @Test
    public void consider_not_annotated_handler_invalid() {
        final Method handler = new InvalidHandlerNoAnnotation().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_without_params_invalid() {
        final Method handler = new InvalidHandlerNoParams().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_with_too_many_params_invalid() {
        final Method handler = new InvalidHandlerTooManyParams().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_with_one_invalid_param_invalid() {
        final Method handler = new InvalidHandlerOneNotMsgParam().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_with_first_not_message_param_invalid() {
        final Method handler = new InvalidHandlerTwoParamsFirstInvalid().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_with_second_not_context_param_invalid() {
        final Method handler = new InvalidHandlerTwoParamsSecondInvalid().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_void_handler_invalid() {
        final Method handler = new InvalidHandlerReturnsVoid().getHandler();

        assertIsCommandHandler(handler, false);
    }

    private static void assertIsCommandHandler(Method handler, boolean isHandler) {
        assertEquals(isHandler, CommandHandlerMethod.PREDICATE.apply(handler));
    }

    /*
     * Valid handlers
     */

    private class ValidHandlerOneParam extends TestCommandHandler {
        @Assign
        @SuppressWarnings("unused")
        public ProjectCreated handleTest(CreateProject cmd) {
            return TestEventMessageFactory.projectCreatedMsg(cmd.getProjectId());
        }
    }

    private class ValidHandlerOneParamReturnsList extends TestCommandHandler {
        @Assign
        public List<Message> handleTest(CreateProject cmd) {
            final List<Message> result = newLinkedList();
            result.add(TestEventMessageFactory.projectCreatedMsg(cmd.getProjectId()));
            return result;
        }
    }

    private class ValidHandlerTwoParams extends TestCommandHandler {
        @Assign
        @SuppressWarnings("unused")
        public ProjectCreated handleTest(CreateProject cmd, CommandContext context) {
            return TestEventMessageFactory.projectCreatedMsg(cmd.getProjectId());
        }
    }

    private class ValidHandlerTwoParamsReturnsList extends TestCommandHandler {
        @Assign
        @SuppressWarnings("unused")
        public List<Message> handleTest(CreateProject cmd, CommandContext context) {
            final List<Message> result = newLinkedList();
            result.add(TestEventMessageFactory.projectCreatedMsg(cmd.getProjectId()));
            return result;
        }
    }

    private class ValidHandlerButPrivate extends TestCommandHandler {
        @Assign
        @SuppressWarnings("unused")
        private ProjectCreated handleTest(CreateProject cmd) {
            return TestEventMessageFactory.projectCreatedMsg(cmd.getProjectId());
        }
    }

    /*
     * Invalid handlers
     */

    private class InvalidHandlerNoAnnotation extends TestCommandHandler {
        @SuppressWarnings("unused")
        public ProjectCreated handleTest(CreateProject cmd, CommandContext context) {
            return TestEventMessageFactory.projectCreatedMsg(cmd.getProjectId());
        }
    }

    private class InvalidHandlerNoParams extends TestCommandHandler {
        @Assign
        @SuppressWarnings("unused")
        public ProjectCreated handleTest() {
            return ProjectCreated.getDefaultInstance();
        }
    }

    private class InvalidHandlerTooManyParams extends TestCommandHandler {
        @Assign
        @SuppressWarnings("unused")
        public ProjectCreated handleTest(CreateProject cmd, CommandContext context, Object redundant) {
            return TestEventMessageFactory.projectCreatedMsg(cmd.getProjectId());
        }
    }

    private class InvalidHandlerOneNotMsgParam extends TestCommandHandler {
        @Assign
        @SuppressWarnings("unused")
        public ProjectCreated handleTest(Exception invalid) {
            return ProjectCreated.getDefaultInstance();
        }
    }

    private class InvalidHandlerTwoParamsFirstInvalid extends TestCommandHandler {
        @Assign
        @SuppressWarnings("unused")
        public ProjectCreated handleTest(Exception invalid, CommandContext context) {
            return ProjectCreated.getDefaultInstance();
        }
    }

    private class InvalidHandlerTwoParamsSecondInvalid extends TestCommandHandler {
        @Assign
        @SuppressWarnings("unused")
        public ProjectCreated handleTest(CreateProject cmd, Exception invalid) {
            return TestEventMessageFactory.projectCreatedMsg(cmd.getProjectId());
        }
    }

    private class InvalidHandlerReturnsVoid extends TestCommandHandler {
        @Assign
        @SuppressWarnings("unused")
        public void handleTest(CreateProject cmd, CommandContext context) {
        }
    }

    private abstract class TestCommandHandler extends CommandHandler {

        private static final String HANDLER_METHOD_NAME = "handleTest";

        protected TestCommandHandler() {
            super(newUuid(), eventBus);
        }

        public Method getHandler() {
            final Method[] methods = getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName()
                          .equals(HANDLER_METHOD_NAME)) {
                    return method;
                }
            }
            throw new RuntimeException("No command handler method found: " + HANDLER_METHOD_NAME);
        }
    }
}
