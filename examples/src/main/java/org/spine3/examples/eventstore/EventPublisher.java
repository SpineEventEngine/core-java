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

import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.server.grpc.EventStoreGrpc;
import org.spine3.util.Commands;
import org.spine3.util.Events;
import org.spine3.util.Users;

import java.util.concurrent.TimeUnit;

/**
 * @author Alexander Yevsyukov
 */
public class EventPublisher {

    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static final String EVENT_STORE_SERVICE_HOST = "localhost";
    private static final int SHUTDOWN_TIMEOUT_SEC = 5;

    private final ManagedChannel channel;
    private final EventStoreGrpc.EventStoreBlockingClient blockingClient;

    public EventPublisher(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        this.blockingClient = EventStoreGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    public void publish(EventRecord record) {
        blockingClient.append(record);
    }

    public static void main(String[] args) throws InterruptedException {
        final EventPublisher publisher = new EventPublisher(EVENT_STORE_SERVICE_HOST, Constants.PORT);

        try {
            EventRecord record = Events.createEventRecord(StringValue.newBuilder().setValue("String 123").build(),
                                                EventContext.newBuilder()
                                                        .setEventId(Events.generateId(Commands.generateId(Users.newUserId(EventPublisher.class.getSimpleName()))))
                                                        .setVersion(1)
                                                        .build());
            publisher.publish(record);

            record = Events.createEventRecord(UInt32Value.newBuilder().setValue(100).build(),
                                                EventContext.newBuilder()
                                                        .setEventId(Events.generateId(Commands.generateId(Users.newUserId(EventPublisher.class.getSimpleName()))))
                                                        .setVersion(10)
                                                        .build());
            publisher.publish(record);

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
