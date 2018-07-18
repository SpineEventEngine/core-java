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

import com.google.common.collect.Queues;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.client.ActorRequestFactory;
import io.spine.client.CommandFactory;
import io.spine.core.ActorContext;
import io.spine.core.CommandContext;
import io.spine.server.commandbus.CommandBus;

import java.util.NoSuchElementException;
import java.util.Queue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A sequence of commands to be posted to {@code CommandBus}.
 *
 * @author Alexander Yevsyukov
 */
public class CommandSequence {

    /** The bus to post commands. */
    private final CommandBus commandBus;

    /** The factory for producing commands. */
    private final CommandFactory commandFactory;

    /** Command messages for commands that we are going to post. */
    private final Queue<Message> queue;

    private CommandSequence(CommandBus bus, ActorContext actorContext) {
        this.commandBus = checkNotNull(bus);
        this.queue = Queues.newConcurrentLinkedQueue();
        this.commandFactory = ActorRequestFactory.fromContext(actorContext)
                                                 .command();
    }

    /**
     * Adds a {@code commandMessage} to the sequence.
     */
    @CanIgnoreReturnValue
    public CommandSequence add(Message commandMessage) {
        queue.add(commandMessage);
        return this;
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
     * A callback for errors occurred during posting a command from the sequence to
     * the {@code CommandBus}.
     */
    public interface ErrorHandler {
        void onError(Message commandMessage, CommandContext context, Throwable throwable);
    }
}
