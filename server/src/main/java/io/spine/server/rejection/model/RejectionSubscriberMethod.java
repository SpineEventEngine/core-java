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
package io.spine.server.rejection.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.RejectionContext;
import io.spine.core.Subscribe;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodPredicate;
import io.spine.server.model.MethodResult;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import static io.spine.server.model.MethodAccessChecker.forMethod;

/**
 * A wrapper for a rejection subscriber method.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 * @author Alexander Yevsyukov
 */
public final class RejectionSubscriberMethod
        extends RejectionHandlerMethod<Object, MethodResult<Empty>> {

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    @VisibleForTesting
    RejectionSubscriberMethod(Method method) {
        super(method);
    }

    /**
     * Invokes the wrapped handler method to handle {@code rejectionMessage},
     * {@code commandMessage} with the passed {@code context} of the {@code Command}.
     *
     * @param target           the target object on which call the method
     * @param rejectionMessage the rejection message to handle
     * @param context          the context of the rejection
     */
    @CanIgnoreReturnValue
    @Override
    public
    MethodResult<Empty> invoke(Object target, Message rejectionMessage, RejectionContext context) {
        ensureExternalMatch(context.getExternal());
        doInvoke(target, rejectionMessage, context);
        return MethodResult.empty();
    }

    @Override
    protected MethodResult<Empty> toResult(Object target, Object rawMethodOutput) {
        return MethodResult.empty();
    }

    /** Returns the factory for filtering and creating rejection subscriber methods. */
    public static AbstractHandlerMethod.Factory<RejectionSubscriberMethod> factory() {
        return Factory.getInstance();
    }

    /**
     * The factory for filtering methods that match {@code RejectionSubscriberMethod} specification.
     */
    private static class Factory extends AbstractHandlerMethod.Factory<RejectionSubscriberMethod> {

        @Override
        public Class<RejectionSubscriberMethod> getMethodClass() {
            return RejectionSubscriberMethod.class;
        }

        @Override
        public Predicate<Method> getPredicate() {
            return Filter.INSTANCE;
        }

        @Override
        public void checkAccessModifier(Method method) {
            MethodAccessChecker checker = forMethod(method);
            checker.checkPublic("Rejection subscriber {} must be declared 'public'");
        }

        @Override
        protected RejectionSubscriberMethod doCreate(Method method) {
            RejectionSubscriberMethod result = new RejectionSubscriberMethod(method);
            return result;
        }

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final Factory value = new Factory();
        }

        private static Factory getInstance() {
            return Singleton.INSTANCE.value;
        }
    }

    /**
     * The predicate class allowing to filter rejection subscriber methods.
     *
     * <p>Please see {@link Subscribe} annotation for more information.
     */
    private static class Filter extends AbstractPredicate {

        private static final MethodPredicate INSTANCE = new Filter();

        private Filter() {
            super(Subscribe.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            boolean isVoid = Void.TYPE.equals(method.getReturnType());
            return isVoid;
        }
    }
}
