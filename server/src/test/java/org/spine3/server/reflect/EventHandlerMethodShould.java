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

import org.junit.Test;
import org.spine3.base.EventContext;
import org.spine3.server.event.Subscribe;
import org.spine3.test.project.event.ProjectCreated;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.spine3.testdata.TestEventMessageFactory.projectCreatedEvent;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class EventHandlerMethodShould {

    @Test
    public void scan_target_for_handlers() {
        final TestEventHandler handlerObject = new ValidEventHandlerOneParam();

        final MethodMap<EventHandlerMethod> handlerMap = EventHandlerMethod.scan(handlerObject);

        assertEquals(1, handlerMap.values().size());
        //noinspection ConstantConditions
        assertEquals(handlerObject.getHandler(), handlerMap.get(ProjectCreated.class).getMethod());
    }

    @Test
    public void invoke_handler_method() throws InvocationTargetException {
        final ValidEventHandlerTwoParams handlerObject = spy(new ValidEventHandlerTwoParams());
        final EventHandlerMethod handler = new EventHandlerMethod(handlerObject.getHandler());
        final ProjectCreated msg = projectCreatedEvent();

        handler.invoke(handlerObject, msg, EventContext.getDefaultInstance());

        verify(handlerObject, times(1)).handle(msg, EventContext.getDefaultInstance());
    }

    @Test
    public void consider_handler_with_one_msg_param_valid() {
        final Method handler = new ValidEventHandlerOneParam().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    public void consider_handler_with_msg_and_context_params_valid() {
        final Method handler = new ValidEventHandlerTwoParams().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    public void consider_not_annotated_handler_invalid() {
        final Method handler = new InvalidEventHandlerNoAnnotation().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_without_params_invalid() {
        final Method handler = new InvalidEventHandlerNoParams().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_with_too_many_params_invalid() {
        final Method handler = new InvalidEventHandlerTooManyParams().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_with_one_invalid_param_invalid() {
        final Method handler = new InvalidEventHandlerOneNotMsgParam().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_with_first_not_message_param_invalid() {
        final Method handler = new InvalidEventHandlerTwoParamsFirstInvalid().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_with_second_not_context_param_invalid() {
        final Method handler = new InvalidEventHandlerTwoParamsSecondInvalid().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_void_handler_invalid() {
        final Method handler = new InvalidEventHandlerNotVoid().getHandler();

        assertIsCommandHandler(handler, false);
    }

    private static void assertIsCommandHandler(Method handler, boolean isHandler) {
        assertEquals(isHandler, EventHandlerMethod.PREDICATE.apply(handler));
    }

    /*
     * Valid handlers
     */

    private static class ValidEventHandlerOneParam extends TestEventHandler {
        @Subscribe
        public void handle(ProjectCreated event) {
        }
    }

    private static class ValidEventHandlerTwoParams extends TestEventHandler {
        @Subscribe
        public void handle(ProjectCreated event, EventContext context) {
        }
    }

    /*
     * Invalid handlers
     */

    private static class InvalidEventHandlerNoAnnotation extends TestEventHandler {
        @SuppressWarnings("unused")
        public void handle(ProjectCreated event, EventContext context) {
        }
    }

    private static class InvalidEventHandlerNoParams extends TestEventHandler {
        @Subscribe
        public void handle() {
        }
    }

    private static class InvalidEventHandlerTooManyParams extends TestEventHandler {
        @Subscribe
        public void handle(ProjectCreated event, EventContext context, Object redundant) {
        }
    }

    private static class InvalidEventHandlerOneNotMsgParam extends TestEventHandler {
        @Subscribe
        public void handle(Exception invalid) {
        }
    }

    private static class InvalidEventHandlerTwoParamsFirstInvalid extends TestEventHandler {
        @Subscribe
        public void handle(Exception invalid, EventContext context) {
        }
    }

    private static class InvalidEventHandlerTwoParamsSecondInvalid extends TestEventHandler {
        @Subscribe
        public void handle(ProjectCreated event, Exception invalid) {
        }
    }

    private static class InvalidEventHandlerNotVoid extends TestEventHandler {
        @Subscribe
        public Object handle(ProjectCreated event, EventContext context) {
            return event;
        }
    }

    private abstract static class TestEventHandler {

        @SuppressWarnings("DuplicateStringLiteralInspection")
        private static final String HANDLER_METHOD_NAME = "handle";

        public Method getHandler() {
            final Method[] methods = getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(HANDLER_METHOD_NAME)) {
                    return method;
                }
            }
            throw new RuntimeException("No handler method found: " + HANDLER_METHOD_NAME);
        }
    }
}
