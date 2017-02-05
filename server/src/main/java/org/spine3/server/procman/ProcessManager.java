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

package org.spine3.server.procman;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.base.Response;
import org.spine3.server.command.CommandBus;
import org.spine3.server.entity.Entity;
import org.spine3.server.reflect.Classes;
import org.spine3.server.reflect.CommandHandlerMethod;
import org.spine3.server.reflect.EventSubscriberMethod;
import org.spine3.server.reflect.MethodRegistry;
import org.spine3.server.users.CurrentTenant;
import org.spine3.time.ZoneOffset;
import org.spine3.users.TenantId;
import org.spine3.users.UserId;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.spine3.base.Identifiers.idToAny;

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
 *     <li><a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/ProcessManager.html">Process Manager Pattern</a>
 *     <li>
 *         <a href="http://kellabyte.com/2012/05/30/clarifying-the-saga-pattern/">Clarifying the Saga pattern</a>
 *         (and the difference between Process Manager and Saga)
 *
 *     <li><a href="https://dzone.com/articles/are-sagas-and-workflows-same-t">Are Sagas and Workflows the same...</a>
 *     <li><a href="https://msdn.microsoft.com/en-us/library/jj591569.aspx">CQRS Journey Guide: A Saga on Sagas</a>
 * </ul>
 *
 * @param <I> the type of the process manager IDs
 * @param <S> the type of the process manager state
 * @author Alexander Litus
 */
public abstract class ProcessManager<I, S extends Message> extends Entity<I, S> {

    /** The Command Bus to post routed commands. */
    private volatile CommandBus commandBus;

    /**
     * Creates a new instance.
     *
     * @param id an ID for the new instance
     * @throws IllegalArgumentException if the ID type is unsupported
     */
    protected ProcessManager(I id) {
        super(id);
    }

    /** The method to inject {@code CommandBus} instance from the repository. */
    void setCommandBus(CommandBus commandBus) {
        this.commandBus = checkNotNull(commandBus);
    }

