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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.server.command.Assign;
import io.spine.server.event.EventFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The wrapper for a command handler method.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandHandlerMethod extends HandlerMethod<CommandContext> {

    /** The instance of the predicate to filter command handler methods of a class. */
    private static final MethodPredicate PREDICATE = new FilterPredicate();

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    private CommandHandlerMethod(Method method) {
        super(method);
    }

    /**
     * Returns command classes handled by the passed class.
     *
     * @param cls the class of objects that handle commands
     * @return immutable set of command classes
     */
    @CheckReturnValue
    public static Set<CommandClass> inspect(Class<?> cls) {
        checkNotNull(cls);
        final Set<CommandClass> result = CommandClass.setOf(inspect(cls, predicate()));
        return result;
    }

    /**
     * Obtains handler method for the command message.
     *
     * @param cls            the class that handles the message
     * @param commandMessage the message
     * @return handler method
     * @throws IllegalStateException if the passed class does not handle messages of this class
     */
    private static CommandHandlerMethod getMethod(Class<?> cls, Message commandMessage) {
        final Class<? extends Message> commandClass = commandMessage.getClass();
        final CommandHandlerMethod method = MethodRegistry.getInstance()
                                                          .get(cls, commandClass, factory());
        if (method == null) {
            throw newIllegalStateException("The class %s does not handle commands of the class %s.",
                                           cls.getName(), commandClass.getName());
        }
        return method;
    }

    static CommandHandlerMethod from(Method method) {
        return new CommandHandlerMethod(method);
    }

    static MethodPredicate predicate() {
        return PREDICATE;
    }

    /**
     * Invokes the handler method in the passed object.
     *
     * @return the list of events produced by the handler method
     */
    public static List<? extends Message> invokeFor(Object target,
                                                    Message command,
                                                    CommandContext context) {
        checkNotNull(target);
        checkNotNull(command);
        checkNotNull(context);
        final Message commandMessage = ensureMessage(command);

        final CommandHandlerMethod method = getMethod(target.getClass(), commandMessage);
        final List<? extends Message> eventMessages =
                method.invoke(target, commandMessage, context);
        return eventMessages;
    }

    public static List<Event> toEvents(final Any producerId,
                                       @Nullable final Version version,
                                       final List<? extends Message> eventMessages,
                                       final CommandEnvelope origin) {
        checkNotNull(producerId);
        checkNotNull(eventMessages);
        checkNotNull(origin);

        final EventFactory eventFactory =
                EventFactory.on(origin, producerId, eventMessages.size());

        return Lists.transform(eventMessages, new Function<Message, Event>() {
            @Override
            public Event apply(@Nullable Message eventMessage) {
                if (eventMessage == null) {
                    return Event.getDefaultInstance();
                }
                final Event result = eventFactory.createEvent(eventMessage, version);
                return result;
            }
        });
    }

    private static HandlerMethod.Factory<CommandHandlerMethod> factory() {
        return Factory.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return the list of event messages (or an empty list if the handler returns nothing)
     */
    @Override
    public <R> R invoke(Object target, Message message, CommandContext context) {
        final R handlingResult = super.invoke(target, message, context);

        final List<? extends Message> events = toList(handlingResult);
        // The list of event messages is the return type expected.
        @SuppressWarnings("unchecked") final R result = (R) events;
        return result;
    }

    /**
     * The factory for filtering {@linkplain CommandHandlerMethod command handling methods}.
     */
    private static class Factory implements HandlerMethod.Factory<CommandHandlerMethod> {

        private static final Factory INSTANCE = new Factory();

        private static Factory getInstance() {
            return INSTANCE;
        }

        @Override
        public Class<CommandHandlerMethod> getMethodClass() {
            return CommandHandlerMethod.class;
        }

        @Override
        public CommandHandlerMethod create(Method method) {
            return from(method);
        }

        @Override
        public Predicate<Method> getPredicate() {
            return predicate();
        }

        @Override
        public void checkAccessModifier(Method method) {
            if (!isPackagePrivate(method)) {
                warnOnWrongModifier(
                        "Command handler method {} should be package-private.", method);
            }
        }
    }

    /**
     * The predicate that filters command handling methods.
     *
     * <p>See {@link Assign} annotation for more info about such methods.
     */
    private static class FilterPredicate extends HandlerMethodPredicate<CommandContext> {

        private FilterPredicate() {
            super(Assign.class, CommandContext.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            return returnsMessageOrList(method);
        }
    }
}
