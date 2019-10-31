/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.client;

import io.grpc.stub.StreamObserver;
import io.spine.base.Field;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.UserId;
import io.spine.logging.Logging;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.Filters.eq;
import static io.spine.util.Preconditions2.checkNotDefaultArg;
import static java.lang.String.format;

/**
 * Subscribes to events which originate from the given command and delivers them to the passed
 * event consumers.
 */
final class EventsAfterCommand implements Logging {

    private final Client client;
    private final UserId user;
    private final Command command;
    private final MultiEventConsumers consumers;

    private EventsAfterCommand(Client client, Command cmd, MultiEventConsumers consumers) {
        this.client = checkNotNull(client);
        this.command = checkNotDefaultArg(cmd);
        this.user = cmd.getContext()
                       .getActorContext()
                       .getActor();
        this.consumers = checkNotNull(consumers);
    }

    static Subscription subscribe(Client client,
                                  Command command,
                                  MultiEventConsumers consumers,
                                  @Nullable ErrorHandler errorHandler) {
        EventsAfterCommand commandOutcome = new EventsAfterCommand(client, command, consumers);
        Subscription result = commandOutcome.subscribeWith(errorHandler);
        return result;
    }

    private Subscription subscribeWith(@Nullable ErrorHandler errorHandler) {
        Topic topic = allEventsOf(command);
        StreamObserver<Event> observer = consumers.toObserver(errorHandler);
        return client.subscribeTo(topic, observer);
    }

    /**
     * Creates a subscription topic for all events for which the passed command is the origin.
     */
    private Topic allEventsOf(Command c) {
        String fieldName = pastMessageField();
        Topic topic =
                client.requestOf(user)
                      .topic()
                      .select(Event.class)
                      .where(eq(fieldName, c.asMessageOrigin()))
                      .build();
        return topic;
    }

    /**
     * Obtains the path to the "context.past_message" field of {@code Event}.
     *
     * <p>This method is safer than using a string constant because it relies on field numbers,
     * rather than names (that might be changed).
     */
    private static String pastMessageField() {
        Field context = Field.withNumberIn(Event.CONTEXT_FIELD_NUMBER, Event.getDescriptor());
        Field pastMessage = Field.withNumberIn(EventContext.PAST_MESSAGE_FIELD_NUMBER,
                                               EventContext.getDescriptor());
        return format("%s.%s", context.toString(), pastMessage.toString());
    }
}
