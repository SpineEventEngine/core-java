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

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.EventContext;
import org.spine3.test.Tests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.util.Exceptions.wrapped;

@SuppressWarnings("InstanceMethodNamingConvention")
public class HandlerMethodShould {

    private HandlerMethod<EventContext> twoParamMethod;
    private HandlerMethod<Empty> oneParamMethod;

    private Object target;

    @Before
    public void setUp() {
        target = new StubHandler();
        twoParamMethod = new TwoParamMethod(StubHandler.getTwoParameterMethod());
        oneParamMethod = new OneParamMethod(StubHandler.getOneParameterMethod());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_method() {
        //noinspection ResultOfObjectAllocationIgnored
        new TwoParamMethod(Tests.<Method>nullRef());
    }

    @Test
    public void return_method() {
        assertEquals(StubHandler.getTwoParameterMethod(), twoParamMethod.getMethod());
    }

    @Test
    public void check_if_public() {
        assertTrue(twoParamMethod.isPublic());
    }

    @Test
    public void check_if_private() {
        assertTrue(oneParamMethod.isPrivate());
    }

    @Test
    public void have_log_warning_method() {
        HandlerMethod.warnOnWrongModifier("", oneParamMethod.getMethod());
    }

    @Test
    public void return_first_param_type() {
        assertEquals(BoolValue.class, HandlerMethod.getFirstParamType(oneParamMethod.getMethod()));
    }

    @Test
    public void invoke_the_method_with_two_parameters() throws InvocationTargetException {
        twoParamMethod.invoke(target, StringValue.getDefaultInstance(), EventContext.getDefaultInstance());

        assertTrue(((StubHandler)target).wasOnInvoked());
    }

    @Test
    public void invoke_the_method_with_one_parameter() throws InvocationTargetException {
        oneParamMethod.invoke(target, BoolValue.getDefaultInstance(), Empty.getDefaultInstance());

        assertTrue(((StubHandler)target).wasHandleInvoked());
    }

    @Test
    public void return_full_name_in_toString() {
        assertEquals(twoParamMethod.getFullName(), twoParamMethod.toString());
    }

    @Test
    public void be_equal_to_itself() {
        //noinspection EqualsWithItself
        assertTrue(twoParamMethod.equals(twoParamMethod));
    }

    @Test
    public void be_not_equal_to_null() {
        //noinspection ObjectEqualsNull
        assertFalse(oneParamMethod.equals(null));
    }

    @Test
    public void be_not_equal_to_another_class_instance() {
        //noinspection EqualsBetweenInconvertibleTypes
        assertFalse(twoParamMethod.equals(oneParamMethod));
    }

    @Test
    public void compare_fields_in_equals() {
        final HandlerMethod<EventContext> anotherMethod = new TwoParamMethod(StubHandler.getTwoParameterMethod());

        assertTrue(twoParamMethod.equals(anotherMethod));
    }

    @Test
    public void have_hashCode() {
        assertNotEquals(System.identityHashCode(twoParamMethod), twoParamMethod.hashCode());
    }

    @SuppressWarnings("UnusedParameters") // OK for test methods.
    private static class StubHandler {

        private boolean onInvoked;
        private boolean handleInvoked;

        public void on(StringValue message, EventContext context) {
            onInvoked = true;
        }

        @SuppressWarnings("unused") // The method is used via reflection.
        private void handle(BoolValue message) {
            handleInvoked = true;
        }

        /* package */ static Method getTwoParameterMethod() {
            final Method method;
            final Class<?> clazz = StubHandler.class;
            try {
                method = clazz.getMethod("on", StringValue.class, EventContext.class);
            } catch (NoSuchMethodException e) {
                throw wrapped(e);
            }
            return method;
        }

        /* package */ static Method getOneParameterMethod() {
            final Method method;
            final Class<?> clazz = StubHandler.class;
            try {
                //noinspection DuplicateStringLiteralInspection
                method = clazz.getDeclaredMethod("handle", BoolValue.class);
            } catch (NoSuchMethodException e) {
                throw wrapped(e);
            }
            return method;
        }

        /* package */ boolean wasOnInvoked() {
            return onInvoked;
        }

        /* package */ boolean wasHandleInvoked() {
            return handleInvoked;
        }
    }

    private static class TwoParamMethod extends HandlerMethod<EventContext> {

        protected TwoParamMethod(Method method) {
            super(method);
        }
    }

    private static class OneParamMethod extends HandlerMethod<Empty> {

        protected OneParamMethod(Method method) {
            super(method);
        }
    }
}
