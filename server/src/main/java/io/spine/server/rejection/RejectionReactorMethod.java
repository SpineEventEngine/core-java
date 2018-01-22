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
package io.spine.server.rejection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.React;
import io.spine.core.RejectionContext;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.MethodPredicate;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static io.spine.server.model.HandlerMethods.ensureExternalMatch;

/**
 * A wrapper for a rejection reactor method.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 * @author Alexander Yevsyukov
 */
@Internal
public class RejectionReactorMethod extends RejectionHandlerMethod {

    /** The instance of the predicate to filter rejection reactor methods of a class. */
    private static final MethodPredicate PREDICATE = new FilterPredicate();

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method reactor method
     */
    @VisibleForTesting
    RejectionReactorMethod(Method method) {
        super(method);
    }

    /**
     * Invokes the wrapped handler method to handle {@code rejectionMessage},
     * {@code commandMessage} with the passed {@code context} of the {@code Command}.
     *
     * <p>Unlike the {@linkplain #invoke(Object, Message, Message) overloaded alternative method},
     * this one implies returning the value, being a reaction to the rejection passed.
     *
     * @param  target           the target object on which call the method
     * @param  rejectionMessage the rejection message to handle
     * @param  context          the context of the rejection
     * @return the list of event messages produced by the reacting method or empty list if no event
     *         messages were produced
     */
    @Override
    public List<? extends Message> invoke(Object target,
                                          Message rejectionMessage,
                                          RejectionContext context) {
        ensureExternalMatch(this, context.getExternal());

        final Object output = doInvoke(target, rejectionMessage, context);
        final List<? extends Message> eventMessages = toList(output);
        return eventMessages;
    }

    /** Returns the factory for filtering and creating rejection reactor methods. */
    public static HandlerMethod.Factory<RejectionReactorMethod> factory() {
        return Factory.getInstance();
    }

    static MethodPredicate predicate() {
        return PREDICATE;
    }

    /**
     * The factory for filtering methods that match {@code RejectionReactorMethod} specification.
     */
    private static class Factory implements HandlerMethod.Factory<RejectionReactorMethod> {

        @Override
        public Class<RejectionReactorMethod> getMethodClass() {
            return RejectionReactorMethod.class;
        }

        @Override
        public RejectionReactorMethod create(Method method) {
            final RejectionReactorMethod result = new RejectionReactorMethod(method);
            return result;
        }

        @Override
        public Predicate<Method> getPredicate() {
            return predicate();
        }

        @Override
        public void checkAccessModifier(Method method) {
            if (!Modifier.isPublic(method.getModifiers())) {
                warnOnWrongModifier("Rejection reactor {} must be declared 'public'",
                                    method);
            }
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
     * The predicate class allowing to filter rejection reactor methods.
     *
     * <p>Please see {@link React} annotation for more information.
     */
    private static class FilterPredicate extends RejectionFilterPredicate {

        private FilterPredicate() {
            super(React.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            final boolean result = returnsMessageOrIterable(method);
            return result;
        }
    }
}
