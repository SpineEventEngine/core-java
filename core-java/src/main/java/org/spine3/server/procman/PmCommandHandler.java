/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.procman;

import com.google.common.base.Predicate;
import com.google.protobuf.Message;
import org.spine3.server.internal.CommandHandlerMethod;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;

/**
 * The wrapper for a command handler method of Process Manager.
 *
 * @author Alexander Litus
 */
class PmCommandHandler extends CommandHandlerMethod {

    static final Predicate<Method> IS_PM_COMMAND_HANDLER = new Predicate<Method>() {
        @Override
        public boolean apply(@Nullable Method method) {
            checkNotNull(method);
            return isProcessManagerCommandHandler(method);
        }
    };

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param target object to which the method applies
     * @param method subscriber method
     */
    PmCommandHandler(Object target, Method method) {
        super(target, method);
    }

    /**
     * Checks if a method is a command handler of Process Manager.
     *
     * @param method a method to check
     * @return {@code true} if the method is a command handler, {@code false} otherwise
     */
    static boolean isProcessManagerCommandHandler(Method method) {
        if (!isAnnotatedCorrectly(method)){
            return false;
        }
        if (!acceptsCorrectParams(method)) {
            return false;
        }
        final boolean isReturnTypeCorrect = isReturnTypeCorrect(method);
        return isReturnTypeCorrect;
    }

    private static boolean isReturnTypeCorrect(Method method) {
        final Class<?> returnType = method.getReturnType();

        if (Message.class.isAssignableFrom(returnType)) {
            return true;
        }
        if (List.class.isAssignableFrom(returnType)) {
            return true;
        }
        //noinspection RedundantIfStatement
        if (Void.TYPE.equals(returnType)) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @param handlingResult the command handler method return value.
     *                       Could be a {@link Message}, a list of messages or {@code null}.
     */
    @Override
    protected List<? extends Message> commandHandlingResultToEvents(@Nullable Object handlingResult) {
        if (handlingResult == null) {
            return emptyList();
        }
        final List<? extends Message> result = super.commandHandlingResultToEvents(handlingResult);
        return result;
    }
}
