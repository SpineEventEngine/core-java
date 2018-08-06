/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.server.command.CommandHandlingEntity;
import io.spine.server.command.model.CommandHandlerMethod;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandSequence;
import io.spine.server.event.EventReactor;
import io.spine.server.event.model.EventReactorMethod;
import io.spine.server.model.ReactorMethodResult;
import io.spine.server.procman.model.ProcessManagerClass;
import io.spine.validate.ValidatingBuilder;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.procman.model.ProcessManagerClass.asProcessManagerClass;

/**
 * A central processing unit used to maintain the state of the business process and determine
 * the next processing step based on intermediate results.
 *
 * <p>A process manager reacts to domain events in a cross-aggregate, eventually consistent manner.
 *
 * <p>Event and command handlers are invoked by the {@link ProcessManagerRepository}
 * that manages instances of a process manager class.
 *
 * <p>For more information on Process Managers, please see:
 * <ul>
 * <li>
 *   <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/ProcessManager.html">
 *       Process Manager Pattern</a>
 * <li><a href="http://kellabyte.com/2012/05/30/clarifying-the-saga-pattern/">
 *     Clarifying the Saga pattern</a> — the difference between Process Manager and Saga
 * <li><a href="https://dzone.com/articles/are-sagas-and-workflows-same-t">
 *     Are Sagas and Workflows the same...</a>
 * <li><a href="https://msdn.microsoft.com/en-us/library/jj591569.aspx">
 *     CQRS Journey Guide: A Saga on Sagas</a>
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
        extends CommandHandlingEntity<I, S, B>
        implements EventReactor {

    /** The Command Bus to post routed commands. */
    private volatile @MonotonicNonNull CommandBus commandBus;

    /**
     * Creates a new instance.
     *
     * @param  id an ID for the new instance
     * @throws IllegalArgumentException if the ID type is unsupported
     */
    protected ProcessManager(I id) {
        super(id);
    }

    @Internal
    @Override
    protected ProcessManagerClass<?> getModelClass() {
        return asProcessManagerClass(getClass());
    }

    @Override
    protected ProcessManagerClass<?> thisClass() {
        return (ProcessManagerClass<?>) super.thisClass();
    }

    /** The method to inject {@code CommandBus} instance from the repository. */
    void setCommandBus(CommandBus commandBus) {
        this.commandBus = checkNotNull(commandBus);
    }

    /**
     * Returns the {@code CommandBus} to which the commands produced by this process manager
     * are to be posted.
     */
    private CommandBus getCommandBus() {
        checkNotNull(commandBus, "CommandBus is not set in ProcessManager %s", this);
        return commandBus;
    }

    /**
     * {@inheritDoc}
     *
     * <p>In {@code ProcessManager}, this method must be called from an event reactor, a rejection
     * reactor, or a command handler.
     *
     * @throws IllegalStateException if the method is called from outside an event/rejection reactor
     *         or a command handler
     */
    @Override
    @VisibleForTesting
    protected B getBuilder() {
        return super.getBuilder();
    }

    /**
     * Dispatches the command to the handler method and transforms the output
     * into a list of events.
     *
     * @param  command the envelope with the command to dispatch
     * @return the list of events generated as the result of handling the command
     */
    @Override
    protected List<Event> dispatchCommand(CommandEnvelope command) {
        CommandHandlerMethod method = thisClass().getHandler(command.getMessageClass());
        CommandHandlerMethod.Result result =
                method.invoke(this, command);
        List<Event> events = result.produceEvents(command);
        return events;
    }

    /**
     * Dispatches an event to the event reactor method of the process manager.
     *
     * @param  event the envelope with the event
     * @return a list of produced events or an empty list if the process manager does not
     *         produce new events because of the passed event
     */
    List<Event> dispatchEvent(EventEnvelope event) {
        EventReactorMethod method =
                thisClass().getReactor(event.getMessageClass(), event.getOriginClass());
        ReactorMethodResult methodResult =
                method.invoke(this, event);
        List<Event> result = methodResult.produceEvents(event);
        return result;
    }

    /**
     * Creates a new empty command sequence for posting two or more commands in response to
     * an incoming command.
     *
     * <p>A typical usage looks like this:
     *
     * <pre>
     *     {@literal @}Assign
     *     CommandSplit on(MyCommand message, CommandContext context) {
     *         // Create new command messages here.
     *         return split(message, context)
     *                  .add(messageOne)
     *                  .add(messageTwo)
     *                  .postAll();
     *     }
     * </pre>
     *
     * <p>The routed commands are created on behalf of the actor of the original command.
     * That is, the {@code actor} and {@code zoneOffset} fields of created {@code CommandContext}
     * instances will be the same as in the incoming command.
     *
     * @param commandMessage the message of the command to split
     * @param context the context of the command
     * @return new empty sequence
     */
    protected CommandSequence.Split split(Message commandMessage, CommandContext context) {
        checkNotNull(commandMessage);
        checkNotNull(context);
        CommandSequence.Split result =
                CommandSequence.split(commandMessage, context, getCommandBus());
        return result;
    }

    /**
     * Creates a new empty command transformation sequence for posting exactly one command
     * in response to incoming one.
     *
     * <p>A typical usage looks like this:
     *
     * <pre>
     *     {@literal @}Assign
     *     CommandTransformed on(MyCommand message, CommandContext context) {
     *         // Create new command message here.
     *         return transform(message, context)
     *                  .to(anotherMessage)
     *                  .post();
     *     }
     * </pre>
     *
     * @param commandMessage the message of the command which we transform
     * @param context the context of the command
     * @return new empty sequence
     */
    protected CommandSequence.Transform transform(Message commandMessage, CommandContext context) {
        checkNotNull(commandMessage);
        checkNotNull(context);
        CommandSequence.Transform result =
            CommandSequence.transform(commandMessage, context, getCommandBus());
        return result;
    }

    @Override
    protected String getMissingTxMessage() {
        return "ProcessManager modification is not available this way. " +
                "Please modify the state from a command handling or event reacting method.";
    }
}
