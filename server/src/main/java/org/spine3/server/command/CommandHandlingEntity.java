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

package org.spine3.server.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.FailureThrowable;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.entity.Entity;
import org.spine3.server.reflect.CommandHandlerMethod;
import org.spine3.server.reflect.MethodRegistry;
import org.spine3.server.type.CommandClass;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Events.generateId;
import static org.spine3.base.Identifiers.idToAny;
import static org.spine3.protobuf.Timestamps.getCurrentTime;
import static org.spine3.server.reflect.Classes.getHandledMessageClasses;
import static org.spine3.util.Exceptions.wrappedCause;

/**
 * An entity that can handle commands.
 *
 * <h2>Command handling methods</h2>
 *
 * <p>A command handling method is a {@code public} method that accepts two parameters.
 * The first parameter is a command message. The second parameter is {@link CommandContext}.
 *
 * <p>The method returns an event message of the specific type, or {@code List} of messages
 * if it produces more than one event.
 *
 * <p>The method may throw one or more throwables derived from {@link FailureThrowable}.
 * Throwing a {@code FailureThrowable} indicates that the passed command cannot be handled
 * because of a business failure, which can be obtained via {@link FailureThrowable#getFailure()}.
 *
 * @author Alexander Yevsyukov
 */
public abstract class CommandHandlingEntity<I, S extends Message> extends Entity<I, S> {

    /** Cached value of the ID in the form of {@code Any} instance. */
    private final Any idAsAny;

    /**
     * {@inheritDoc}
     */
    protected CommandHandlingEntity(I id) {
        super(id);
        this.idAsAny = idToAny(id);
    }

    protected Any getIdAsAny() {
        return idAsAny;
    }

    /**
     * Creates a context for an event message.
     *
     * <p>The context may optionally have custom attributes added by
     * {@link #extendEventContext(EventContext.Builder, Message, CommandContext)}.
     *
     * @param event          the event for which to create the context
     * @param commandContext the context of the command, execution of which produced the event
     * @return new instance of the {@code EventContext}
     * @see #extendEventContext(EventContext.Builder, Message, CommandContext)
     */
    @CheckReturnValue
    protected EventContext createEventContext(Message event, CommandContext commandContext) {
        final EventId eventId = generateId();
        final Timestamp whenModified = getCurrentTime();
        final EventContext.Builder builder = EventContext.newBuilder()
                                                         .setEventId(eventId)
                                                         .setTimestamp(whenModified)
                                                         .setCommandContext(commandContext)
                                                         .setProducerId(getIdAsAny())
                                                         .setVersion(getVersion());
        extendEventContext(builder, event, commandContext);
        return builder.build();
    }

    /**
     * Adds custom attributes to {@code EventContext.Builder} during
     * the creation of the event context.
     *
     * <p>Does nothing by default. Override this method if you want to
     * add custom attributes to the created context.
     *
     * @param builder        a builder for the event context
     * @param event          the event message
     * @param commandContext the context of the command that produced the event
     * @see #createEventContext(Message, CommandContext)
     */
    @SuppressWarnings({"NoopMethodInAbstractClass", "UnusedParameters"})
    // Have no-op method to avoid forced overriding.
    protected void extendEventContext(EventContext.Builder builder,
                                      Message event,
                                      CommandContext commandContext) {
        // Do nothing.
    }

    /**
     * Returns the set of the command classes handled by the passed class.
     *
     * @param clazz the class of objects that handle commands
     * @return immutable set of command classes
     */
    public static Set<CommandClass> getCommandClasses(Class<? extends CommandHandlingEntity> clazz) {
        final Set<CommandClass> result = CommandClass.setOf(
                getHandledMessageClasses(clazz, CommandHandlerMethod.PREDICATE));
        return result;
    }

    /**
     * Dispatches the passed command to appropriate handler.
     *
     * @param command the command message to be handled.
     *                If this parameter is passed as {@link Any} the enclosing
     *                message will be unwrapped.
     * @param context the context of the command
     * @return event messages generated by the handler
     * @throws RuntimeException if an exception occurred during command dispatching
     *                          with this exception as the cause
     * @see #dispatchForTest(Message, CommandContext)
     */
    protected List<? extends Message> dispatchCommand(Message command, CommandContext context) {
        checkNotNull(command);
        checkNotNull(context);

        final Message commandMessage = ensureCommandMessage(command);

        try {
            final List<? extends Message> eventMessages = invokeHandler(commandMessage, context);
            return eventMessages;
        } catch (InvocationTargetException e) {
            throw wrappedCause(e);
        }
    }

    /**
     * This method is provided <em>only</em> for the purpose of testing command
     * handling and must not be called from the production code.
     *
     * <p>The production code uses the method {@link #dispatchCommand(Message, CommandContext)},
     * which is called automatically.
     */
    @VisibleForTesting
    public final List<? extends Message> dispatchForTest(Message command, CommandContext context) {
        return dispatchCommand(command, context);
    }

    /**
     * Ensures that the passed instance of {@code Message} is not an {@code Any},
     * and unwraps the command message if {@code Any} is passed.
     */
    private static Message ensureCommandMessage(Message command) {
        Message commandMessage;
        if (command instanceof Any) {
            /* It looks that we're getting the result of `command.getMessage()`
               because the calling code did not bother to unwrap it.
               Extract the wrapped message (instead of treating this as an error).
               There may be many occasions of such a call especially from the
               testing code. */
            final Any any = (Any) command;
            commandMessage = AnyPacker.unpack(any);
        } else {
            commandMessage = command;
        }
        return commandMessage;
    }

    /**
     * Directs the passed command to the corresponding command handler method.
     *
     * @param commandMessage the command to be processed
     * @param context the context of the command
     * @return a list of the event messages that were produced as the result of handling the command
     * @throws InvocationTargetException if an exception occurs during command handling
     */
    protected List<? extends Message> invokeHandler(Message commandMessage, CommandContext context)
            throws InvocationTargetException {
        final Class<? extends Message> commandClass = commandMessage.getClass();
        final CommandHandlerMethod method = MethodRegistry.getInstance()
                                                          .get(getClass(),
                                                               commandClass,
                                                               CommandHandlerMethod.factory());
        if (method == null) {
            throw missingCommandHandler(commandClass);
        }
        final List<? extends Message> result = method.invoke(this, commandMessage, context);
        return result;
    }

    private IllegalStateException missingCommandHandler(Class<? extends Message> commandClass) {
        return new IllegalStateException(
                String.format("Missing handler for command class %s in the class %s.",
                        commandClass.getName(), getClass().getName()));
    }
}
