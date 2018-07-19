/*
 * Copyright 2018, TeamDev. All rights reserved.
 *
 * Redistribution and use in origin and/or binary forms, with or without
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
import io.spine.core.Commands;
import io.spine.server.commandbus.CommandBus;

import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.commandbus.CommandSequence.checkSent;
import static io.spine.server.commandbus.CommandSequence.newAckObserver;

/**
 * Routes one or more commands in response to a command or an event.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandRouter {

    private final CommandRouted.Builder eventBuilder;
    private final CommandFactory commandFactory;

    /**
     * The message of the original command.
     */
    private final Message originMessage;

    /**
     * The context of the root command of the message we route.
     */
    private final CommandContext rootCommandContext;

    /**
     * The {@code CommandBus} to which we post commands.
     */
    private final CommandBus commandBus;

    /**
     * Command messages for commands that we post during routing.
     */
    private final Queue<Message> queue;

    CommandRouter(CommandBus commandBus, Message commandMessage, CommandContext commandContext) {
        this.commandBus = checkNotNull(commandBus);
        checkNotNull(commandMessage);
        checkNotNull(commandContext);
        this.rootCommandContext = commandContext;
        this.eventBuilder = CommandRouted
                .newBuilder()
                .setOrigin(Commands.toDispatched(commandMessage, commandContext));
        this.originMessage = commandMessage;
        this.queue = Queues.newConcurrentLinkedQueue();
        this.commandFactory = commandFactory(commandContext.getActorContext());
    }

    /**
     * Obtains the origin command message.
     */
    @VisibleForTesting
    Message getOriginMessage() {
        return originMessage;
    }

    @VisibleForTesting
    Message getOriginContext() {
        return eventBuilder.getOrigin()
                           .getContext();
    }

    /**
     * Adds {@code commandMessage} to be routed.
     */
    @CanIgnoreReturnValue
    public CommandRouter add(Message commandMessage) {
        queue.add(commandMessage);
        return this;
    }

    /**
     * Tests whether the queue of command messages to be routed is empty.
     *
     * @return {@code true} if the queue is empty, {@code false} otherwise
     */
    private boolean hasNext() {
        boolean result = !queue.isEmpty();
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
    private Command post(Message message) {
        Command command = produceCommand(message);
        SettableFuture<Ack> finishFuture = SettableFuture.create();
        StreamObserver<Ack> observer = newAckObserver(finishFuture);
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
        Command result = commandFactory.createBasedOnContext(commandMessage, rootCommandContext);
        return result;
    }

    /**
     * Gets and removes the next command message from the queue.
     *
     * @return the command message
     * @throws NoSuchElementException if the queue is already empty
     */
    private Message next() throws NoSuchElementException {
        Message result = queue.remove();
        return result;
    }

    /**
     * Creates a {@code CommandFactory} with the settings from the passed {@code ActorContext}
     */
    private static CommandFactory commandFactory(ActorContext actorContext) {
        ActorRequestFactory factory = ActorRequestFactory.fromContext(actorContext);
        return factory.command();
    }

    /**
     * Posts the added messages as commands to {@code CommandBus}.
     *
     * <p>The commands are posted in the order their messages were added.
     *
     * <p>The method returns after the last command was successfully posted.
     *
     * @return the event with the origin and produced commands
     */
    @SuppressWarnings("CheckReturnValue") // calling builder
    public CommandRouted postAll() {
        while (hasNext()) {
            Message message = next();
            Command command = post(message);
            eventBuilder.addGenerated(Commands.toDispatched(command));
        }

        return eventBuilder.build();
    }
}
