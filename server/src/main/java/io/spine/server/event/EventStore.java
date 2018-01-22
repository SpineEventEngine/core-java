/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.protobuf.TextFormat;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.server.event.grpc.EventStoreGrpc;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.EventOperation;
import io.spine.server.tenant.TenantAwareOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.tryFind;
import static io.spine.core.Events.getTenantId;

/**
 * A store of all events in a bounded context.
 *
 * @author Alexander Yevsyukov
 */
public class EventStore implements AutoCloseable {

    private static final String TENANT_MISMATCH_ERROR_MSG =
            "Events, that target different tenants, cannot be stored in a single operation. " +
                    System.lineSeparator() +
                    "Observed tenants are: %s";

    private final ERepository storage;
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

    private static void ensureSameTenant(Iterable<Event> events) {
        checkNotNull(events);

        final Set<TenantId> tenants =
                FluentIterable.from(events)
                              .transform(new Function<Event, TenantId>() {
                                  @Override
                                  public TenantId apply(
                                          @Nullable Event event) {
                                      checkNotNull(event);
                                      return getTenantId(event);
                                  }
                              })
                              .toSet();
        checkArgument(tenants.size() == 1,
                      TENANT_MISMATCH_ERROR_MSG,
                      tenants);
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
        final ERepository eventRepository = new ERepository();
        eventRepository.initStorage(storageFactory);
        this.storage = eventRepository;
        this.streamExecutor = streamExecutor;
        this.logger = logger;
    }

    ERepository getStorage() {
        return storage;
    }

    /**
     * Appends the passed event to the history of events.
     *
     * @param event the record to append
     */
    public void append(final Event event) {
        checkNotNull(event);
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
     * Appends the passed events to the history of events.
     *
     * <p>If the passed {@link Iterable} is empty, no action is performed.
     *
     * <p>If the passed {@linkplain Event Events} belong to the different
     * {@linkplain TenantId tenants}, an {@link IllegalArgumentException} is thrown.
     *
     * @param events the events to append
     */
    public void appendAll(final Iterable<Event> events) {
        checkNotNull(events);
        final Optional<Event> tenantDefiningEvent = tryFind(events, Predicates.<Event>notNull());
        if (!tenantDefiningEvent.isPresent()) {
            return;
        }
        final Event event = tenantDefiningEvent.get();
        final TenantAwareOperation op = new EventOperation(event) {
            @Override
            public void run() {
                if (isTenantSet()) { // If multitenant context
                    ensureSameTenant(events);
                }
                store(events);
            }
        };
        op.execute();

        logStored(events);
    }

    /**
     * Stores the passed event.
     *
     * @param event the event to store.
     */
    protected void store(Event event) {
        storage.store(event);
    }

    /**
     * Stores the passed events.
     *
     * @param events the events to store.
     */
    protected void store(Iterable<Event> events) {
        storage.store(events);
    }

    /**
     * Creates iterator for traversing through the history of events matching the passed query.
     *
     * @param query the query filtering the history
     * @return iterator instance
     */
    protected Iterator<Event> iterator(EventStreamQuery query) {
        return storage.iterator(query);
    }

    /**
     * Creates the steam with events matching the passed query.
     *
     * @param request          the query with filtering parameters for the event history
     * @param responseObserver observer for the resulting stream
     */
    public void read(final EventStreamQuery request, final StreamObserver<Event> responseObserver) {
        checkNotNull(request);
        checkNotNull(responseObserver);

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
     * Closes the underlying storage.
     *
     * @throws IOException if the attempt to close the storage throws an exception
     */
    @Override
    public void close() throws Exception {
        storage.close();
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EventStore.class);
    }

    /**
     * Abstract builder base for building.
     *
     * @param <T> the type of the builder product
     * @param <B> the type of the builder for covariance in derived classes
     */
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

        public Executor getStreamExecutor() {
            return streamExecutor;
        }

        public B setStreamExecutor(Executor executor) {
            this.streamExecutor = checkNotNull(executor);
            return castThis();
        }

        public StorageFactory getStorageFactory() {
            return storageFactory;
        }

        public B setStorageFactory(StorageFactory storageFactory) {
            this.storageFactory = checkNotNull(storageFactory);
            return castThis();
        }

        @Nullable
        public Logger getLogger() {
            return logger;
        }

        public B setLogger(@Nullable Logger logger) {
            this.logger = logger;
            return castThis();
        }

        /**
         * Sets default logger.
         *
         * @see EventStore#log()
         */
        public B withDefaultLogger() {
            setLogger(log());
            return castThis();
        }

        /** Casts this to generic type to provide type covariance in the derived classes. */
        @SuppressWarnings("unchecked") // See Javadoc
        private B castThis() {
            return (B) this;
        }
    }

    /**
     * Builder for creating new local {@code EventStore} instance.
     */
    public static class Builder extends AbstractBuilder<EventStore, Builder> {

        @Override
        public EventStore build() {
            checkState();
            final EventStore result =
                    new EventStore(getStreamExecutor(), getStorageFactory(), getLogger());
            return result;
        }
    }

    /**
     * The builder of {@code EventStore} instance exposed as gRPC service.
     *
     * @see io.spine.server.event.grpc.EventStoreGrpc.EventStoreImplBase
     *      EventStoreGrpc.EventStoreImplBase
     */
    public static class ServiceBuilder
            extends AbstractBuilder<ServerServiceDefinition, ServiceBuilder> {

        @Override
        public ServerServiceDefinition build() {
            checkState();
            final EventStore eventStore =
                    new EventStore(getStreamExecutor(), getStorageFactory(), getLogger());
            final EventStoreGrpc.EventStoreImplBase grpcService = new GrpcService(eventStore);
            final ServerServiceDefinition result = grpcService.bindService();
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
        if (logger.isTraceEnabled()) {
            logger.trace("Stored: {}", TextFormat.shortDebugString(request));
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

        if (logger.isInfoEnabled()) {
            final String requestData = TextFormat.shortDebugString(request);
            logger.info("Creating stream on request: {} for observer: {}",
                        requestData,
                        responseObserver);
        }
    }

    /** Returns default logger for the class. */
    public static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private void logReadingComplete(StreamObserver<Event> observer) {
        if (logger == null) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Observer {} got all queried events.", observer);
        }
    }
}
