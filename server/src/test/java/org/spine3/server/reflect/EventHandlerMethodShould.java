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

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.spine3.base.EventContext;
import org.spine3.server.event.Subscribe;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;

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

    private static final ImmutableMap<String, Method> HANDLERS = getHandlers();

    private final EventHandlerMethod validHandlerOneParam =
            new EventHandlerMethod(HANDLERS.get("validHandlerOneParam"));

    private final EventHandlerMethod validHandlerTwoParams =
            new EventHandlerMethod(HANDLERS.get("validHandlerTwoParams"));

    @Test
    public void scan_target_for_handlers() {
        final TestEventHandler handlerObject = new TestEventHandler();

        final MethodMap<EventHandlerMethod> handlerMap = EventHandlerMethod.scan(handlerObject);

        assertEquals(2, handlerMap.values().size());
        assertEquals(validHandlerOneParam, handlerMap.get(ProjectStarted.class));
        assertEquals(validHandlerTwoParams, handlerMap.get(ProjectCreated.class));
    }

    @Test
    public void invoke_handler_method() throws InvocationTargetException {
        final TestEventHandler handlerObject = mock(TestEventHandler.class);
        final ProjectCreated msg = projectCreatedEvent();

        validHandlerTwoParams.invoke(handlerObject, msg, EventContext.getDefaultInstance());

        verify(handlerObject, times(1)).validHandlerTwoParams(msg, EventContext.getDefaultInstance());
    }

    @Test
    public void consider_handler_with_one_msg_param_valid() {
        final Method handler = validHandlerOneParam.getMethod();

        assertTrue(EventHandlerMethod.PREDICATE.apply(handler));
    }

    @Test
    public void consider_handler_with_msg_and_context_params_valid() {
        final Method handler = validHandlerTwoParams.getMethod();

        assertTrue(EventHandlerMethod.PREDICATE.apply(handler));
    }

    @Test
    public void consider_not_annotated_handler_invalid() {
        final Method handler = HANDLERS.get("invalidHandlerNoAnnotation");

        assertFalse(EventHandlerMethod.PREDICATE.apply(handler));
    }

    @Test
    public void consider_handler_without_params_invalid() {
        final Method handler = HANDLERS.get("invalidHandlerNoParams");

        assertFalse(EventHandlerMethod.PREDICATE.apply(handler));
    }

    @Test
    public void consider_handler_with_too_many_params_invalid() {
        final Method handler = HANDLERS.get("invalidHandlerTooManyParams");

        assertFalse(EventHandlerMethod.PREDICATE.apply(handler));
    }

    @Test
    public void consider_handler_with_one_invalid_param_invalid() {
        final Method handler = HANDLERS.get("invalidHandlerOneNotMsgParam");

        assertFalse(EventHandlerMethod.PREDICATE.apply(handler));
    }

    @Test
    public void consider_handler_with_first_not_message_param_invalid() {
        final Method handler = HANDLERS.get("invalidHandlerTwoParamsFirstInvalid");

        assertFalse(EventHandlerMethod.PREDICATE.apply(handler));
    }

    @Test
    public void consider_handler_with_second_not_context_param_invalid() {
        final Method handler = HANDLERS.get("invalidHandlerTwoParamsSecondInvalid");

        assertFalse(EventHandlerMethod.PREDICATE.apply(handler));
    }

    @Test
    public void consider_void_handler_invalid() {
        final Method handler = HANDLERS.get("invalidHandlerNotVoid");

        assertFalse(EventHandlerMethod.PREDICATE.apply(handler));
    }

    private static ImmutableMap<String, Method> getHandlers() {
        final Method[] methods = TestEventHandler.class.getDeclaredMethods();
        final ImmutableMap.Builder<String, Method> builder = ImmutableMap.builder();
        for (Method method : methods) {
            builder.put(method.getName(), method);
        }
        return builder.build();
    }

    @SuppressWarnings("unused")
    private static class TestEventHandler {

        @Subscribe
        public void validHandlerOneParam(ProjectStarted event) {
        }

        @Subscribe
        public void validHandlerTwoParams(ProjectCreated event, EventContext context) {
        }

        /*
         * Invalid handlers
         */

        public void invalidHandlerNoAnnotation(ProjectCreated event, EventContext context) {
        }

        @Subscribe
        public void invalidHandlerNoParams() {
        }

        @Subscribe
        public void invalidHandlerTooManyParams(ProjectCreated event, EventContext context, Object redundant) {
        }

        @Subscribe
        public void invalidHandlerOneNotMsgParam(Exception invalid) {
        }

        @Subscribe
        public void invalidHandlerTwoParamsFirstInvalid(Exception invalid, EventContext context) {
        }

        @Subscribe
        public void invalidHandlerTwoParamsSecondInvalid(ProjectCreated event, Exception invalid) {
        }

        @Subscribe
        public Object invalidHandlerNotVoid(ProjectCreated event, EventContext context) {
            return event;
        }
    }
}
