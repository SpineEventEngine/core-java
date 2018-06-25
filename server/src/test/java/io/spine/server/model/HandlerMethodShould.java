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

package io.spine.server.model;

import com.google.common.base.Predicate;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.test.Tests;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public class HandlerMethodShould {

    private final HandlerMethod.Factory<OneParamMethod> factory = OneParamMethod.factory();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private HandlerMethod<EventClass, EventContext> twoParamMethod;
    private HandlerMethod<EventClass, Empty> oneParamMethod;
    private Object target;

    @Before
    public void setUp() {
        target = new StubHandler();
        twoParamMethod = new TwoParamMethod(StubHandler.getTwoParameterMethod());
        oneParamMethod = new OneParamMethod(StubHandler.getOneParameterMethod());
    }

    @Test
    @DisplayName("do not accept null method")
    void doNotAcceptNullMethod() {
        thrown.expect(NullPointerException.class);
        new TwoParamMethod(Tests.nullRef());
    }

    @Test
    @DisplayName("return method")
    void returnMethod() {
        assertEquals(StubHandler.getTwoParameterMethod(), twoParamMethod.getMethod());
    }

    @Test
    @DisplayName("check if public")
    void checkIfPublic() {
        assertTrue(twoParamMethod.isPublic());
    }

    @Test
    @DisplayName("check if private")
    void checkIfPrivate() {
        assertTrue(oneParamMethod.isPrivate());
    }

    @Test
    @DisplayName("return first param type")
    void returnFirstParamType() {
        assertEquals(BoolValue.class, HandlerMethod.getFirstParamType(oneParamMethod.getMethod()));
    }

    @SuppressWarnings("CheckReturnValue") // can ignore the result in this test
    @Test
    @DisplayName("invoke the method with two parameters")
    void invokeTheMethodWithTwoParameters() {
        twoParamMethod.invoke(target,
                              StringValue.getDefaultInstance(),
                              EventContext.getDefaultInstance());

        assertTrue(((StubHandler) target).wasOnInvoked());
    }

    @SuppressWarnings("CheckReturnValue") // can ignore the result in this test
    @Test
    @DisplayName("invoke the method with one parameter")
    void invokeTheMethodWithOneParameter() {
        oneParamMethod.invoke(target, BoolValue.getDefaultInstance(), Empty.getDefaultInstance());

        assertTrue(((StubHandler) target).wasHandleInvoked());
    }

    @Test
    @DisplayName("return full name in toString")
    void returnFullNameInToString() {
        assertEquals(twoParamMethod.getFullName(), twoParamMethod.toString());
    }

    @Test
    @DisplayName("be equal to itself")
    void beEqualToItself() {
        assertEquals(twoParamMethod, twoParamMethod);
    }

    @Test
    @DisplayName("be not equal to null")
    void beNotEqualToNull() {
        assertNotEquals(null, oneParamMethod);
    }

    @Test
    @DisplayName("be not equal to another class instance")
    void beNotEqualToAnotherClassInstance() {
        assertNotEquals(twoParamMethod, oneParamMethod);
    }

    @Test
    @DisplayName("compare fields in equals")
    void compareFieldsInEquals() {
        HandlerMethod<EventClass, EventContext> anotherMethod =
                new TwoParamMethod(StubHandler.getTwoParameterMethod());

        assertEquals(twoParamMethod, anotherMethod);
    }

    @Test
    @DisplayName("have hashCode")
    void haveHashCode() {
        assertNotEquals(System.identityHashCode(twoParamMethod), twoParamMethod.hashCode());
    }

    @Test
    @DisplayName("do not be created from method with checked exception")
    void doNotBeCreatedFromMethodWithCheckedException() {
        thrown.expect(IllegalStateException.class);
        factory.create(StubHandler.getMethodWithCheckedException());
    }

    @Test
    @DisplayName("be normally created from method with runtime exception")
    void beNormallyCreatedFromMethodWithRuntimeException() {
        OneParamMethod method = factory.create(StubHandler.getMethodWithRuntimeException());
        assertEquals(StubHandler.getMethodWithRuntimeException(), method.getMethod());
    }

    /*
     * Test environment classes.
     *****************************/

    @SuppressWarnings("UnusedParameters") // OK for test methods.
    private static class StubHandler {

        private boolean onInvoked;
        private boolean handleInvoked;

        private static void throwCheckedException(BoolValue message) throws Exception {
            throw new IOException("Throw new checked exception");
        }

        private static void throwRuntimeException(BoolValue message) throws RuntimeException {
            throw new RuntimeException("Throw new runtime exception");
        }

        private static Method getTwoParameterMethod() {
            Method method;
            Class<?> clazz = StubHandler.class;
            try {
                method = clazz.getMethod("on", StringValue.class, EventContext.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            return method;
        }

        private static Method getOneParameterMethod() {
            Method method;
            Class<?> clazz = StubHandler.class;
            try {
                method = clazz.getDeclaredMethod("handle", BoolValue.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            return method;
        }

        private static Method getMethodWithCheckedException() {
            Method method;
            Class<?> clazz = StubHandler.class;
            try {
                method = clazz.getDeclaredMethod("throwCheckedException", BoolValue.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            return method;
        }

        private static Method getMethodWithRuntimeException() {
            Method method;
            Class<?> clazz = StubHandler.class;
            try {
                method = clazz.getDeclaredMethod("throwRuntimeException", BoolValue.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            return method;
        }

        public void on(StringValue message, EventContext context) {
            onInvoked = true;
        }

        @SuppressWarnings("unused") // The method is used via reflection.
        private void handle(BoolValue message) {
            handleInvoked = true;
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

        public static Factory factory() {
            return Factory.getInstance();
        }        @Override
        public EventClass getMessageClass() {
            return EventClass.of(rawMessageClass());
        }

        private static class Factory extends HandlerMethod.Factory<OneParamMethod> {

            private static final Factory INSTANCE = new Factory();

            private static Factory getInstance() {
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
        }        @Override
        public HandlerKey key() {
            throw new IllegalStateException("The method is not a target of the test.");
        }
    }
}
