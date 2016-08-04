/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.examples.eventstore;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.TextFormat;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.server.event.grpc.EventStoreGrpc;

import java.util.concurrent.TimeUnit;

import static org.spine3.examples.eventstore.Constants.*;
import static org.spine3.examples.eventstore.SampleData.events;

/**
 * @author Alexander Yevsyukov
 */
public class EventPublisher {

    private final ManagedChannel channel;
    private final EventStoreGrpc.EventStoreBlockingStub blockingClient;

    public EventPublisher(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        this.blockingClient = EventStoreGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS);
        log().info(CHANNEL_SHUT_DOWN);
    }

    public void publish(Event record) {
        blockingClient.append(record);
        log().trace("Event published: {}", TextFormat.shortDebugString(record));
    }

    public static void main(String[] args) throws InterruptedException {
        final EventPublisher publisher = new EventPublisher(EVENT_STORE_SERVICE_HOST, PORT);

        try {
            for (GeneratedMessageV3 message : events) {
                // Simulate event id generation.
                final EventId eventId = Events.generateId();

                // Simulate `EventContext` creation. Normally version, and aggregate ID will be set by
                // the framework code when new instance of `EventContext` is generated.
                final EventContext context = EventContext.newBuilder().setEventId(eventId).build();

                final Event record = Events.createEvent(message, context);
                publisher.publish(record);
            }
        } finally {
            publisher.shutdown();
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EventPublisher.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
