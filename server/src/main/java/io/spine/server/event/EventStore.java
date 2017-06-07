/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package io.spine.server.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.TextFormat;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import io.spine.base.Event;
import io.spine.base.Response;
import io.spine.base.Responses;
import io.spine.server.event.grpc.EventStoreGrpc;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.EventOperation;
import io.spine.server.tenant.TenantAwareOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
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
     * @param logger         debug logger instance
     */
    protected EventStore(Executor streamExecutor, @Nullable Logger logger) {
        super();
        this.streamExecutor = streamExecutor;
        this.logger = logger;
    }

    /**
     * Appends the passed event to the history of events.
     *
     * @param event the record to append
     */
    public void append(final Event event) {
        final TenantAwareOperation op = new EventOperation(event) {
            @Override
            public void run() {
                store(event);
            }
        };
        op.execute();

        logStored(event);
    }

    /**
     * Implement this method for storing the passed event.
     *
     * @param event the event record to store.
     */
    protected abstract void store(Event event);

    /**
     * Creates iterator for traversing through the history of events matching the passed query.
     *
     * @param query the query filtering the history
     * @return iterator instance
     */
    protected abstract Iterator<Event> iterator(EventStreamQuery query);

    /**
     * Creates the steam with events matching the passed query.
     *
     * @param request the query with filtering parameters for the event history
     * @param responseObserver observer for the resulting stream
     */
    public void read(final EventStreamQuery request, final StreamObserver<Event> responseObserver) {
        logReadingStart(request, responseObserver);

        streamExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Iterator<Event> eventRecords = iterator(request);
                while (eventRecords.hasNext()) {
                    final Event event = eventRecords.next();
                    responseObserver.onNext(event);
                }
                responseObserver.onCompleted();
                logReadingComplete(responseObserver);
            }
        });
    }

    @VisibleForTesting
    Executor getStreamExecutor() {
        return streamExecutor;
    }

    /**
     * Abstract builder base for building.
     *
     * @param <T> the type of the builder product
     * @param <B> the type of the builder for covariance in derived classes
     */
    @SuppressWarnings("unchecked") // The cast is safe
    private abstract static class AbstractBuilder<T, B extends AbstractBuilder<T, B>> {

        private Executor streamExecutor;
        private StorageFactory storageFactory;
        @Nullable
        private Logger logger;

        public abstract T build();

        /**
         * This method must be called in {@link #build()} implementations to
         * verify that all required parameters are set.
         */
        protected void checkState() {
            checkNotNull(getStreamExecutor(), "streamExecutor must be set");
            checkNotNull(getStorageFactory(), "eventStorage must be set");
        }

        public B setStreamExecutor(Executor executor) {
            this.streamExecutor = checkNotNull(executor);
            return (B)this;
        }

        public Executor getStreamExecutor() {
            return streamExecutor;
        }

        public B setStorageFactory(StorageFactory storageFactory) {
            this.storageFactory = checkNotNull(storageFactory);
            return (B)this;
        }

        public StorageFactory getStorageFactory() {
            return storageFactory;
        }

        public B setLogger(@Nullable Logger logger) {
            this.logger = logger;
            return (B)this;
        }

        @Nullable
        public Logger getLogger() {
            return logger;
        }

        /**
         * Sets default logger.
         *
         * @see EventStore#log()
         */
        public B withDefaultLogger() {
            setLogger(log());
            return (B)this;
        }
    }

    /**
     * Builder for creating new local {@code EventStore} instance.
     */
    public static class Builder extends AbstractBuilder<EventStore, Builder> {

        @Override
        public EventStore build() {
            checkState();
            final LocalEventStore result =
                    new LocalEventStore(getStreamExecutor(), getStorageFactory(), getLogger());
            return result;
        }
    }

    /**
     * The builder of {@code EventStore} instance exposed as gRPC service.
     *
     * @see EventStoreGrpc.EventStoreImplBase
     */
    public static class ServiceBuilder
            extends AbstractBuilder<ServerServiceDefinition, ServiceBuilder> {

        @Override
        public ServerServiceDefinition build() {
            checkState();
            final LocalEventStore eventStore =
                    new LocalEventStore(getStreamExecutor(), getStorageFactory(), getLogger());
            final EventStoreGrpc.EventStoreImplBase grpcService = new GrpcService(eventStore);
            final ServerServiceDefinition result = grpcService.bindService();
            return result;
        }
    }

    /**
     * gRPC service over the locally running implementation.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
        /* as we override default implementation with `unimplemented` status. */
    private static class GrpcService extends EventStoreGrpc.EventStoreImplBase {

        private final LocalEventStore eventStore;

        private GrpcService(LocalEventStore eventStore) {
            super();
            this.eventStore = eventStore;
        }

        @Override
        public void append(Event request, StreamObserver<Response> responseObserver) {
            try {
                eventStore.append(request);
                responseObserver.onNext(Responses.ok());
                responseObserver.onCompleted();
            } catch (RuntimeException e) {
                responseObserver.onError(e);
            }
        }

        @Override
        public void read(EventStreamQuery request, StreamObserver<Event> responseObserver) {
            eventStore.read(request, responseObserver);
        }
    }

    /*
     * Logging methods
     */

    private void logStored(Event request) {
        if (logger == null) {
            return;
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Stored: {}", TextFormat.shortDebugString(request));
        }
    }

    private void logReadingStart(EventStreamQuery request, StreamObserver<Event> responseObserver) {
        if (logger == null) {
            return;
        }

        if (logger.isInfoEnabled()) {
            final String requestData = TextFormat.shortDebugString(request);
            logger.info("Creating stream on request: {} for observer: {}",
                        requestData,
                        responseObserver);
        }
    }

    private void logReadingComplete(StreamObserver<Event> observer) {
        if (logger == null) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Observer {} got all queried events.", observer);
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EventStore.class);
    }

    /** Returns default logger for the class. */
    public static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
