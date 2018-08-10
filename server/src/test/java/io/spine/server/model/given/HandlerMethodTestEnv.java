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

package io.spine.server.model.given;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.HandlerKey;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.MethodResult;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 * @author Dmytro Kuzmin
 */
public class HandlerMethodTestEnv {

    /** Prevents instantiation of this utility class. */
    private HandlerMethodTestEnv() {
    }

    @SuppressWarnings("UnusedParameters") // OK for test methods.
    public static class StubHandler {

        private boolean onInvoked;
        private boolean handleInvoked;

        private static void throwCheckedException(BoolValue message) throws Exception {
            throw new IOException("Throw new checked exception");
        }

        private static void throwRuntimeException(BoolValue message) throws RuntimeException {
            throw new RuntimeException("Throw new runtime exception");
        }

        public static Method getTwoParameterMethod() {
            Method method;
            Class<?> clazz = StubHandler.class;
            try {
                method = clazz.getMethod("on", StringValue.class, EventContext.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            return method;
        }

        public static Method getOneParameterMethod() {
            Method method;
            Class<?> clazz = StubHandler.class;
            try {
                method = clazz.getDeclaredMethod("handle", BoolValue.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            return method;
        }

        public static Method getMethodWithCheckedException() {
            Method method;
            Class<?> clazz = StubHandler.class;
            try {
                method = clazz.getDeclaredMethod("throwCheckedException", BoolValue.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            return method;
        }

        public static Method getMethodWithRuntimeException() {
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

        public boolean wasOnInvoked() {
            return onInvoked;
        }

        public boolean wasHandleInvoked() {
            return handleInvoked;
        }
    }

    public static class TwoParamMethod
        extends AbstractHandlerMethod<Object, EventClass, EventContext, MethodResult<Empty>> {

        public TwoParamMethod(Method method) {
            super(method);
        }

        @Override
        public EventClass getMessageClass() {
            return EventClass.from(rawMessageClass());
        }

        @Override
        protected MethodResult<Empty> toResult(Object target, Object rawMethodOutput) {
            return MethodResult.empty();
        }

        @Override
        public HandlerKey key() {
            throw new IllegalStateException("The method is not a target of the test.");
        }
    }

    public static class OneParamMethod
            extends AbstractHandlerMethod<Object, EventClass, Empty, MethodResult<Empty>> {

        public OneParamMethod(Method method) {
            super(method);
        }

        @Override
        protected MethodResult<Empty> toResult(Object target, Object rawMethodOutput) {
            return MethodResult.empty();
        }

        public static Factory factory() {
            return Factory.getInstance();
        }

        @Override
        public EventClass getMessageClass() {
            return EventClass.from(rawMessageClass());
        }

        private static class Factory extends MethodFactory<OneParamMethod> {

            private static final Factory INSTANCE = new Factory();

            private Factory() {
                super(OneParamMethod.class, method -> true);
            }

            private static Factory getInstance() {
                return INSTANCE;
            }

            @Override
            public void checkAccessModifier(Method method) {
                // Any access modifier is accepted for the test method.
            }

            @Override
            protected OneParamMethod doCreate(Method method) {
                return new OneParamMethod(method);
            }
        }

        @Override
        public HandlerKey key() {
            throw new IllegalStateException("The method is not a target of the test.");
        }
    }
}
