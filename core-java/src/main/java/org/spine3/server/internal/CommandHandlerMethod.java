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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.CommandClass;
import org.spine3.base.CommandContext;
import org.spine3.internal.MessageHandlerMethod;
import org.spine3.server.Assign;
import org.spine3.server.MultiHandler;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.util.MethodMap;
import org.spine3.util.Methods;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.spine3.server.internal.AggregateCommandHandler.IS_AGGREGATE_COMMAND_HANDLER;
import static org.spine3.server.internal.ProcessManagerCommandHandler.IS_PM_COMMAND_HANDLER;

/**
 * The wrapper for a command handler method.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class CommandHandlerMethod extends MessageHandlerMethod<Object, CommandContext> {

    private static final int MESSAGE_PARAM_INDEX = 0;
    private static final int COMMAND_CONTEXT_PARAM_INDEX = 1;

    private static final int COMMAND_HANDLER_PARAMETER_COUNT = 2;

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param target object to which the method applies
     * @param method subscriber method
     */
    protected CommandHandlerMethod(Object target, Method method) {
        super(target, method);
    }

    protected static boolean isAnnotatedCorrectly(AnnotatedElement element) {
        final boolean isAnnotated = element.isAnnotationPresent(Assign.class);
        return isAnnotated;
    }

    protected static boolean acceptsCorrectParameters(Method method) {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final boolean paramCountIsCorrect = parameterTypes.length == COMMAND_HANDLER_PARAMETER_COUNT;
        if (!paramCountIsCorrect) {
            return false;
        }
        final boolean acceptsCorrectParams =
                Message.class.isAssignableFrom(parameterTypes[MESSAGE_PARAM_INDEX]) &&
                CommandContext.class.equals(parameterTypes[COMMAND_CONTEXT_PARAM_INDEX]);
        return acceptsCorrectParams;
    }

    /**
     * {@inheritDoc}
     *
     * @return the list of event messages/records (or an empty list if the handler returns nothing)
     */
    @Override
    public <R> R invoke(Message message, CommandContext context) throws InvocationTargetException {
        final R handlingResult = super.invoke(message, context);

        final List<? extends Message> events = commandHandlingResultToEvents(handlingResult);
        // The list of event messages/records is the return type expected.
        @SuppressWarnings("unchecked")
        final R result = (R) events;
        return result;
    }

    /**
     * Casts a command handling result to a list of event messages.
     *
     * @param handlingResult the command handler method return value. Could be a {@link Message} or a list of messages.
     * @return the list of events as messages
     * @see AggregateCommandHandler#isAggregateCommandHandler(Method)
     * @see ProcessManagerCommandHandler#isProcessManagerCommandHandler(Method)
     */
    protected <R> List<? extends Message> commandHandlingResultToEvents(R handlingResult) {
        final Class<?> resultClass = handlingResult.getClass();
        if (List.class.isAssignableFrom(resultClass)) {
            // Cast to the list of messages as it is the one of the return types we expect by methods we call.
            @SuppressWarnings("unchecked")
            final List<? extends Message> result = (List<? extends Message>) handlingResult;
            return result;
        } else {
            // Another type of result is single event (as Message).
            final List<Message> result = singletonList((Message) handlingResult);
            return result;
        }
    }

    /**
     * Returns a map of the command handler methods from the passed instance.
     *
     * @param object the object that keeps command handler methods
     * @return immutable map
     */
    @CheckReturnValue
    public static Map<CommandClass, CommandHandlerMethod> scan(Object object) {
        final ImmutableMap.Builder<CommandClass, CommandHandlerMethod> result = ImmutableMap.builder();

        final Map<CommandClass, CommandHandlerMethod> regularHandlers = getRegularHandlers(object);
        result.putAll(regularHandlers);

        if (object instanceof MultiHandler) {
            final MultiHandler multiHandler = (MultiHandler) object;
            final Map<CommandClass, CommandHandlerMethod> multiHandlers = getMultiHandlers(multiHandler);
            checkModifiers(toMethods(multiHandlers.values()));
            result.putAll(multiHandlers);
        }
        return result.build();
    }

    private static Map<CommandClass, CommandHandlerMethod> getRegularHandlers(Object object) {
        final ImmutableMap.Builder<CommandClass, CommandHandlerMethod> result = ImmutableMap.builder();

        final Predicate<Method> isHandlerPredicate = getHandlerMethodPredicate(object);
        final MethodMap handlers = new MethodMap(object.getClass(), isHandlerPredicate);
        checkModifiers(handlers.values());
        for (Map.Entry<Class<? extends Message>, Method> entry : handlers.entrySet()) {
            final CommandClass commandClass = CommandClass.of(entry.getKey());
            final CommandHandlerMethod handler = getCommandHandler(object, entry.getValue());
            result.put(commandClass, handler);
        }
        return result.build();
    }

    // TODO:2015-12-03:alexander.litus: avoid instanceof checks
    private static CommandHandlerMethod getCommandHandler(Object object, Method value) {
        //noinspection IfMayBeConditional
        if (object instanceof Aggregate || object instanceof AggregateRepository) {
            return new AggregateCommandHandler(object, value);
        } else {
            return new ProcessManagerCommandHandler(object, value);
        }
    }

    private static Predicate<Method> getHandlerMethodPredicate(Object object) {
        //noinspection IfMayBeConditional
        if (object instanceof Aggregate || object instanceof AggregateRepository) {
            return IS_AGGREGATE_COMMAND_HANDLER;
        } else {
            return IS_PM_COMMAND_HANDLER;
        }
    }

    /**
     * Creates a command handler map from the passed instance of {@link MultiHandler}.
     */
    @CheckReturnValue
    private static Map<CommandClass, CommandHandlerMethod> getMultiHandlers(MultiHandler obj) {
        final Multimap<Method, Class<? extends Message>> methodsToClasses = obj.getCommandHandlers();

        final ImmutableMap.Builder<CommandClass, CommandHandlerMethod> builder = ImmutableMap.builder();

        for (Method method : methodsToClasses.keySet()) {
            final Collection<Class<? extends Message>> classes = methodsToClasses.get(method);
            builder.putAll(createMap(obj, method, classes));
        }

        return builder.build();
    }

    /**
     * Creates a command handler map for a single method of an object that handles multiple
     * message classes.
     *
     * @param target  the object on which execute the method
     * @param method  the method to call
     * @param classes the classes of messages handled by the method
     * @return immutable map of command handlers
     */
    private static Map<CommandClass, CommandHandlerMethod> createMap(Object target,
                                                                     Method method,
                                                                     Iterable<Class<? extends Message>> classes) {
        final ImmutableMap.Builder<CommandClass, CommandHandlerMethod> builder = ImmutableMap.builder();
        for (Class<? extends Message> messageClass : classes) {
            final CommandClass key = CommandClass.of(messageClass);
            final CommandHandlerMethod value = getCommandHandler(target, method);
            builder.put(key, value);
        }
        return builder.build();
    }

    /**
     * Verifiers modifiers in the methods in the passed map to be 'public'.
     *
     * <p>Logs warning for the methods with a non-public modifier.
     *
     * @param methods the map of methods to check
     */
    public static void checkModifiers(Iterable<Method> methods) {
        for (Method method : methods) {
            final boolean isPublic = Modifier.isPublic(method.getModifiers());
            if (!isPublic) {
                log().warn(String.format("Command handler %s must be declared 'public'.",
                        Methods.getFullMethodName(method)));
            }
        }
    }

    private static Iterable<Method> toMethods(Iterable<CommandHandlerMethod> handlerMethods) {
        return Iterables.transform(handlerMethods, new Function<CommandHandlerMethod, Method>() {
            @Nullable // return null because an exception won't be propagated in this case
            @Override
            public Method apply(@Nullable CommandHandlerMethod eventHandlerMethod) {
                if (eventHandlerMethod == null) {
                    return null;
                }
                return eventHandlerMethod.getMethod();
            }
        });
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(CommandHandlerMethod.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
