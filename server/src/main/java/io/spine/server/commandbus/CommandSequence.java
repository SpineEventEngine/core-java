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

package io.spine.server.commandbus;

import com.google.common.base.Throwables;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.SettableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.client.ActorRequestFactory;
import io.spine.client.CommandFactory;
import io.spine.core.Ack;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandId;
import io.spine.core.Status;
import io.spine.server.procman.CommandSplit;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.core.Commands.toDispatched;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * A sequence of commands to be posted to {@code CommandBus}.
 *
 * @param <R> the type of the result generated when the command sequence is posted
 * @param <B> the type of the result builder
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ClassReferencesSubclass")
@Internal
public abstract class CommandSequence<R extends Message, B extends Message.Builder,
        S extends CommandSequence<R, B, S>> {

    /** The bus to post commands. */
    private final CommandBus commandBus;

    /** The factory for producing commands. */
    private final CommandFactory commandFactory;

    /** Command messages for commands that we are going to post. */
    private final Queue<Message> queue;

    /** The handler for the posting errors. */
    private ErrorHandler errorHandler = new DefaultErrorHandler();

    private CommandSequence(CommandBus bus, ActorContext actorContext) {
        this.commandBus = checkNotNull(bus);
        this.queue = Queues.newConcurrentLinkedQueue();
        this.commandFactory = ActorRequestFactory.fromContext(actorContext)
                                                 .command();
    }

    /**
     * Creates a sequence for splitting the source command into several ones.
     *
     * @param bus the command bus to post commands
     * @param commandMessage the message of the source command
     * @param context the context of the source command
     * @return new empty sequence
     */
    public static Split split(CommandBus bus, Message commandMessage, CommandContext context) {
        return new Split(bus, commandMessage, context);
    }

    /**
     * Creates a new builder for the result generated when all the commands are posted.
     */
    protected abstract B newBuilder();

    /**
     * Adds the posted command to the result builder.
     */
    protected abstract void addPosted(B builder, Message message, CommandContext context);

    /**
     * Adds a command message to the sequence of commands to be posted.
     */
    @CanIgnoreReturnValue
    protected  S add(Message commandMessage) {
        queue.add(commandMessage);
        return getThis();
    }

    @SuppressWarnings("unchecked") // The cast preserved by generic parameters of the class.
    private S getThis() {
        return (S) this;
    }

    /**
     * Defines a custom handler for posting errors.
     *
     * <p>When the handler is not defined {@link CommandPostingException} will be thrown
     * if an error occurs.
     */
    @CanIgnoreReturnValue
    public S onError(ErrorHandler handler) {
        errorHandler = checkNotNull(handler);
        return getThis();
    }

    /**
     * Obtains the size of the command sequence.
     */
    protected int size() {
        return queue.size();
    }

    /**
     * Tests whether the queue of command messages to be posted is empty.
     */
    private boolean hasNext() {
        boolean result = !queue.isEmpty();
        return result;
    }

    /**
     * Gets and removes the next command message from the queue.
     */
    private Message next() throws NoSuchElementException {
        Message result = queue.remove();
        return result;
    }

    /**
     * Posts all commands to the {@code CommandBus} in the order that they were added.
     *
     * <p>The {@linkplain #size() size} of the sequence after this method is zero <em>if</em>
     * all the commands were posted successfully.
     *
     * @return the result
     */
    public R postAll() {
        B builder = newBuilder();
        while (hasNext()) {
            Message message = next();
            Optional<Command> posted = post(message);
            if (posted.isPresent()) {
                Command command = posted.get();
                addPosted(builder, message, command.getContext());
            }
        }
        @SuppressWarnings("unchecked") /* The type is ensured by correct couples of R,B types passed
            to in the classes derived from `CommandSequence` that are limited to this package. */
        R result = (R) builder.build();
        return result;
    }

    /**
     * Creates a new command from the passed message and posts it
     * to the {@code CommandBus}.
     *
     * <p>This method waits till the posting of the command is finished.
     *
     * @param message the command message for the new command
     * @return the created and posted {@code Command}
     */
    private Optional<Command> post(Message message) {
        Command command = commandFactory.create(message);
        SettableFuture<Ack> finishFuture = SettableFuture.create();
        StreamObserver<Ack> observer = newAckObserver(finishFuture);
        commandBus.post(command, observer);
        Ack ack;
        // Wait till the call is completed.
        try {
            ack = finishFuture.get();
            checkSent(command, ack);
        } catch (InterruptedException | ExecutionException e) {
            Throwable rootCause = Throwables.getRootCause(e);
            errorHandler.onError(message, command.getContext(), rootCause);
            return Optional.empty();
        }
        return Optional.of(command);
    }

    public static StreamObserver<Ack> newAckObserver(SettableFuture<Ack> finishFuture) {
        return new StreamObserver<Ack>() {
            @Override
            public void onNext(Ack value) {
                finishFuture.set(value);
            }

            @Override
            public void onError(Throwable t) {
                finishFuture.setException(t);
            }

            @Override
            public void onCompleted() {
                // Do nothing. It's just a confirmation of successful post to Command Bus.
            }
        };
    }

    public static void checkSent(Command command, Ack ack) {
        Status status = ack.getStatus();
        CommandId routedCommandId = unpack(ack.getMessageId());
        CommandId commandId = command.getId();
        checkState(commandId.equals(routedCommandId),
                   "Unexpected command posted. Intending (%s) but was (%s).",
                   commandId,
                   routedCommandId);
        checkState(status.getStatusCase() == Status.StatusCase.OK,
                   "Command posting failed with status: %s.",
                   status);
    }

    /**
     * A callback for errors occurred during posting a command from the sequence to
     * the {@code CommandBus}.
     */
    public interface ErrorHandler {
        void onError(Message commandMessage, CommandContext context, Throwable throwable);
    }

    /**
     * Throws a {@code CommandPostingException} when an error occurred during posting
     * a command.
     */
    private static class DefaultErrorHandler implements ErrorHandler {

        @Override
        public void onError(Message commandMessage, CommandContext context, Throwable throwable) {
            throw new CommandPostingException(commandMessage, context, throwable);
        }
    }

    /**
     * Abstract base for command sequences initiated from a source command.
     */
    private abstract static
    class OnCommand<R extends Message,
                    B extends Message.Builder,
                    S extends CommandSequence<R, B, S>>
            extends CommandSequence<R, B, S> {

        private final Message sourceMessage;
        private final CommandContext sourceContext;

        protected OnCommand(CommandBus commandBus, Message message, CommandContext context) {
            super(commandBus, context.getActorContext());
            this.sourceMessage = message;
            this.sourceContext = context;
        }

        protected Message getSourceMessage() {
            return sourceMessage;
        }

        protected CommandContext getSourceContext() {
            return sourceContext;
        }
    }

    /**
     * A {@code CommandSequence} of two or more commands which is generated in response to
     * a source command.
     */
    public static final class Split extends OnCommand<CommandSplit, CommandSplit.Builder, Split> {

        private Split(CommandBus commandBus, Message sourceMessage, CommandContext sourceContext) {
            super(commandBus, sourceMessage, sourceContext);
        }

        /** {@inheritDoc} */
        @CanIgnoreReturnValue
        @Override
        public Split add(Message commandMessage) {
            return super.add(commandMessage);
        }

        @Override
        protected CommandSplit.Builder newBuilder() {
            CommandSplit.Builder result = CommandSplit
                    .newBuilder()
                    .setSource(toDispatched(getSourceMessage(), getSourceContext()));
            return result;
        }

        @SuppressWarnings("CheckReturnValue") // calling builder method
        @Override
        protected void addPosted(CommandSplit.Builder builder,
                                 Message message,
                                 CommandContext context) {
            builder.addProduced(toDispatched(message, context));
        }

        @Override
        public CommandSplit postAll() {
            checkState(size() >= 2,
                       "The split sequence must have at least two commands. " +
                               "For converting a command to another please use " +
                               "`CommandSequence.transform()`."
            );
            return super.postAll();
        }
    }
}
