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
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.protobuf.TextFormat;
import io.grpc.stub.StreamObserver;
import io.spine.base.Event;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.EventOperation;
import io.spine.server.tenant.TenantAwareOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.tryFind;

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

    public void appendAll(final Iterable<Event> events) {
        final Optional<Event> tenantDiningEvent = tryFind(events, Predicates.<Event>notNull());
        if (!tenantDiningEvent.isPresent()) {
            return;
        }
        final Event event = tenantDiningEvent.get();
        final TenantAwareOperation op = new EventOperation(event) {
            @Override
            public void run() {
                store(events);
            }
        };
        op.execute();

        logStored(events);
    }

    /**
     * Implement this method for storing the passed event.
     *
     * @param event the event to store.
     */
    protected abstract void store(Event event);

    /**
     * Implement this method for storing the passed events.
     *
     * @param events the events to store.
     */
    protected abstract void store(Iterable<Event> events);

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
                logCatchUpComplete(responseObserver);
            }
        });
    }

    /**
     * Abstract builder base for building.
     *
     * @param <T> the type of the builder product
     */
    private abstract static class AbstractBuilder<T> {

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

        protected AbstractBuilder setStreamExecutor(Executor executor) {
            this.streamExecutor = checkNotNull(executor);
            return this;
        }

        public Executor getStreamExecutor() {
            return streamExecutor;
        }

        protected AbstractBuilder setStorageFactory(StorageFactory storageFactory) {
            this.storageFactory = checkNotNull(storageFactory);
            return this;
        }

        public StorageFactory getStorageFactory() {
            return storageFactory;
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
         *
         * @see EventStore#log()
         */
        public AbstractBuilder withDefaultLogger() {
            setLogger(log());
            return this;
        }
    }

    /** Builder for creating new local {@code EventStore} instance. */
    public static class Builder extends AbstractBuilder<EventStore> {

        @Override
        public EventStore build() {
            checkState();
            final LocalImpl result = new LocalImpl(getStreamExecutor(),
                                                   getStorageFactory(),
                                                   getLogger());
            return result;
        }

        @Override
        public Builder setStreamExecutor(Executor executor) {
            super.setStreamExecutor(executor);
            return this;
        }

        @Override
        public Builder setStorageFactory(StorageFactory storageFactory) {
            super.setStorageFactory(storageFactory);
            return this;
        }

        @Override
        public Builder setLogger(@Nullable Logger logger) {
            super.setLogger(logger);
            return this;
        }

        @Override
        public Builder withDefaultLogger() {
            super.withDefaultLogger();
            return this;
        }
    }

    @VisibleForTesting
    Executor getStreamExecutor() {
        return streamExecutor;
    }

    /** A locally running {@code EventStore} implementation. */
    private static class LocalImpl extends EventStore {

        private final ERepository storage;

        private LocalImpl(Executor catchUpExecutor,
                          StorageFactory storageFactory,
                          @Nullable Logger logger) {
            super(catchUpExecutor, logger);

            final ERepository eventRepository = new ERepository();
            eventRepository.initStorage(storageFactory);
            this.storage = eventRepository;
        }

        @Override
        protected void store(Event event) {
            storage.store(event);
        }

        @Override
        protected void store(Iterable<Event> events) {
            storage.store(events);
        }

        @Override
        protected Iterator<Event> iterator(EventStreamQuery query) {
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

    //
    // Logging methods
    //------------------------------------------

    private void logStored(Event request) {
        if (logger == null) {
            return;
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Stored: {}", TextFormat.shortDebugString(request));
        }
    }

    private void logStored(Iterable<Event> requests) {
        for (Event event : requests) {
            logStored(event);
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

    private void logCatchUpComplete(StreamObserver<Event> observer) {
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

    /** Returns default logger of {EventStore} class. */
    public static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
