/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.base.Identifiers;
import org.spine3.base.UserId;
import org.spine3.server.command.CommandBus;
import org.spine3.server.entity.Entity;
import org.spine3.server.reflect.Classes;
import org.spine3.server.reflect.CommandHandlerMethod;
import org.spine3.server.reflect.EventHandlerMethod;
import org.spine3.server.reflect.MethodRegistry;
import org.spine3.time.ZoneOffset;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.spine3.server.reflect.EventHandlerMethod.PREDICATE;

/**
 * An independent component that reacts to domain events in a cross-aggregate, eventually consistent manner.
 *
 * <p>A central processing unit used to maintain the state of the business process and determine
 * the next processing step based on intermediate results.
 *
 * <p>Event/command handlers are invoked by the {@link ProcessManagerRepository}
 * that manages instances of a process manager class.
 *
 * <p>For more information on Process Managers, please see:
 * <ul>
 *     <li><a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/ProcessManager.html">Process Manager Pattern</a></li>
 *     <li>
 *         <a href="http://kellabyte.com/2012/05/30/clarifying-the-saga-pattern/">Clarifying the Saga pattern</a>
 *         (and the difference between Process Manager and Saga)
 *     </li>
 *     <li><a href="https://dzone.com/articles/are-sagas-and-workflows-same-t">Are Sagas and Workflows the same...</a></li>
 *     <li><a href="https://msdn.microsoft.com/en-us/library/jj591569.aspx">CQRS Journey Guide: A Saga on Sagas</a></li>
 * </ul>
 *
 * @param <I> the type of the process manager IDs
 * @param <M> the type of the process manager state
 * @author Alexander Litus
 */
public abstract class ProcessManager<I, M extends Message> extends Entity<I, M> {

    /**
     * The Command Bus to post routed commands.
     */
    private volatile CommandBus commandBus;

    /**
     * Creates a new instance.
     *
     * @param id an ID for the new instance
     * @throws IllegalArgumentException if the ID type is unsupported
     */
    @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
    public ProcessManager(I id) {
        super(id);
    }

    /**
     * The method to inject {@code CommandBus} instance from the repository.
     */
    /* package */ void setCommandBus(CommandBus commandBus) {
        this.commandBus = checkNotNull(commandBus);
    }

    /**
     * Returns the {@code CommandBus} to which post commands produced by this process manager.
     */
    protected CommandBus getCommandBus() {
        return commandBus;
    }

    /**
     * Dispatches a command to the command handler method of the process manager.
     *
     * @param command the command to be executed on the process manager
     * @param context of the command
     * @throws InvocationTargetException if an exception occurs during command dispatching
     * @throws IllegalStateException if no command handler method found for a command
     */
    protected List<Event> dispatchCommand(Message command, CommandContext context)
            throws InvocationTargetException, IllegalStateException {
        checkNotNull(command);
        checkNotNull(context);

        final Class<? extends Message> commandClass = command.getClass();
        final CommandHandlerMethod method = MethodRegistry.getInstance()
                                                          .get(getClass(),
                                                               commandClass,
                                                               CommandHandlerMethod.factory());
        if (method == null) {
            throw missingCommandHandler(commandClass);
        }

        final List<? extends Message> events = method.invoke(this, command, context);
        final List<Event> eventRecords = toEvents(events, context.getCommandId());
        return eventRecords;
    }

    private List<Event> toEvents(final List<? extends Message> events, final CommandId commandId) {
        return Lists.transform(events, new Function<Message, Event>() {
            @Nullable // return null because an exception won't be propagated in this case
            @Override
            public Event apply(@Nullable Message event) {
                if (event == null) {
                    return Event.getDefaultInstance();
                }
                final EventContext eventContext = createEventContext(commandId, event, getState(), whenModified(), getVersion());
                final Event result = Events.createEvent(event, eventContext);
                return result;
            }
        });
    }

    /**
     * Dispatches an event to the event handler method of the process manager.
     *
     * @param event the event to be handled by the process manager
     * @param context of the event
     * @throws InvocationTargetException if an exception occurs during event dispatching
     */
    protected void dispatchEvent(Message event, EventContext context) throws InvocationTargetException {
        checkNotNull(context);
        checkNotNull(event);

        final Class<? extends Message> eventClass = event.getClass();
        final EventHandlerMethod method = MethodRegistry.getInstance()
                                                        .get(getClass(), eventClass, EventHandlerMethod.factory());
        if (method == null) {
            throw missingEventHandler(eventClass);
        }

        method.invoke(this, event, context);
    }

    /**
     * Creates a new {@link CommandRouter} instance.
     */
    protected CommandRouter newRouter() {
        final CommandBus commandBus = getCommandBus();
        checkState(commandBus != null, "CommandBus must be initialized");
        return new CommandRouter(commandBus);
    }

