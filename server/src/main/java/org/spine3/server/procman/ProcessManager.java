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
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandHandlingEntity;
import org.spine3.server.reflect.Classes;
import org.spine3.server.reflect.EventSubscriberMethod;
import org.spine3.server.reflect.MethodRegistry;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * A central processing unit used to maintain the state of the business process and determine
 * the next processing step based on intermediate results.
 *
 * <p>A process manager reacts to domain events in a cross-aggregate, eventually consistent manner.
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
public abstract class ProcessManager<I, S extends Message> extends CommandHandlingEntity<I, S> {

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
     * Directs the passed command to the handler method and transforms its output to a list of events.
     *
     * @param commandMessage the command to be processed
     * @param context the context of the command
     * @return the events resulted from the call
     * @throws InvocationTargetException if an exception occurs during command handling
     */
    @Override
    protected List<Event> invokeHandler(Message commandMessage, CommandContext context)
            throws InvocationTargetException {
        final List<? extends Message> messages = super.invokeHandler(commandMessage, context);

        final List<Event> events = toEvents(messages, context);
        return events;
    }

    private List<Event> toEvents(final List<? extends Message> events, final CommandContext commandContext) {
        return Lists.transform(events, new Function<Message, Event>() {
            @Override
            public Event apply(@Nullable Message event) {
                if (event == null) {
                    return Event.getDefaultInstance();
                }
                final EventContext eventContext = createEventContext(event, commandContext);
                final Event result = Events.createEvent(event, eventContext);
                return result;
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * This method overrides the parent for:
     * <ol>
     *     <li>Opening the method to the package.
     *     <li>Casting the result to {@code List<Event>}, which is produced by
     *     {@link #invokeHandler(Message, CommandContext) invokeHander()}.
     * </ol>
     *
     * @return the list of events generated as the result of handling the command.
     */
    @SuppressWarnings("unchecked") // See Javadoc above
    @Override
    protected List<Event> dispatchCommand(Message command, CommandContext context) {
        final List<? extends Message> messages = super.dispatchCommand(command, context);
        return (List<Event>)messages;
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
     * Returns the set of event classes handled by the process manager.
     *
     * @param pmClass the process manager class to inspect
     * @return immutable set of event classes or an empty set if no events are handled
     */
    public static ImmutableSet<Class<? extends Message>> getHandledEventClasses(Class<? extends ProcessManager> pmClass) {
        return Classes.getHandledMessageClasses(pmClass, EventSubscriberMethod.PREDICATE);
    }

    /**
     * Creates a new {@link CommandRouter}.
     *
     * <p>A {@code CommandRouter} allows to create and post one or more commands
     * in response to a command received by the {@code ProcessManager}.
     *
     * <p>A typical usage looks like this:
     *
     * <pre>
     *     {@literal @}Assign
     *     CommandRouted on(MyCommand message, CommandContext context) {
     *         // Create new command messages here.
     *         return newRouterFor(message, context)
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
     * @param commandMessage the source command message
     * @param commandContext the context of the source command
     * @return new {@code CommandRouter}
     */
    protected CommandRouter newRouterFor(Message commandMessage, CommandContext commandContext) {
        checkNotNull(commandMessage);
        checkNotNull(commandContext);
        final CommandBus commandBus = ensureCommandBus();
        final CommandRouter router = new CommandRouter(commandBus, commandMessage, commandContext);
        return router;
    }

    /**
     * Creates a new {@code IteratingCommandRouter}.
     *
     * <p>An {@code IteratingCommandRouter} allows to create several commands
     * in response to a command received by the {@code ProcessManager} and
     * post these commands one by one.
     * .
     * <p>A typical usage looks like this:
     * <pre>
     *     {@literal @}Assign
     *     CommandRouted on(MyCommand message, CommandContext context) {
     *         // Create new command messages here.
     *         router = newIteratingRouterFor(message, context);
     *         return router.add(messageOne)
     *                      .add(messageTwo)
     *                      .add(messageThree)
     *                      .routeFirst();
     *     }
     *
     *     {@literal @}Subscribe
     *     void on(EventOne message, EventContext context) {
     *         if (router.hasNext()) {
     *             router.routeNext();
     *         }
     *     }
     * </pre>
     *
     * @param commandMessage the source command message
     * @param commandContext the context of the source command
     * @return new {@code IteratingCommandRouter}
     * @see IteratingCommandRouter#routeFirst()
     * @see IteratingCommandRouter#routeNext()
     */
    protected IteratingCommandRouter newIteratingRouterFor(Message commandMessage, CommandContext commandContext) {
        checkNotNull(commandMessage);
        checkNotNull(commandContext);
        final CommandBus commandBus = ensureCommandBus();
        final IteratingCommandRouter router = new IteratingCommandRouter(commandBus, commandMessage, commandContext);
        return router;
    }

    private CommandBus ensureCommandBus() {
        final CommandBus commandBus = getCommandBus();
        checkState(commandBus != null, "CommandBus must be initialized");
        return commandBus;
    }

    private IllegalStateException missingEventHandler(Class<? extends Message> eventClass) {
        final String msg = format("Missing event handler for event class %s in the process manager class %s",
                                     eventClass, this.getClass());
        return new IllegalStateException(msg);
    }
}
