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
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import org.spine3.CommandClass;
import org.spine3.base.CommandContext;
import org.spine3.error.AccessLevelException;
import org.spine3.internal.MessageHandlerMethod;
import org.spine3.server.Assign;
import org.spine3.server.Repository;
import org.spine3.server.aggregate.AggregateRoot;
import org.spine3.util.MethodMap;
import org.spine3.util.Methods;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The wrapper for a command handler method.
 *
 * @author Alexander Yevsyukov
 */
public class CommandHandlerMethod extends MessageHandlerMethod<Object, CommandContext> {

    public static final Predicate<Method> isCommandHandlerPredicate = new Predicate<Method>() {
        @Override
        public boolean apply(@Nullable Method method) {
            checkNotNull(method);
            return isCommandHandler(method);
        }
    };

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param target object to which the method applies
     * @param method subscriber method
     */
    public CommandHandlerMethod(Object target, Method method) {
        super(target, method);
    }

    /**
     * Checks if a method is a command handler.
     *
     * @param method a method to check
     * @return {@code true} if the method is a command handler, {@code false} otherwise
     */
    public static boolean isCommandHandler(Method method) {

        boolean isAnnotated = method.isAnnotationPresent(Assign.class);

        Class<?>[] parameterTypes = method.getParameterTypes();

        //noinspection LocalVariableNamingConvention
        boolean acceptsMessageAndCommandContext =
                parameterTypes.length == 2
                        && Message.class.isAssignableFrom(parameterTypes[0])
                        && CommandContext.class.equals(parameterTypes[1]);

        boolean returnsMessageList = List.class.equals(method.getReturnType());
        boolean returnsMessage = Message.class.isAssignableFrom(method.getReturnType());

        //noinspection OverlyComplexBooleanExpression
        return isAnnotated
                && acceptsMessageAndCommandContext
                && (returnsMessageList || returnsMessage);
    }

    /**
     * Returns a map of the command handler methods from the passed instance.
     *
     * @param object the object that keeps command handler methods
     * @return immutable map
     */
    public static Map<CommandClass, CommandHandlerMethod> scan(Object object) {
        MethodMap handlers = new MethodMap(object.getClass(), isCommandHandlerPredicate);

        final ImmutableMap.Builder<CommandClass, CommandHandlerMethod> builder = ImmutableMap.builder();
        for (Map.Entry<Class<? extends Message>, Method> entry : handlers.entrySet()) {
            Class<? extends Message> commandClass = entry.getKey();
            Method method = entry.getValue();
            final CommandHandlerMethod handler = new CommandHandlerMethod(object, method);
            handler.checkModifier();
            builder.put(CommandClass.of(commandClass), handler);
        }
        return builder.build();
    }

    public static AccessLevelException forRepositoryCommandHandler(Repository repository, Method method) {
        return new AccessLevelException(messageForRepositoryCommandHandler(repository, method));
    }

    public static String messageForAggregateCommandHandler(Object aggregate, Method method) {
        return "Command handler of the aggregate " + Methods.getFullMethodName(aggregate, method) +
                " must be declared 'public'. It is part of the public API of the aggregate.";
    }

    public static final String MUST_BE_PUBLIC_FOR_COMMAND_DISPATCHER = " must be declared 'public' to be called by CommandDispatcher.";

    private static String messageForRepositoryCommandHandler(Object repository, Method method) {
        return "Command handler of the repository " + Methods.getFullMethodName(repository, method) +
                MUST_BE_PUBLIC_FOR_COMMAND_DISPATCHER;
    }

    public static AccessLevelException forAggregateCommandHandler(AggregateRoot aggregate, Method method) {
        return new AccessLevelException(messageForAggregateCommandHandler(aggregate, method));
    }

    public static AccessLevelException forCommandHandler(Object handler, Method method) {
        return new AccessLevelException(messageForCommandHandler(handler, method));
    }

    private static String messageForCommandHandler(Object handler, Method method) {
        return "Command handler " + Methods.getFullMethodName(handler, method) +
                MUST_BE_PUBLIC_FOR_COMMAND_DISPATCHER;
    }

    @Override
    protected void checkModifier() {
        final Method method = getMethod();
        final Object target = getTarget();

        boolean methodIsPublic = isPublic();

        boolean isAggregateRoot = target instanceof AggregateRoot;
        if (isAggregateRoot && !methodIsPublic) {
            throw forAggregateCommandHandler((AggregateRoot) target, method);
        }

        boolean isRepository = target instanceof Repository;
        if (isRepository && !methodIsPublic) {
            throw forRepositoryCommandHandler((Repository) target, method);
        }

        if (!methodIsPublic) {
            throw forCommandHandler(target, method);
        }
    }

    @Override
    public <R> R invoke(Message message, CommandContext context) throws InvocationTargetException {
        return super.invoke(message, context);
    }
}
