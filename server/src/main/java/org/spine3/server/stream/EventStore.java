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
package org.spine3.server.stream;

import com.google.protobuf.TextFormat;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.EventRecord;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.storage.EventStorage;
import org.spine3.server.stream.grpc.EventStoreGrpc;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A store of all events in a bounded context.
 *
 * @author Alexander Yevsyukov
 */
public abstract class EventStore implements AutoCloseable {

    private final Executor streamExecutor;
    @Nullable
    private final Logger logger;

    /**
     * Creates a builder for locally running {@code EventStore}.
     *
     * @return new builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates new {@link ServiceBuilder} for building {@code EventStore} instance
     * that will be exposed as a gRPC service.
     */
    public static ServiceBuilder newServiceBuilder() {
        return new ServiceBuilder();
    }

    /**
     * Constructs an instance with the passed executor for returning streams.
     *
     * @param streamExecutor the executor for updating new subscribers
     * @param logger debug logger instance
     */
    protected EventStore(Executor streamExecutor, @Nullable Logger logger) {
        this.streamExecutor = streamExecutor;
        this.logger = logger;
    }

    /**
     * Appends the passed event record to the history of events.
     *
     * @param record the record to append
     */
    public void append(EventRecord record) {
        store(record);
        logStored(record);
    }

    /**
     * Implement this method for storing the passed event record.
     *
     * @param record the event record to store.
     */
    protected abstract void store(EventRecord record);

    /**
     * Creates iterator for traversing through the history of event records
     * matching the passed query.
     *
     * @param query the query filtering the history
     * @return iterator instance
     */
    protected abstract Iterator<EventRecord> iterator(EventStreamQuery query);

    /**
     * Creates the steam with event records matching the passed query.
     *
     * @param request the query with filtering parameters for the event history
     * @param responseObserver observer for the resulting stream
     */
    public void read(final EventStreamQuery request, final StreamObserver<EventRecord> responseObserver) {
        logReadingStart(request, responseObserver);

        streamExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Iterator<EventRecord> eventRecords = iterator(request);
                while (eventRecords.hasNext()) {
                    final EventRecord record = eventRecords.next();
                    responseObserver.onNext(record);
                }
                responseObserver.onCompleted();
                logCatchUpComplete(responseObserver);
            }
        });
    }

    /**
     * Abstract builder base for building
     * @param <T> the type of the builder product
     */
    private abstract static class AbstractBuilder<T> {

        private Executor streamExecutor;
        private EventStorage eventStorage;
        @Nullable
        private Logger logger;

        public abstract T build();

        /**
         * This method must be called in {@link #build()} implementations to
         * verify that all required parameters are set.
         */
        protected void checkState() {
            checkNotNull(getStreamExecutor(), "streamExecutor must be set");
            checkNotNull(getEventStorage(), "eventStorage must be set");
        }

        protected AbstractBuilder setStreamExecutor(Executor executor) {
            this.streamExecutor = checkNotNull(executor);
            return this;
        }

        public Executor getStreamExecutor() {
            return streamExecutor;
        }

        protected AbstractBuilder setStorage(EventStorage eventStorage) {
            this.eventStorage = checkNotNull(eventStorage);
            return this;
        }

        public EventStorage getEventStorage() {
            return eventStorage;
        }

        protected AbstractBuilder setLogger(@Nullable Logger logger) {
            this.logger = logger;
            return this;
        }

        @Nullable
        public Logger getLogger() {
            return logger;
        }

        /**
         * Sets default logger.
         * @see EventStore#log()
         */
        public AbstractBuilder withDefaultLogger() {
            setLogger(log());
            return this;
        }
    }

    /**
     * Builder for creating new local {@code EventStore} instance.
     */
    public static class Builder extends AbstractBuilder<EventStore> {

        @Override
        public EventStore build() {
            checkState();
            final LocalImpl result = new LocalImpl(getStreamExecutor(), getEventStorage(), getLogger());
            return result;
        }

        @Override
        public Builder setStreamExecutor(Executor executor) {
            super.setStreamExecutor(executor);
            return this;
        }

        @Override
        public Builder setStorage(EventStorage eventStorage) {
            super.setStorage(eventStorage);
            return this;
        }

        @Override
        public Builder setLogger(@Nullable Logger logger) {
            super.setLogger(logger);
            return this;
        }

        @Override
        public AbstractBuilder withDefaultLogger() {
            super.withDefaultLogger();
            return this;
        }
    }

    /**
     * A locally running {@code EventStore} implementation.
     */
    private static class LocalImpl extends EventStore {

        private final EventStorage storage;

        private LocalImpl(Executor catchUpExecutor, EventStorage storage, @Nullable Logger logger) {
            super(catchUpExecutor, logger);
            this.storage = storage;
        }

        @Override
        protected void store(EventRecord record) {
            storage.write(record);
        }

        @Override
        protected Iterator<EventRecord> iterator(EventStreamQuery query) {
            return storage.iterator(query);
        }

        /**
         * Notifies all the subscribers and closes the underlying storage.
         *
         * @throws IOException if the attempt to close the storage throws an exception
         */
        @Override
        public void close() throws Exception {
            storage.close();
        }
    }

    /**
     * The builder of {@code EventStore} instance exposed as gRPC service.
     *
     * @see EventStoreGrpc.EventStore
     */
    public static class ServiceBuilder extends AbstractBuilder<ServerServiceDefinition> {

        @Override
        public ServerServiceDefinition build() {
            checkState();
            final LocalImpl eventStore = new LocalImpl(getStreamExecutor(), getEventStorage(), getLogger());
            final EventStoreGrpc.EventStore grpcService = new GrpcService(eventStore);
            final ServerServiceDefinition result = EventStoreGrpc.bindService(grpcService);
            return result;
        }

        @Override
        public ServiceBuilder setStreamExecutor(Executor executor) {
            super.setStreamExecutor(executor);
            return this;
        }

        @Override
        public ServiceBuilder setStorage(EventStorage eventStorage) {
            super.setStorage(eventStorage);
            return this;
        }

        @Override
        public ServiceBuilder setLogger(@Nullable Logger logger) {
            super.setLogger(logger);
            return this;
        }

        @Override
        public ServiceBuilder withDefaultLogger() {
            super.withDefaultLogger();
            return this;
        }
    }

    /**
     * gRPC service over the locally running implementation.
     */
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

        @Override
        public void read(EventStreamQuery request, StreamObserver<EventRecord> responseObserver) {
            eventStore.read(request, responseObserver);
        }
    }

    //
    // Logging methods
    //------------------------------------------

    private void logStored(EventRecord request) {
        if (logger == null) {
            return;
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Stored: {}", TextFormat.shortDebugString(request));
        }
    }

    private void logReadingStart(EventStreamQuery request, StreamObserver<EventRecord> responseObserver) {
        if (logger == null) {
            return;
        }

        if (logger.isInfoEnabled()) {
            final String requestData = TextFormat.shortDebugString(request);
            logger.info("Creating stream on request: {} for observer: {}", requestData, responseObserver);
        }
    }

    private void logCatchUpComplete(StreamObserver<EventRecord> observer) {
        if (logger == null) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Observer {} got all catch-up events.", observer);
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EventStore.class);
    }

    /**
     * @return default logger of {EventStore} class.
     */
    public static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
