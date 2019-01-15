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
package io.spine.server.event.store;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.TextFormat;
import io.grpc.stub.StreamObserver;
import io.spine.core.Event;
import io.spine.core.Events;
import io.spine.core.TenantId;
import io.spine.logging.Logging;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.EventOperation;
import io.spine.server.tenant.TenantAwareOperation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toSet;

/**
 * A store of all events in a bounded context.
 */
public final class EventStore implements AutoCloseable {

    private static final String TENANT_MISMATCH_ERROR_MSG =
            "Events, that target different tenants, cannot be stored in a single operation. " +
                    System.lineSeparator() +
                    "Observed tenants are: %s";

    private final ERepository storage;
    private final Executor streamExecutor;

    private final @Nullable Logger logger;

    /**
     * Creates a builder for locally running {@code EventStore}.
     *
     * @return new builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    private static void ensureSameTenant(Iterable<Event> events) {
        checkNotNull(events);
        Set<TenantId> tenants = Streams.stream(events)
                                       .map(Events::getTenantId)
                                       .collect(toSet());
        checkArgument(tenants.size() == 1, TENANT_MISMATCH_ERROR_MSG, tenants);
    }

    /**
     * Constructs an instance with the passed executor for returning streams.
     *
     * @param streamExecutor the executor for updating new subscribers
     * @param storageFactory the storage factory for creating underlying storage
     * @param logger         debug logger instance
     */
    private EventStore(Executor streamExecutor,
                       StorageFactory storageFactory,
                       @Nullable Logger logger) {
        super();
        ERepository eventRepository = new ERepository();
        eventRepository.initStorage(storageFactory);
        this.storage = eventRepository;
        this.streamExecutor = streamExecutor;
        this.logger = logger;
    }

    /**
     * Appends the passed event to the history of events.
     *
     * @param event the record to append
     */
    public void append(Event event) {
        checkNotNull(event);
        TenantAwareOperation op = new EventOperation(event) {
            @Override
            public void run() {
                storage.store(event);
            }
        };
        op.execute();

        logStored(event);
    }

    /**
     * Appends the passed events to the history of events.
     *
     * <p>If the passed {@link Iterable} is empty, no action is performed.
     *
     * <p>If the passed {@linkplain Event Events} belong to the different
     * {@linkplain TenantId tenants}, an {@link IllegalArgumentException} is thrown.
     *
     * @param events the events to append
     */
    public void appendAll(Iterable<Event> events) {
        checkNotNull(events);
        Optional<Event> tenantDefiningEvent = Streams.stream(events)
                                                     .filter(Objects::nonNull)
                                                     .findFirst();
        if (!tenantDefiningEvent.isPresent()) {
            return;
        }
        Event event = tenantDefiningEvent.get();
        TenantAwareOperation op = new EventOperation(event) {
            @Override
            public void run() {
                if (isTenantSet()) { // If multitenant context
                    ensureSameTenant(events);
                }
                storage.store(events);
            }
        };
        op.execute();

        logStored(events);
    }

    /**
     * Creates the stream with events matching the passed query.
     *
     * @param request          the query with filtering parameters for the event history
     * @param responseObserver observer for the resulting stream
     */
    public void read(EventStreamQuery request, StreamObserver<Event> responseObserver) {
        checkNotNull(request);
        checkNotNull(responseObserver);

        logReadingStart(request, responseObserver);

        streamExecutor.execute(() -> {
            Iterator<Event> eventRecords = storage.iterator(request);
            while (eventRecords.hasNext()) {
                Event event = eventRecords.next();
                responseObserver.onNext(event);
            }
            responseObserver.onCompleted();
            logReadingComplete(responseObserver);
        });
    }

    /**
     * Obtains stream executor used by the store.
     */
    @VisibleForTesting
    public Executor getStreamExecutor() {
        return streamExecutor;
    }

    /**
     * Closes the underlying storage.
     */
    @Override
    public void close() {
        storage.close();
    }

    /**
     * Tells if the store is open.
     */
    public boolean isOpen() {
        return storage.isOpen();
    }

    /**
     * Builder for creating new {@code EventStore} instance.
     */
    public static final class Builder {

        private Executor streamExecutor;
        private StorageFactory storageFactory;
        private @Nullable Logger logger;

        /** Prevents instantiation from outside. */
        private Builder() {
        }

        /**
         * This method must be called in {@link #build()} implementations to
         * verify that all required parameters are set.
         */
        private void checkState() {
            checkNotNull(getStreamExecutor(), "streamExecutor must be set");
            checkNotNull(getStorageFactory(), "eventStorage must be set");
        }

        public Executor getStreamExecutor() {
            return streamExecutor;
        }

        @CanIgnoreReturnValue
        public Builder setStreamExecutor(Executor executor) {
            this.streamExecutor = checkNotNull(executor);
            return this;
        }

        public StorageFactory getStorageFactory() {
            return storageFactory;
        }

        @CanIgnoreReturnValue
        public Builder setStorageFactory(StorageFactory storageFactory) {
            this.storageFactory = checkNotNull(storageFactory);
            return this;
        }

        public @Nullable Logger getLogger() {
            return logger;
        }

        @CanIgnoreReturnValue
        public Builder setLogger(@Nullable Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Sets default logger.
         *
         * @see EventStore#log()
         */
        @CanIgnoreReturnValue
        public Builder withDefaultLogger() {
            setLogger(log());
            return this;
        }

        /**
         * Creates new {@code EventStore} instance.
         */
        public EventStore build() {
            checkState();
            EventStore result =
                    new EventStore(getStreamExecutor(), getStorageFactory(), getLogger());
            return result;
        }
    }

    /*
     * Logging methods
     *******************/

    private void logStored(Event request) {
        if (logger == null) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Stored: {}", TextFormat.shortDebugString(request));
        }
    }

    private void logStored(Iterable<Event> events) {
        for (Event event : events) {
            logStored(event);
        }
    }

    private void logReadingStart(EventStreamQuery request, StreamObserver<Event> responseObserver) {
        if (logger == null) {
            return;
        }

        if (logger.isDebugEnabled()) {
            String requestData = TextFormat.shortDebugString(request);
            logger.debug("Creating stream on request: {} for observer: {}",
                         requestData,
                         responseObserver);
        }
    }

    private void logReadingComplete(StreamObserver<Event> observer) {
        if (logger == null) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Observer {} got all queried events.", observer);
        }
    }

    /** Returns default logger for this class. */
    public static Logger log() {
        return Logging.get(EventStore.class);
    }
}
