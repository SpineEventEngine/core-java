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

package io.spine.server.procman;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.server.command.CommandHandlingEntity;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.reflect.CommandHandlerMethod;
import io.spine.server.reflect.EventSubscriberMethod;
import io.spine.server.reflect.RejectionSubscriberMethod;
import io.spine.validate.ValidatingBuilder;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.server.reflect.EventSubscriberMethod.getMethod;

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
 * @author Alexander Yevsyukov
 */
public abstract class ProcessManager<I,
                                     S extends Message,
                                     B extends ValidatingBuilder<S, ? extends Message.Builder>>
        extends CommandHandlingEntity<I, S, B> {

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

    @Override
    @VisibleForTesting      // Overridden to expose this method to tests.
    protected B getBuilder() {
        return super.getBuilder();
    }

    /**
     * Dispatches the command to the handler method and transforms the output
     * into a list of events.
     *
     * @param cmd the envelope with the command to dispatch
     * @return the list of events generated as the result of handling the command.
     */
    @Override
    protected List<Event> dispatchCommand(CommandEnvelope cmd) {
        final List<? extends Message> messages = super.dispatchCommand(cmd);
        final List<Event> result = toEvents(messages, cmd);
        return result;
    }

    /**
     * Transforms the passed list of event messages into the list of events.
     *
     * @param eventMessages event messages for which generate events
     * @param envelope the context of the command which generated the event messages
     * @return list of events
     */
    private List<Event> toEvents(List<? extends Message> eventMessages,
                                 final CommandEnvelope envelope) {
        return CommandHandlerMethod.toEvents(getProducerId(),
                                             getVersion(),
                                             eventMessages,
                                             envelope);
    }

    /**
     * Dispatches an event to the event subscriber method of the process manager.
     *
     * @param event the envelope with the event
     */
    void dispatchEvent(EventEnvelope event)  {
        checkNotNull(event);

        final Message eventMessage = event.getMessage();
        final EventSubscriberMethod method = getMethod(getClass(), eventMessage);
        method.invoke(this, eventMessage, event.getEventContext());
    }

    /**
     * Dispatched a rejection to the subscribing method of the process manager.
     *
     * @param rejection the envelope with the rejection
     */
    void dispatchRejection(RejectionEnvelope rejection) {
        checkNotNull(rejection);
        final Message rejectionMessage = rejection.getMessage();
        final Message commandMessage = rejection.getCommandMessage();
        final RejectionSubscriberMethod method =
                RejectionSubscriberMethod.getMethod(getClass(), rejectionMessage, commandMessage);
        method.invoke(this, rejectionMessage, commandMessage, rejection.getCommandContext());
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
     * in response to a command received by the {@code ProcessManager} and post these commands
     * one by one.
     * 
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
    protected IteratingCommandRouter newIteratingRouterFor(Message commandMessage,
                                                           CommandContext commandContext) {
        checkNotNull(commandMessage);
        checkNotNull(commandContext);
        final CommandBus commandBus = ensureCommandBus();
        final IteratingCommandRouter router =
                new IteratingCommandRouter(commandBus, commandMessage, commandContext);
        return router;
    }

    private CommandBus ensureCommandBus() {
        final CommandBus commandBus = getCommandBus();
        checkState(commandBus != null, "CommandBus must be initialized");
        return commandBus;
    }

    @Override
    protected String getMissingTxMessage() {
        return "ProcessManager modification is not available this way. Please modify the state from" +
                " a command handling or event subscribing method.";
    }

    /**
     * Provides type information for process manager classes.
     */
    static class TypeInfo {

        private TypeInfo() {
            // Prevent construction of this utility class.
        }

        /**
         * Obtains the set of command classes handled by passed process manager class.
         *
         * @param pmClass the process manager class to inspect
         * @return immutable set of command classes or an empty set if no commands are handled
         */
        static Set<CommandClass> getCommandClasses(Class<? extends ProcessManager> pmClass) {
            return ImmutableSet.copyOf(CommandHandlerMethod.inspect(pmClass));
        }

        /**
         * Returns the set of event classes handled by the process manager.
         *
         * @param pmClass the process manager class to inspect
         * @return immutable set of event classes or an empty set if no events are handled
         */
        static Set<EventClass> getEventClasses(Class<? extends ProcessManager> pmClass) {
            return EventSubscriberMethod.inspect(pmClass);
        }

        /**
         * Obtains the set of rejection classes to which process managers of the passed class are
         * subscribed.
         *
         * @param pmClass the process manager class to inspect
         * @return immutable set of rejection classes or an empty set if the class is not subscribed
         * to rejections
         */
        static Set<RejectionClass> getRejectionClasses(Class<? extends ProcessManager> pmClass) {
            return ImmutableSet.copyOf(RejectionSubscriberMethod.inspect(pmClass));
        }
    }
}
