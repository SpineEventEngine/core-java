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

package io.spine.testing.server.blackbox;

import com.google.common.annotations.VisibleForTesting;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventStreamQuery;
import io.spine.testing.client.blackbox.Acknowledgements;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.grpc.StreamObservers.memoizingObserver;

/**
 * An output of a {@link BlackBoxBoundedContext}, which provides emitted domain messages.
 */
@VisibleForTesting
final class BlackBoxOutput {

    private final EventBus eventBus;
    private final CommandMemoizingTap commandTap;
    private final MemoizingObserver<Ack> observer;

    BlackBoxOutput(EventBus eventBus,
                   CommandMemoizingTap commandTap,
                   MemoizingObserver<Ack> observer) {
        this.eventBus = checkNotNull(eventBus);
        this.commandTap = checkNotNull(commandTap);
        this.observer = checkNotNull(observer);
    }

    /** Obtains all commands posted to a command bus. */
    EmittedCommands emittedCommands() {
        List<Command> commands = allCommands();
        return new EmittedCommands(commands);
    }

    /** Obtains acknowledgements of {@linkplain #emittedCommands() emitted commands}. */
    Acknowledgements commandAcks() {
        return new Acknowledgements(observer.responses());
    }

    /**
     * Reads all events emitted in a bounded context.
     */
    EmittedEvents emittedEvents() {
        MemoizingObserver<Event> queryObserver = memoizingObserver();
        eventBus.getEventStore()
                .read(allEventsQuery(), queryObserver);
        List<Event> responses = queryObserver.responses();
        return new EmittedEvents(responses);
    }

    private List<Command> allCommands() {
        return commandTap.commands();
    }

    /**
     * Creates a new {@link io.spine.server.event.EventStreamQuery} without any filters.
     */
    private static EventStreamQuery allEventsQuery() {
        return EventStreamQuery.newBuilder()
                               .build();
    }
}
