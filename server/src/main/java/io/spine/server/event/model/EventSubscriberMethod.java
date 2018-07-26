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

package io.spine.server.event.model;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.HandlerKey;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodPredicate;
import io.spine.server.model.MethodResult;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import static io.spine.core.Rejections.isRejection;
import static io.spine.server.model.MethodAccessChecker.forMethod;

/**
 * A wrapper for an event subscriber method.
 *
 * @author Alexander Yevsyukov
 * @see Subscribe
 */
public final class EventSubscriberMethod
        extends AbstractHandlerMethod<EventClass, EventContext, MethodResult<Empty>> {

    /** Creates a new instance. */
    private EventSubscriberMethod(Method method) {
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

    public static EventSubscriberMethod from(Method method) {
        return new EventSubscriberMethod(method);
    }

    /** Returns the factory for filtering and creating event subscriber methods. */
    public static AbstractHandlerMethod.Factory<EventSubscriberMethod> factory() {
        return Factory.getInstance();
    }

    @CanIgnoreReturnValue // since event subscriber methods do not return values
    @Override
    public MethodResult<Empty> invoke(Object target, Message message, EventContext context) {
        ensureExternalMatch(context.getExternal());
        return super.invoke(target, message, context);
    }

    @Override
    protected MethodResult<Empty> toResult(Object rawMethodOutput, Object target) {
        return MethodResult.empty();
    }

    /**
     * The factory for creating {@linkplain EventSubscriberMethod event subscriber} methods.
     */
    private static class Factory extends AbstractHandlerMethod.Factory<EventSubscriberMethod> {

        private static final Factory INSTANCE = new Factory();

        private static Factory getInstance() {
            return INSTANCE;
        }

        @Override
        public Class<EventSubscriberMethod> getMethodClass() {
            return EventSubscriberMethod.class;
        }

        @Override
        public Predicate<Method> getPredicate() {
            return Filter.INSTANCE;
        }

        @Override
        public void checkAccessModifier(Method method) {
            MethodAccessChecker checker = forMethod(method);
            checker.checkPublic("Event subscriber {} must be declared 'public'");
        }

        @Override
        protected EventSubscriberMethod doCreate(Method method) {
            return from(method);
        }
    }

    /**
     * The predicate class allowing to filter event subscriber methods.
     *
     * <p>Please see {@link Subscribe} annotation for more information.
     */
    private static class Filter extends EventMethodPredicate {

        private static final MethodPredicate INSTANCE = new Filter();

        private Filter() {
            super(Subscribe.class);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Filters out methods that accept rejection messages as the first parameter.
         */
        @Override
        protected boolean verifyParams(Method method) {
            if (super.verifyParams(method)) {
                @SuppressWarnings("unchecked") // The case is safe since super returned `true`.
                Class<? extends Message> firstParameter =
                        (Class<? extends Message>) method.getParameterTypes()[0];
                boolean isRejection = isRejection(firstParameter);
                return !isRejection;
            }
            return false;
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            boolean isVoid = Void.TYPE.equals(method.getReturnType());
            return isVoid;
        }
    }
}
