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

package org.spine3.server.internal;

import com.google.common.base.Predicate;
import com.google.protobuf.Message;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The wrapper for a command handler method of an Aggregate Root.
 *
 * @author Alexander Litus
 */
@SuppressWarnings("UtilityClass")
public class AggregateCommandHandler extends CommandHandlerMethod {


    public static final Predicate<Method> IS_AGGREGATE_COMMAND_HANDLER_PREDICATE = new Predicate<Method>() {
        @Override
        public boolean apply(@Nullable Method method) {
            checkNotNull(method);
            return isAggregateCommandHandler(method);
        }
    };

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param target object to which the method applies
     * @param method subscriber method
     */
    public AggregateCommandHandler(Object target, Method method) {
        super(target, method);
    }

    /**
     * Checks if a method is a command handler of an aggregate root.
     *
     * @param method a method to check
     * @return {@code true} if the method is a command handler, {@code false} otherwise
     */
    public static boolean isAggregateCommandHandler(Method method) {
        final boolean isAnnotated = CommandHandlerMethod.isAnnotatedCorrectly(method);
        if (!isAnnotated){
            return false;
        }
        final boolean acceptsCorrectParams = CommandHandlerMethod.acceptsCorrectParameters(method);
        if (!acceptsCorrectParams) {
            return false;
        }
        final boolean returnsMessageOrList = returnsMessageOrList(method);
        return returnsMessageOrList;
    }

    private static boolean returnsMessageOrList(Method method) {
        final Class<?> returnType = method.getReturnType();
        final boolean result = Message.class.isAssignableFrom(returnType) || List.class.equals(returnType);
        return result;
    }
}
