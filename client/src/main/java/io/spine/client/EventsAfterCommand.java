/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.collect.ImmutableSet;
import io.grpc.stub.StreamObserver;
import io.spine.base.EventMessage;
import io.spine.base.Field;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.UserId;
import io.spine.logging.Logging;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.spine.client.Filters.eq;
import static io.spine.util.Preconditions2.checkNotDefaultArg;

/**
 * Subscribes to events which originate from the given command and arranges the delivery
  * to the passed event consumers.
 */
final class EventsAfterCommand implements Logging {

    private final Client client;
    private final UserId user;
    private final Command command;
    private final MultiEventConsumers consumers;

    static @NonNull ImmutableSet<Subscription>
    subscribe(Client client,
              Command command,
              MultiEventConsumers consumers,
              @Nullable ErrorHandler errorHandler) {
        EventsAfterCommand commandOutcome = new EventsAfterCommand(client, command, consumers);
        ImmutableSet<Subscription> result = commandOutcome.subscribeWith(errorHandler);
        return result;
    }

    private EventsAfterCommand(Client client, Command cmd, MultiEventConsumers consumers) {
        this.client = checkNotNull(client);
        this.command = checkNotDefaultArg(cmd);
        this.user = cmd.actor();
        this.consumers = checkNotNull(consumers);
    }

    private ImmutableSet<Subscription> subscribeWith(@Nullable ErrorHandler errorHandler) {
        ImmutableSet<Topic> topics = eventsOf(command);
        StreamObserver<Event> observer = consumers.toObserver(errorHandler);
        ImmutableSet<Subscription> subscriptions =
                topics.stream()
                      .map((topic) -> client.subscriptions()
                                            .subscribeTo(topic, observer))
                      .collect(toImmutableSet());
        return subscriptions;
    }

    /**
     * Creates subscription topics for the subscribed events which have the passed command
     * as the origin.
     */
    private ImmutableSet<Topic> eventsOf(Command c) {
        Field pastMessage =
                EventContext.Field.pastMessage()
                                  .getField();
        String fieldName =
                Event.Field.context()
                           .getField()
                           .nested(pastMessage)
                           .toString();
        ImmutableSet<Class<? extends EventMessage>> eventTypes = consumers.eventTypes();
        TopicFactory topic = client.requestOf(user)
                                   .topic();
        ImmutableSet<Topic> topics =
                eventTypes.stream()
                          .map((eventType) -> topic.select(eventType)
                                                   .where(eq(fieldName, c.asMessageOrigin()))
                                                   .build())
                          .collect(toImmutableSet());
        return topics;
    }
}
