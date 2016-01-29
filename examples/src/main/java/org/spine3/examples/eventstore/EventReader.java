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

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.server.event.Events;
import org.spine3.server.event.grpc.EventStoreGrpc;
import org.spine3.type.TypeName;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static org.spine3.examples.eventstore.Constants.*;

/**
 * A simple client connecting to {@code EventStore} for reading events.
 *
 * <p>Start this class after starting {@link EventPublisher}.
 *
 * @author Alexander Yevsyukov
 */
public class EventReader {

    private final ManagedChannel channel;
    private final EventStoreGrpc.EventStoreBlockingClient blockingClient;

    public EventReader(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        this.blockingClient = EventStoreGrpc.newBlockingStub(channel);
    }

    public void readEvents() {
        final Iterator<Event> iterator = blockingClient.read(EventStreamQuery.getDefaultInstance());
        while (iterator.hasNext()) {
            final Event next = iterator.next();
            final Message event = Events.getMessage(next);
            final EventContext context = next.getContext();
            log().info("Event: {} ({})", TypeName.of(event), TextFormat.shortDebugString(event));
            log().info("Context: {}", TextFormat.shortDebugString(context));
        }
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS);
        log().info(CHANNEL_SHUT_DOWN);
    }

    public static void main(String[] args) throws InterruptedException {
        final EventReader reader = new EventReader(EVENT_STORE_SERVICE_HOST, PORT);
        try {
            reader.readEvents();
        } finally {
            reader.shutdown();
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EventReader.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