    /**
     * A {@code CommandRouter} allows to create and post one or more commands
     * in response to a command received by the {@code ProcessManager}.
     *
     * <p>A typical usage looks like this:
     *
     * <pre>
     *     {@literal @}Assign
     *     public CommandRouted on(MyCommand message, CommandContext context) {
     *         // Create new command messages here.
     *         return new Router().of(message, context)
     *                  .add(messageOne)
     *                  .add(messageTwo)
     *                  .route();
     *     }
     * </pre>
     *
     * <p>The routed commands are created on behalf of the actor of the original command.
     * That is, the {@code actor} and {@code zoneOffset} fields of created {@code CommandContext}
     * instances will be the same as in the incoming command.
     */
    protected static class CommandRouter {

        private final CommandBus commandBus;
        private Message source;
        private CommandContext sourceContext;

        /**
         * The actor of the command we route.
         *
         * <p>We route commands on the original's author behalf.
         */
        private UserId actor;

        /**
         * The zone offset from which the actor works.
         */
        private ZoneOffset zoneOffset;

        /**
         * Command messages to route.
         */
        private final List<Message> toRoute = Lists.newArrayList();

        private CommandRouter(CommandBus commandBus) {
            this.commandBus = commandBus;
        }

        /**
         * Sets command to be routed.
         */
        public CommandRouter of(Message source, CommandContext context) {
            this.source = checkNotNull(source);
            this.sourceContext = checkNotNull(context);

            this.actor = context.getActor();
            this.zoneOffset = context.getZoneOffset();

            return this;
        }

        /**
         * Adds {@code commandMessage} to be routed as a command.
         */
        public CommandRouter add(Message commandMessage) {
            toRoute.add(commandMessage);
            return this;
        }

        /**
         * Posts the added messages as commands to {@code CommandBus}.
         *
         * @return the event with source and produced commands
         */
        public CommandRouted route() {
            final CommandRouted.Builder result = CommandRouted.newBuilder();
            result.setSource(Commands.create(this.source, this.sourceContext));

            for (Message message : toRoute) {
                final Command command = produceCommand(message);
                commandBus.post(command);
                result.addProduced(command);
            }
            return result.build();
        }

        private Command produceCommand(Message newMessage) {
            final CommandContext newContext = Commands.createContext(actor, zoneOffset);
            final Command result = Commands.create(newMessage, newContext);
            return result;
        }
    }

    /**
     * Creates a context for an event.
     *
     * <p>The context may optionally have custom attributes added by
     * {@link #addEventContextAttributes(EventContext.Builder, CommandId, Message, Message, int)}.
     *
     * @param commandId      the ID of the command, which caused the event
     * @param event          the event for which to create the context
     * @param currentState   the state of the process manager after the event was applied
     * @param whenModified   the moment of the process manager modification for this event
     * @param currentVersion the version of the process manager after the event was applied
     * @return new instance of the {@code EventContext}
     */
    @CheckReturnValue
    private EventContext createEventContext(CommandId commandId, Message event, M currentState,
                                              Timestamp whenModified, int currentVersion) {
        final EventId eventId = Events.generateId();
        final EventContext.Builder builder = EventContext.newBuilder()
                .setEventId(eventId)
                .setTimestamp(whenModified)
                .setVersion(currentVersion)
                .setProducerId(Identifiers.idToAny(getId()));

        addEventContextAttributes(builder, commandId, event, currentState, currentVersion);

        return builder.build();
    }

    /**
     * Adds custom attributes to an event context builder during the creation of the event context.
     *
     * <p>Does nothing by default. Override this method if you want to add custom attributes to the created context.
     *
     * @param builder        a builder for the event context
     * @param commandId      the id of the command, which cased the event
     * @param event          the event message
     * @param currentState   the current state of the aggregate after the event was applied
     * @param currentVersion the version of the process manager after the event was applied
     */
    @SuppressWarnings({"NoopMethodInAbstractClass", "UnusedParameters"}) // Have no-op method to avoid forced overriding.
    protected void addEventContextAttributes(EventContext.Builder builder,
                                             CommandId commandId, Message event, M currentState, int currentVersion) {
        // Do nothing.
    }

    /**
     * Returns the set of the command types handled by the process manager.
     *
     * @param pmClass the process manager class to inspect
     * @return immutable set of command classes or an empty set if no commands are handled
     */
    public static Set<Class<? extends Message>> getHandledCommandClasses(Class<? extends ProcessManager> pmClass) {
        return Classes.getHandledMessageClasses(pmClass, CommandHandlerMethod.PREDICATE);
    }

    /**
     * Returns the set of event classes handled by the process manager.
     *
     * @param pmClass the process manager class to inspect
     * @return immutable set of event classes or an empty set if no events are handled
     */
    public static ImmutableSet<Class<? extends Message>> getHandledEventClasses(Class<? extends ProcessManager> pmClass) {
        return Classes.getHandledMessageClasses(pmClass, PREDICATE);
    }

    private IllegalStateException missingCommandHandler(Class<? extends Message> commandClass) {
        return new IllegalStateException(String.format("Missing handler for command class %s in process manager class %s.",
                        commandClass.getName(), getClass().getName()));
    }

    private IllegalStateException missingEventHandler(Class<? extends Message> eventClass) {
        return new IllegalStateException(String.format("Missing event handler for event class %s in the process manager class %s",
                eventClass, this.getClass()));
    }

}
