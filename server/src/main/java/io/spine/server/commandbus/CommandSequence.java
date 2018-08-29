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
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.EventEnvelope;
import io.spine.core.Status;
import io.spine.core.TenantId;
import io.spine.system.server.SystemGateway;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * A sequence of commands to be posted to {@code CommandBus}.
 *
 * @param <O> the type of origin ID which caused the sequence
 * @param <R> the type of the result generated when the command sequence is posted
 * @param <B> the type of the result builder
 * @param <S> the type of the sequence for the return type covariance
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ClassReferencesSubclass")
@Internal
public abstract class CommandSequence<O extends Message,
                                      R extends Message,
                                      B extends Message.Builder,
                                      S extends CommandSequence<O, R, B, S>> {

    /** The ID of the message which caused the sequence. */
    private final O origin;

    /** The context in which the sequence is generated. */
    private final ActorContext actorContext;

    /** The factory for producing commands. */
    private final CommandFactory commandFactory;

    /** Command messages for commands that we are going to post. */
    private final Queue<Message> queue;

    /** The handler for the posting errors. */
    private ErrorHandler errorHandler = new DefaultErrorHandler();

    CommandSequence(O origin, ActorContext actorContext) {
        this.origin = checkNotNull(origin);
        this.queue = Queues.newConcurrentLinkedQueue();
        this.actorContext = actorContext;
        this.commandFactory = ActorRequestFactory.fromContext(actorContext)
                                                 .command();
    }

    protected O origin() {
        return origin;
    }

    /**
     * Creates an empty sequence for splitting the source command into several ones.
     */
    public static Split split(CommandEnvelope command) {
        return new Split(command);
    }

    /**
     * Creates an empty sequence for transforming incoming command into another one.
     */
    public static Transform transform(CommandEnvelope command) {
        return new Transform(command);
    }

    /**
     * Creates an empty sequence for creating a command in response to the passed event.
     */
    public static SingleCommand inResponseTo(EventEnvelope event) {
        return new SingleCommand(event.getId(), event.getActorContext());
    }

    /**
     * Creates an empty sequence for creating two or more commands in response to the passed event.
     */
    public static SeveralCommands respondMany(EventEnvelope event) {
        return new SeveralCommands(event.getId(), event.getActorContext());
    }

    /**
     * Creates a new builder for the result generated when all the commands are posted.
     */
    protected abstract B newBuilder();

    /**
     * Adds the posted command to the result builder.
     */
    protected abstract void addPosted(B builder, Command command, SystemGateway gateway);

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
     */
    protected R postAll(CommandBus bus) {
        SystemGateway gateway = gateway(bus);
        B builder = newBuilder();
        while (hasNext()) {
            Message message = next();
            Optional<Command> posted = post(message, bus);
            if (posted.isPresent()) {
                Command command = posted.get();
                addPosted(builder, command, gateway);
            }
        }
        @SuppressWarnings("unchecked") /* The type is ensured by correct couples of R,B types passed
            to in the classes derived from `CommandSequence` that are limited to this package. */
        R result = (R) builder.build();
        gateway.postCommand(result);
        return result;
    }

    private SystemGateway gateway(CommandBus bus) {
        TenantId tenantId = actorContext.getTenantId();
        return bus.gatewayFor(tenantId);
    }

    /**
     * Creates a new command from the passed message and posts it
     * to the {@code CommandBus}.
     *
     * <p>This method waits till the posting of the command is finished.
     * @return the created and posted {@code Command}
     */
    private Optional<Command> post(Message message, CommandBus bus) {
        Command command = commandFactory.create(message);
        SettableFuture<Ack> finishFuture = SettableFuture.create();
        StreamObserver<Ack> observer = newAckObserver(finishFuture);
        bus.post(command, observer);
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

    private static StreamObserver<Ack> newAckObserver(SettableFuture<Ack> finishFuture) {
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

    private static void checkSent(Command command, Ack ack) {
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
}
