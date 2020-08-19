/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.flogger.FluentLogger;
import com.google.protobuf.TextFormat;
import io.grpc.stub.StreamObserver;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.Signal;
import io.spine.core.TenantId;
import io.spine.logging.Logging;
import io.spine.query.RecordQuery;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.storage.MessageRecordSpec;
import io.spine.server.storage.MessageStorage;
import io.spine.server.storage.RecordWithColumns;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.EventOperation;
import io.spine.server.tenant.TenantAwareOperation;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.flogger.LazyArgs.lazy;
import static io.spine.server.event.EventComparator.chronological;
import static java.util.stream.Collectors.toSet;

/**
 * Default implementation of {@link EventStore}.
 */
public final class DefaultEventStore extends MessageStorage<EventId, Event>
        implements EventStore, Logging {

    private static final String TENANT_MISMATCH_ERROR_MSG =
            "Events, that target different tenants, cannot be stored in a single operation. " +
                    System.lineSeparator() +
                    "Observed tenants are: %s.";

    private final Log log;

    /**
     * Constructs new instance.
     */
    public DefaultEventStore(StorageFactory factory, boolean multitenant) {
        super(factory.createRecordStorage(spec(), multitenant));
        this.log = new Log();
    }

    private static MessageRecordSpec<EventId, Event> spec() {
        MessageRecordSpec<EventId, Event> spec =
                new MessageRecordSpec<>(EventId.class,
                                        Event.class,
                                        Signal::id,
                                        EventColumn.definitions());
        return spec;
    }

    private static void ensureSameTenant(ImmutableList<Event> events) {
        checkNotNull(events);
        Set<TenantId> tenants = events.stream()
                                      .map(Event::tenant)
                                      .collect(toSet());
        checkArgument(tenants.size() == 1, TENANT_MISMATCH_ERROR_MSG, tenants);
    }

    @Override
    public void append(Event event) {
        checkNotNull(event);
        TenantAwareOperation op = new EventOperation(event) {
            @Override
            public void run() {
                store(event);
            }
        };
        op.execute();
        log.stored(event);
    }

    @Override
    public void appendAll(Iterable<Event> events) {
        checkNotNull(events);
        ImmutableList<Event> eventList =
                Streams.stream(events)
                       .filter(Objects::nonNull)
                       .collect(toImmutableList());
        if (eventList.isEmpty()) {
            return;
        }
        Event event = eventList.get(0);
        TenantAwareOperation op = new EventOperation(event) {
            @Override
            public void run() {
                if (isTenantSet()) { // If multitenant context
                    ensureSameTenant(eventList);
                }
                store(eventList);
            }
        };
        op.execute();

        log.stored(events);
    }

    @Override
    public void read(EventStreamQuery request, StreamObserver<Event> responseObserver) {
        checkNotNull(request);
        checkNotNull(responseObserver);

        log.readingStart(request, responseObserver);

        Iterator<Event> eventRecords = iterator(request);
        while (eventRecords.hasNext()) {
            Event event = eventRecords.next();
            responseObserver.onNext(event);
        }
        responseObserver.onCompleted();

        log.readingComplete(responseObserver);
    }

    /**
     * Obtains an iterator over events matching the passed query.
     * The iteration is chronologically sorted.
     */
    private Iterator<Event> iterator(EventStreamQuery query) {
        checkNotNull(query);
        Iterator<Event> iterator = find(query);
        ImmutableList<Event> entities = ImmutableList.copyOf(iterator);
        Predicate<Event> predicate = new MatchesStreamQuery(query);
        Iterator<Event> result = entities
                .stream()
                .filter(predicate)
                .sorted(chronological())
                .iterator();
        return result;
    }

    /**
     * Obtains iteration over entities matching the passed query.
     */
    private Iterator<Event> find(EventStreamQuery query) {
        RecordQuery<EventId, Event> converted = Queries.convert(query);
        return readAll(converted);
    }

    private void store(Event event) {
        Event toStore = event.clearEnrichments();
        write(toStore.getId(), toStore);
    }

    private void store(Iterable<Event> events) {
        ImmutableList<RecordWithColumns<EventId, Event>> records =
                Streams.stream(events)
                       .map(Event::clearEnrichments)
                       .map((e) -> RecordWithColumns.create(e.getId(), e, recordSpec()))
                       .collect(toImmutableList());
        writeAll(records);
    }

    /**
     * Logging for operations of {@link DefaultEventStore}.
     */
    final class Log {

        private final FluentLogger.Api debug = logger().atFine();
        private final boolean debugEnabled = debug.isEnabled();

        private void stored(Event event) {
            debug.log("Stored: %s.", lazy(() -> TextFormat.shortDebugString(event)));
        }

        private void stored(Iterable<Event> events) {
            if (debugEnabled) {
                for (Event event : events) {
                    stored(event);
                }
            }
        }

        private void readingStart(EventStreamQuery query, StreamObserver<Event> observer) {
            debug.log("Creating stream on request: `%s` for observer: `%s`.",
                      lazy(() -> TextFormat.shortDebugString(query)),
                      observer);
        }

        private void readingComplete(StreamObserver<Event> observer) {
            debug.log("Observer `%s` got all queried events.", observer);
        }
    }
}
