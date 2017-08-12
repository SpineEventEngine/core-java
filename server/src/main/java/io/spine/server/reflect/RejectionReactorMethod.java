/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package io.spine.server.reflect;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.React;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionContext;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

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
     * Invokes the reactor method in the passed object.
     */
    public static List<? extends Message> invokeFor(Object target,
                                                    Message rejectionMessage,
                                                    Message commandMessage,
                                                    RejectionContext context) {
        checkNotNull(target);
        checkNotNull(rejectionMessage);
        checkNotNull(commandMessage);
        checkNotNull(context);

        final RejectionReactorMethod method =
                getMethod(target.getClass(), rejectionMessage, commandMessage);
        final List<? extends Message> result =
                method.invoke(target, rejectionMessage, context);
        return result;
    }

    /**
     * Obtains the method for handling the rejection in the passed class.
     *
     * @throws IllegalStateException if the passed class does not have an rejection handling method
     *                               for the class of the passed message
     */
    public static RejectionReactorMethod getMethod(Class<?> cls,
                                                   Message rejectionMessage,
                                                   Message commandMessage) {
        checkNotNull(cls);
        checkNotNull(rejectionMessage);
        checkNotNull(commandMessage);

        final Class<? extends Message> rejectionClass = rejectionMessage.getClass();
        final MethodRegistry registry = MethodRegistry.getInstance();
        final RejectionReactorMethod method = registry.get(cls,
                                                           rejectionClass,
                                                           factory());
        if (method == null) {
            throw missingRejectionHandler(cls, rejectionClass);
        }
        return method;
    }

    @CheckReturnValue
    public static Set<RejectionClass> inspect(Class<?> cls) {
        return inspectWith(cls, predicate());
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