    /** Returns the {@code CommandBus} to which post commands produced by this process manager. */
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
        final List<? extends Message> messages = method.invoke(this, command, context);
        final List<Event> events = toEvents(messages, context);
        return events;
    }

    private List<Event> toEvents(final List<? extends Message> events, final CommandContext cmdContext) {
        return Lists.transform(events, new Function<Message, Event>() {
            @Nullable // return null because an exception won't be propagated in this case
            @Override
            public Event apply(@Nullable Message event) {
                if (event == null) {
                    return Event.getDefaultInstance();
                }
                final EventContext eventContext = createEventContext(cmdContext, event, getState(), whenModified(), getVersion());
                final Event result = Events.createEvent(event, eventContext);
                return result;
            }
        });
    }

    /**
     * Creates a context for an event.
     *
     * <p>The context may optionally have custom attributes added by
     * {@link #addEventContextAttributes(EventContext.Builder, CommandId, Message, Message, int)}.
     *
     * @param cmdContext     the context of the command, which processing caused the event
     * @param event          the event for which to create the context
     * @param currentState   the state of the process manager after the event was applied
     * @param whenModified   the moment of the process manager modification for this event
     * @param currentVersion the version of the process manager after the event was applied
     * @return new instance of the {@code EventContext}
     */
    @CheckReturnValue
    private EventContext createEventContext(CommandContext cmdContext,
                                            Message event,
                                            S currentState,
                                            Timestamp whenModified,
                                            int currentVersion) {
        final EventId eventId = Events.generateId();
        final Any producerId = idToAny(getId());
        final CommandId commandId = cmdContext.getCommandId();
        final EventContext.Builder builder = EventContext.newBuilder()
                                                         .setEventId(eventId)
                                                         .setTimestamp(whenModified)
                                                         .setCommandContext(cmdContext)
                                                         .setProducerId(producerId)
                                                         .setVersion(currentVersion);
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
                                             CommandId commandId,
                                             Message event,
                                             S currentState,
                                             int currentVersion) {
        // Do nothing.
    }

    /**
     * Dispatches an event to the event subscriber method of the process manager.
     *
     * @param event the event to be handled by the process manager
     * @param context of the event
     * @throws InvocationTargetException if an exception occurs during event dispatching
     */
    protected void dispatchEvent(Message event, EventContext context) throws InvocationTargetException {
        checkNotNull(context);
        checkNotNull(event);
        final Class<? extends Message> eventClass = event.getClass();
        final EventSubscriberMethod method = MethodRegistry.getInstance()
                                                        .get(getClass(), eventClass, EventSubscriberMethod.factory());
        if (method == null) {
            throw missingEventHandler(eventClass);
        }
        method.invoke(this, event, context);
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
        return Classes.getHandledMessageClasses(pmClass, EventSubscriberMethod.PREDICATE);
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
     *     CommandRouted on(MyCommand message, CommandContext context) {
     *         // Create new command messages here.
     *         return newRouter().of(message, context)
     *                  .add(messageOne)
     *                  .add(messageTwo)
     *                  .routeAll();
     *     }
     * </pre>
     *
     * <p>The routed commands are created on behalf of the actor of the original command.
     * That is, the {@code actor} and {@code zoneOffset} fields of created {@code CommandContext}
     * instances will be the same as in the incoming command.
     *
     * <p>This class is made internal and protected to {@code ProcessManager} so that only derived classes
     * can have the code constructs similar to the quoted above.
     */
    protected static class CommandRouter {

        private final CommandBus commandBus;

        /** The command message of the source command. */
        private Message sourceCommand;

        /** The command context of the source command. */
        private CommandContext sourceContext;

        /** The cached value of the source command. */
        private Command source;

        /**
         * The actor of the command we route.
         *
         * <p>We route commands on the original's author behalf.
         */
        private UserId actor;

        /** The zone offset from which the actor works. */
        private ZoneOffset zoneOffset;

        /** Command messages to route. */
        private final List<Message> toRoute = Lists.newArrayList();

        /**
         * The future for waiting until the {@link CommandBus#post posting of the command} completes.
         */
        private final SettableFuture<Void> finishFuture = SettableFuture.create();

        /**
         * The observer for posting commands.
         */
        private final StreamObserver<Response> responseObserver = newResponseObserver(finishFuture);

        private CommandRouter(CommandBus commandBus) {
            this.commandBus = commandBus;
        }

        /** Sets the command to be routed. */
        public CommandRouter of(Message sourceCommand, CommandContext context) {
            this.sourceCommand = checkNotNull(sourceCommand);
            this.sourceContext = checkNotNull(context);
            this.source = Commands.create(sourceCommand, sourceContext);
            this.actor = context.getActor();
            this.zoneOffset = context.getZoneOffset();
            return this;
        }

        /** Adds {@code commandMessage} to be routed. */
        public CommandRouter add(Message commandMessage) {
            toRoute.add(commandMessage);
            return this;
        }

        /**
         * Posts the added messages as commands to {@code CommandBus}.
         *
         * @return the event with source and produced commands
         */
        public CommandRouted routeAll() {
            final CommandRouted.Builder result = CommandRouted.newBuilder();
            result.setSource(source);

            for (Message message : toRoute) {
                final Command command = produceCommand(message);
                commandBus.post(command, responseObserver);
                // Wait till the call is completed.
                try {
                    finishFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new IllegalArgumentException(e);
                }
                result.addProduced(command);
            }
            return result.build();
        }

        private static StreamObserver<Response> newResponseObserver(final SettableFuture<Void> finishFuture) {
            return new StreamObserver<Response>() {
                @Override
                public void onNext(Response response) {
                    // Do nothing. It's just a confirmation of successful post to Command Bus.
                }

                @Override
                public void onError(Throwable throwable) {
                    finishFuture.setException(throwable);
                }

                @Override
                public void onCompleted() {
                    finishFuture.set(null);
                }
            };
        }

        private Command produceCommand(Message newMessage) {
            final TenantId currentTenant = CurrentTenant.get();
            final CommandContext newContext = Commands.createContext(currentTenant, actor, zoneOffset);
            final Command result = Commands.create(newMessage, newContext);
            return result;
        }
    }

    private IllegalStateException missingCommandHandler(Class<? extends Message> commandClass) {
        final String msg = format("Missing handler for command class %s in process manager class %s.",
                                     commandClass.getName(), getClass().getName());
        return new IllegalStateException(msg);
    }

    private IllegalStateException missingEventHandler(Class<? extends Message> eventClass) {
        final String msg = format("Missing event handler for event class %s in the process manager class %s",
                                     eventClass, this.getClass());
        return new IllegalStateException(msg);
    }
}
