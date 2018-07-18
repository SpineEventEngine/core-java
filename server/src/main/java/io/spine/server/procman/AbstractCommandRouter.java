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

import com.google.common.collect.Iterators;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.SettableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.client.ActorRequestFactory;
import io.spine.client.CommandFactory;
import io.spine.core.Ack;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandId;
import io.spine.core.Status;
import io.spine.server.commandbus.CommandBus;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * Abstract base for command routers.
 *
 * @author Alexander Yevsyukov
 */
abstract class AbstractCommandRouter<T extends AbstractCommandRouter> {

    /**
     * The command that we route.
     */
    private final Command source;

    /**
     * The {@code CommandBus} to which we post commands.
     */
    private final CommandBus commandBus;

    /**
     * Command messages for commands that we post during routing.
     */
    private final Queue<Message> queue;

    AbstractCommandRouter(CommandBus commandBus,
                          Message commandMessage,
                          CommandContext commandContext) {
        this.commandBus = checkNotNull(commandBus);
        checkNotNull(commandMessage);
        checkNotNull(commandContext);
        this.source = asCommand(commandMessage, commandContext);
        this.queue = Queues.newConcurrentLinkedQueue();
    }

    /**
     * Returns typed reference to {@code this}.
     *
     * <p>This method provides return type covariance in fluent API methods.
     *
     * @see #add(Message)
     * @see #addAll(Iterable)
     */
    protected abstract T getThis();

    /**
     * Obtains the source command to be routed.
     */
    protected Command getSource() {
        return source;
    }

    /**
     * Adds {@code commandMessage} to be routed.
     */
    @CanIgnoreReturnValue
    public T add(Message commandMessage) {
        queue.add(commandMessage);
        return getThis();
    }

    /**
     * Adds all command messages from the passed iterable.
     */
    @CanIgnoreReturnValue
    protected T addAll(Iterable<Message> iterable) {
        for (Message message : iterable) {
            add(message);
        }
        return getThis();
    }

    /**
     * Tests whether the queue of command messages to be routed is empty.
     *
     * @return {@code true} if the queue is empty, {@code false} otherwise
     */
    protected boolean hasNext() {
        boolean result = !queue.isEmpty();
        return result;
    }

    protected Iterator<Message> commandMessages() {
        return Iterators.unmodifiableIterator(queue.iterator());
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
    protected Command route(Message message) {
        Command command = produceCommand(message);
        SettableFuture<Ack> finishFuture = SettableFuture.create();
        StreamObserver<Ack> observer = newAckingObserver(finishFuture);
        commandBus.post(command, observer);
        Ack ack;
        // Wait till the call is completed.
        try {
            ack = finishFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
        checkSent(command, ack);
        return command;
    }

    private Command produceCommand(Message commandMessage) {
        CommandContext sourceContext = source.getContext();
        CommandFactory commandFactory = commandFactory(sourceContext);
        Command result = commandFactory.createBasedOnContext(commandMessage, sourceContext);
        return result;
    }

    /**
     * Gets and removes the next command message from the queue.
     *
     * @return the command message
     * @throws NoSuchElementException if the queue is already empty
     */
    protected Message next() throws NoSuchElementException {
        Message result = queue.remove();
        return result;
    }

    private static StreamObserver<Ack> newAckingObserver(
            SettableFuture<Ack> finishFuture) {
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

    /**
     * Creates a {@code CommandFactory} using the {@linkplain io.spine.core.UserId actor},
     * {@linkplain io.spine.core.TenantId tenant ID} and {@linkplain io.spine.time.ZoneOffset
     * zone offset} from the given command context.
     */
    private static CommandFactory commandFactory(CommandContext sourceContext) {
        ActorContext actorContext = sourceContext.getActorContext();
        ActorRequestFactory factory = ActorRequestFactory.fromContext(actorContext);
        return factory.command();
    }

    private static Command asCommand(Message message, CommandContext context) {
        Command command = commandFactory(context).createWithContext(message, context);
        return command;
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
}
