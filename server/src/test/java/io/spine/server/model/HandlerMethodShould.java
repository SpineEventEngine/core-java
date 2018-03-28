/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Predicate;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.test.Tests;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public class HandlerMethodShould {

    private final HandlerMethod.Factory<OneParamMethod> factory = OneParamMethod.factory();

    private HandlerMethod<EventClass, EventContext> twoParamMethod;
    private HandlerMethod<EventClass, Empty> oneParamMethod;

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
    public void return_first_param_type() {
        assertEquals(BoolValue.class, HandlerMethod.getFirstParamType(oneParamMethod.getMethod()));
    }

    @Test
    public void invoke_the_method_with_two_parameters() throws InvocationTargetException {
        twoParamMethod.invoke(target,
                              StringValue.getDefaultInstance(),
                              EventContext.getDefaultInstance());

        assertTrue(((StubHandler) target).wasOnInvoked());
    }

    @Test
    public void invoke_the_method_with_one_parameter() throws InvocationTargetException {
        oneParamMethod.invoke(target, BoolValue.getDefaultInstance(), Empty.getDefaultInstance());

        assertTrue(((StubHandler) target).wasHandleInvoked());
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
        final HandlerMethod<EventClass, EventContext> anotherMethod =
                new TwoParamMethod(StubHandler.getTwoParameterMethod());

        assertTrue(twoParamMethod.equals(anotherMethod));
    }

    @Test
    public void have_hashCode() {
        assertNotEquals(System.identityHashCode(twoParamMethod), twoParamMethod.hashCode());
    }

    @Test(expected = IllegalStateException.class)
    public void do_not_be_created_from_method_with_checked_exception() {
        //noinspection ResultOfMethodCallIgnored
        factory.create(StubHandler.getMethodWithCheckedException());
    }

    @Test
    public void be_normally_created_from_method_with_runtime_exception() {
        final OneParamMethod method =
                factory.create(StubHandler.getMethodWithRuntimeException());
        assertEquals(StubHandler.getMethodWithRuntimeException(), method.getMethod());
    }

    /*
     * Test environment classes.
     *****************************/

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

        private static void throwCheckedException(BoolValue message) throws Exception {
            throw new IOException("Throw new checked exception");
        }

        private static void throwRuntimeException(BoolValue message) throws RuntimeException {
            throw new RuntimeException("Throw new runtime exception");
        }

        private static Method getTwoParameterMethod() {
            final Method method;
            final Class<?> clazz = StubHandler.class;
            try {
                method = clazz.getMethod("on", StringValue.class, EventContext.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            return method;
        }

        private static Method getOneParameterMethod() {
            final Method method;
            final Class<?> clazz = StubHandler.class;
            try {
                //noinspection DuplicateStringLiteralInspection
                method = clazz.getDeclaredMethod("handle", BoolValue.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            return method;
        }

        private static Method getMethodWithCheckedException() {
            final Method method;
            final Class<?> clazz = StubHandler.class;
            try {
                method = clazz.getDeclaredMethod("throwCheckedException", BoolValue.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            return method;
        }

        private static Method getMethodWithRuntimeException() {
            final Method method;
            final Class<?> clazz = StubHandler.class;
            try {
                method = clazz.getDeclaredMethod("throwRuntimeException", BoolValue.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            return method;
        }

        private boolean wasOnInvoked() {
            return onInvoked;
        }

        private boolean wasHandleInvoked() {
            return handleInvoked;
        }
    }

    private static class TwoParamMethod extends HandlerMethod<EventClass, EventContext> {

        private TwoParamMethod(Method method) {
            super(method);
        }

        @Override
        public EventClass getMessageClass() {
            return EventClass.of(rawMessageClass());
        }

        @Override
        public HandlerKey key() {
            throw new IllegalStateException("The method is not a target of the test.");
        }
    }

    private static class OneParamMethod extends HandlerMethod<EventClass, Empty> {

        private OneParamMethod(Method method) {
            super(method);
        }

        @Override
        public EventClass getMessageClass() {
            return EventClass.of(rawMessageClass());
        }

        @Override
        public HandlerKey key() {
            throw new IllegalStateException("The method is not a target of the test.");
        }

        public static Factory factory() {
            return Factory.getInstance();
        }

        private static class Factory extends HandlerMethod.Factory<OneParamMethod> {

            private static final OneParamMethod.Factory INSTANCE = new OneParamMethod.Factory();

            private static OneParamMethod.Factory getInstance() {
                return INSTANCE;
            }

            @Override
            public Class<OneParamMethod> getMethodClass() {
                return OneParamMethod.class;
            }

            @Override
            public Predicate<Method> getPredicate() {
                throw new IllegalStateException("The test factory cannot provide the predicate.");
            }

            @Override
            public void checkAccessModifier(Method method) {
                // Any access modifier is accepted for the test method.
            }

            @Override
            protected OneParamMethod createFromMethod(Method method) {
                return new OneParamMethod(method);
            }
        }
    }
}
