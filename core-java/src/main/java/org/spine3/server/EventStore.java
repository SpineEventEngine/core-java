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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A store of all events in a bounded context.
 *
 * @author Mikhail Mikhaylov
 */
public abstract class EventStore implements Closeable {

    private final Collection<EventStreamObserver> observers = new CopyOnWriteArrayList<>();
    private final Executor streamExecutor;

    /**
     * Creates a new instance running on the passed storage.
     *
     * @param streamExecutor the executor for iterating through the history of events for a new subscriber
     * @param storage the underlying storage for events.
     */
    public static EventStore create(Executor streamExecutor, EventStorage storage) {
        return new LocalImpl(streamExecutor, storage);
    }

    protected EventStore(Executor streamExecutor) {
        this.streamExecutor = streamExecutor;
    }

    public void append(EventRecord record) {
        store(record);
        notifySubscribers(record);
    }

    /**
     * Implement this method for storing passed record.
     *
     * @param record the event record to store.
     */
    protected abstract void store(EventRecord record);

    /**
     * Creates iterator for traversing through the history of messages
     * since the passed timestamp.
     *
     * @param timestamp the point in time from which events should be viewed
     * @return iterator instance
     */
    protected abstract Iterator<EventRecord> since(Timestamp timestamp);

    private void notifySubscribers(EventRecord record) {
        for (EventStreamObserver observer : observers) {
            notify(observer, record);
        }
    }

    /**
     * Subscribes the passed observer to receive the stream of events since the passed timestamp.
     *
     * <p>The observer will receive new events as the added to the {@code EventStore} after
     * receiving the requested history of events.
     *
     * <p>The operation is performed by the stream executor passed during construction
     * of this {@code EventStore}.
     *
     * @param timestamp the point in time since which include events into the stream
     * @param observer the observer for the requested event stream
     */
    public void subscribe(final Timestamp timestamp, final EventStreamObserver observer) {
        streamExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Iterator<EventRecord> eventRecords = since(timestamp);

                while (eventRecords.hasNext()) {
                    final EventRecord record = eventRecords.next();
                    EventStore.notify(observer, record);
                }
                addSubscriber(observer);
            }
        });
    }

    /**
     * Subscribes the passed observer to receive all new events added to the {@code EventStore}
     * <i>after</i> this call.
     *
     * @param observer the observer for the stream of new events
     */
    public void subscribe(EventStreamObserver observer) {
        addSubscriber(observer);
    }

    //TODO:2016-01-09:alexander.yevsyukov: Remove EventStreamObserver in favor of standard gRPC StreamObserver<EventRecord>
    @SuppressWarnings("TypeMayBeWeakened")
    private static void notify(EventStreamObserver observer, EventRecord record) {
        final Message event = Messages.fromAny(record.getEvent());
        final EventContext context = record.getContext();
        observer.onNext(event, context);
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

    /**
     * Creates new {@link ServiceBuilder} instance.
     */
    public static ServiceBuilder newServiceBuilder() {
        return new ServiceBuilder();
    }

    /**
     * The builder of {@code EventStore} instance exposed as gRPC service.
     *
     * @see org.spine3.server.grpc.EventStoreGrpc.EventStore
     */
    public static class ServiceBuilder {
        private Executor streamExecutor;
        private EventStorage eventStorage;

        public ServiceBuilder setStreamExecutor(Executor executor) {
            this.streamExecutor = checkNotNull(executor);
            return this;
        }

        public Executor getStreamExecutor() {
            return streamExecutor;
        }

        public ServiceBuilder setEventStorage(EventStorage eventStorage) {
            this.eventStorage = checkNotNull(eventStorage);
            return this;
        }

        public EventStorage getEventStorage() {
            return eventStorage;
        }

        public ServerServiceDefinition build() {
            checkNotNull(streamExecutor, "streamExecutor must be set");
            checkNotNull(eventStorage, "eventStorage must be set");
            final LocalImpl eventStore = new LocalImpl(streamExecutor, eventStorage);
            final EventStoreGrpc.EventStore grpcService = new GrpcService(eventStore);
            final ServerServiceDefinition result = EventStoreGrpc.bindService(grpcService);
            return result;
        }
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
