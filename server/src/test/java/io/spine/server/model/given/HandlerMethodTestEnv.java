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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.EventMessage;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.HandlerKey;
import io.spine.server.model.MethodResult;
import io.spine.server.model.declare.AccessModifier;
import io.spine.server.model.declare.MethodSignature;
import io.spine.server.model.declare.ParameterSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static com.google.common.collect.ImmutableSet.of;
import static io.spine.server.model.declare.AccessModifier.PACKAGE_PRIVATE;
import static io.spine.server.model.declare.AccessModifier.PRIVATE;
import static io.spine.server.model.declare.AccessModifier.PROTECTED;
import static io.spine.server.model.declare.AccessModifier.PUBLIC;
import static io.spine.server.model.declare.MethodParams.consistsOfSingle;
import static io.spine.server.model.declare.MethodParams.consistsOfTwo;

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
            extends AbstractHandlerMethod<Object,
                                          EventMessage,
                                          EventClass,
                                          EventEnvelope,
                                          MethodResult<Empty>> {

        public TwoParamMethod(Method method,
                              ParameterSpec<EventEnvelope> parameterSpec) {
            super(method, parameterSpec);
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
            extends AbstractHandlerMethod<Object,
                                          EventMessage,
                                          EventClass,
                                          EventEnvelope,
            MethodResult<Empty>> {

        public OneParamMethod(Method method, ParameterSpec<EventEnvelope> parameterSpec) {
            super(method, parameterSpec);
        }

        @Override
        protected MethodResult<Empty> toResult(Object target, Object rawMethodOutput) {
            return MethodResult.empty();
        }

        @Override
        public EventClass getMessageClass() {
            return EventClass.from(rawMessageClass());
        }

        @Override
        public HandlerKey key() {
            throw new IllegalStateException("The method is not a target of the test.");
        }
    }

    @Immutable
    public enum OneParamSpec implements ParameterSpec<EventEnvelope> {

        INSTANCE;

        @Override
        public boolean matches(Class<?>[] methodParams) {
            return consistsOfSingle(methodParams, EventMessage.class);
        }

        @Override
        public Object[] extractArguments(EventEnvelope envelope) {
            return new Object[]{envelope.getMessage()};
        }
    }

    public static class OneParamSignature extends MethodSignature<OneParamMethod, EventEnvelope> {

        public OneParamSignature() {
            super(Annotation.class);
        }

        @Override
        public Class<? extends ParameterSpec<EventEnvelope>> getParamSpecClass() {
            return OneParamSpec.class;
        }

        @Override
        protected ImmutableSet<AccessModifier> getAllowedModifiers() {
            return allModifiers();
        }

        @Override
        protected ImmutableSet<Class<?>> getValidReturnTypes() {
            return of(Object.class);
        }

        @Override
        public OneParamMethod doCreate(Method method, ParameterSpec<EventEnvelope> parameterSpec) {
            return new OneParamMethod(method, parameterSpec);
        }
    }

    @Immutable
    public enum TwoParamSpec implements ParameterSpec<EventEnvelope> {

        INSTANCE;

        @Override
        public boolean matches(Class<?>[] methodParams) {
            return consistsOfTwo(methodParams, Message.class, EventContext.class);
        }

        @Override
        public Object[] extractArguments(EventEnvelope envelope) {
            return new Object[]{envelope.getMessage(), envelope.getEventContext()};
        }
    }

    private static ImmutableSet<AccessModifier> allModifiers() {
        return of(PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE);
    }
}
