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

package io.spine.server.aggregate.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.HandlerKey;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.HandlerMethodPredicate;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodPredicate;
import io.spine.server.model.MethodResult;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import static io.spine.server.model.MethodAccessChecker.forMethod;

/**
 * A wrapper for event applier method.
 *
 * @author Alexander Yevsyukov
 */
public final class EventApplier
        extends AbstractHandlerMethod<Aggregate, EventClass, Empty, MethodResult<Empty>> {

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    private EventApplier(Method method) {
        super(method);
    }

    @Override
    public EventClass getMessageClass() {
        return EventClass.of(rawMessageClass());
    }

    @Override
    public HandlerKey key() {
        return HandlerKey.of(getMessageClass());
    }

    static EventApplier from(Method method) {
        return new EventApplier(method);
    }

    @VisibleForTesting
    static Predicate<Method> predicate() {
        return factory().getPredicate();
    }

    public static AbstractHandlerMethod.Factory<EventApplier> factory() {
        return Factory.INSTANCE;
    }

    /**
     * Invokes the applier method.
     *
     * <p>The method {@linkplain HandlerMethod#invoke(Object, Message, Message) delegates}
     * the invocation passing {@linkplain Empty#getDefaultInstance() empty message}
     * as the context parameter because event appliers do not have a context parameter.
     *
     * <p>Such redirection is correct because {@linkplain #getParamCount()} the number of parameters}
     * is set to one during instance construction.
     */
    @SuppressWarnings("CheckReturnValue") // since method appliers do not return values
    public void invoke(Aggregate aggregate, Message message) {
        invoke(aggregate, message, Empty.getDefaultInstance());
    }

    @Override
    protected MethodResult<Empty> toResult(Aggregate target, Object rawMethodOutput) {
        return MethodResult.empty();
    }

    /** The factory for filtering methods that match {@code EventApplier} specification. */
    private static class Factory extends AbstractHandlerMethod.Factory<EventApplier> {

        private static final Factory INSTANCE = new Factory();

        @Override
        public Class<EventApplier> getMethodClass() {
            return EventApplier.class;
        }

        @Override
        public Predicate<Method> getPredicate() {
            return Filter.INSTANCE;
        }

        @Override
        public void checkAccessModifier(Method method) {
            MethodAccessChecker checker = forMethod(method);
            checker.checkPrivate("Event applier method {} must be declared 'private'.");
        }

        @Override
        protected EventApplier doCreate(Method method) {
            return from(method);
        }
    }

    /**
     * The predicate for filtering event applier methods.
     */
    private static class Filter extends HandlerMethodPredicate<Empty> {

        private static final MethodPredicate INSTANCE = new Filter();

        private static final int NUMBER_OF_PARAMS = 1;
        private static final int EVENT_PARAM_INDEX = 0;

        private Filter() {
            super(Apply.class, Empty.class);
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod") // because we override the checking.
        @Override
        protected boolean verifyParams(Method method) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean paramCountIsValid = parameterTypes.length == NUMBER_OF_PARAMS;
            if (!paramCountIsValid) {
                return false;
            }
            Class<?> paramType = parameterTypes[EVENT_PARAM_INDEX];
            boolean paramIsMessage = Message.class.isAssignableFrom(paramType);
            return paramIsMessage;
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            boolean isVoid = Void.TYPE.equals(method.getReturnType());
            return isVoid;
        }
    }
}
