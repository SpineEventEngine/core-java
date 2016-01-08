/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
package org.spine3.server;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.client.EventStreamObserver;
import org.spine3.protobuf.Messages;
import org.spine3.server.grpc.EventStoreGrpc;
import org.spine3.server.storage.EventStorage;
import org.spine3.util.Events;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * A store of all events in a bounded context.
 *
 * @author Mikhail Mikhaylov
 */
public abstract class EventStore implements Closeable {

    private final Collection<EventStreamObserver> observers = new CopyOnWriteArrayList<>();
    private final Executor subscriptionExecutor;

    /**
     * Creates a new instance running on the passed storage.
     *
     * @param catchUpExecutor the executor for iterating through the history of events for a new subscriber
     * @param storage the underlying storage for the store.
     */
    public static EventStore create(Executor catchUpExecutor, EventStorage storage) {
        return new LocalImpl(catchUpExecutor, storage);
    }

    protected EventStore(Executor subscriptionExecutor) {
        this.subscriptionExecutor = subscriptionExecutor;
    }

    protected abstract void store(EventRecord record);

    protected abstract Iterator<EventRecord> since(Timestamp timestamp);

    public void append(EventRecord record) {
        store(record);
        notifySubscribers(record);
    }

    private void notifySubscribers(EventRecord record) {
        for (EventStreamObserver observer : observers) {
            notify(observer, record);
        }
    }

    public void subscribe(Timestamp timestamp, final EventStreamObserver observer) {
        final Iterator<EventRecord> eventRecords = since(timestamp);

        subscriptionExecutor.execute(new Runnable() {
            @Override
            public void run() {
                while (eventRecords.hasNext()) {
                    final EventRecord record = eventRecords.next();
                    EventStore.notify(observer, record);
                }
                addSubscriber(observer);
            }
        });
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private static void notify(EventStreamObserver observer, EventRecord record) {
        final Message event = Messages.fromAny(record.getEvent());
        final EventContext context = record.getContext();
        observer.onNext(event, context);
    }

    public void subscribe(EventStreamObserver observer) {
        addSubscriber(observer);
    }

    private void addSubscriber(EventStreamObserver observer) {
        observers.add(observer);
    }

    @Override
    public void close() throws IOException {
        for (EventStreamObserver observer : observers) {
            observer.onCompleted();
        }
    }

    /**
     * A locally running implementation.
     */
    private static class LocalImpl extends EventStore {

        private final EventStorage storage;

        /**
         * Creates a new instance running on the passed storage.
         *
         * @param storage the underlying storage for the store.
         */
        private LocalImpl(Executor catchUpExecutor, EventStorage storage) {
            super(catchUpExecutor);
            this.storage = storage;
        }

        @Override
        protected void store(EventRecord record) {
            storage.store(record);
        }

        @Override
        protected Iterator<EventRecord> since(Timestamp timestamp) {
            return storage.since(timestamp);
        }

        /**
         * Closes the underlying storage.
         *
         * @throws IOException if the attempt to close the storage throws an exception
         */
        @Override
        public void close() throws IOException {
            super.close();
            storage.close();
        }

    }

    private static class GrpcService implements EventStoreGrpc.EventStore {

        private final LocalImpl eventStore;

        private GrpcService(LocalImpl eventStore) {
            this.eventStore = eventStore;
        }

        @Override
        public void append(EventRecord request, StreamObserver<Response> responseObserver) {
            try {
                log().info("Appending " + Messages.toJson(request));

                eventStore.append(request);
                responseObserver.onNext(Responses.RESPONSE_OK);
                responseObserver.onCompleted();
            } catch (RuntimeException e) {
                responseObserver.onError(e);
            }
        }

        /**
         * Adapts {@code StreamObserver<EventRecord>} instance to {@code EventStreamObserver}.
         */
        private static class Adapter implements EventStreamObserver {

            private final StreamObserver<EventRecord> streamObserver;

            private Adapter(StreamObserver<EventRecord> streamObserver) {
                this.streamObserver = streamObserver;
            }

            @Override
            public void onNext(Message event, EventContext ctx) {
                final EventRecord record = Events.createEventRecord(event, ctx);
                streamObserver.onNext(record);
            }

            @Override
            public void onError(Throwable t) {
                log().error("Error encountered ", t);
                streamObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                streamObserver.onCompleted();
            }
        }

        @Override
        public void subscribeSince(Timestamp request, StreamObserver<EventRecord> responseObserver) {
            log().info("Subscribe since timestamp: {}", TimeUtil.toString(request));
            eventStore.subscribe(request, new Adapter(responseObserver));
        }

        @Override
        public void subscribe(Empty request, StreamObserver<EventRecord> responseObserver) {
            log().info("Subscribe from now.");
            eventStore.subscribe(new Adapter(responseObserver));
        }
    }

    public static io.grpc.Server createServer(Executor subscriptionExecutor, EventStorage storage, int port) {
        final GrpcService grpcService = new GrpcService(new LocalImpl(subscriptionExecutor, storage));
        final ServerServiceDefinition service = EventStoreGrpc.bindService(grpcService);

        final ServerBuilder builder = ServerBuilder.forPort(port)
                .addService(service);
        return builder.build();
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EventStore.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
