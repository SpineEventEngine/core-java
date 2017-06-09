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

import com.google.common.base.Predicate;
import io.spine.annotation.Internal;
import io.spine.annotation.SPI;
import io.spine.base.Event;
import io.spine.base.EventId;
import io.spine.client.EntityFilters;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.Repository;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.RecordStorageIO;
import io.spine.users.TenantId;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.extensions.protobuf.ProtoCoder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.storage.RecordStorageIO.toInstant;

/**
 * Beam support for I/O operations of {@link EventStore}.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public class EventStoreIO {

    private static final Coder<Event> eventCoder = ProtoCoder.of(Event.class);

    private static final DoFn<EventStreamQuery, EntityFilters> createEntityFilters =
            new DoFn<EventStreamQuery, EntityFilters>() {
                private static final long serialVersionUID = 0L;

                @ProcessElement
                public void processElement(ProcessContext c) {
                    final EventStreamQuery query = c.element();
                    final EntityFilters filters = ERepository.toEntityFilters(query);
                    c.output(filters);
                }
            };

    private static final DoFn<EntityRecord, Event> extractEvents = new DoFn<EntityRecord, Event>() {
        private static final long serialVersionUID = 0L;

        @ProcessElement
        public void processElement(ProcessContext c) {
            final EntityRecord entityRecord = c.element();
            final Event event = AnyPacker.unpack(entityRecord.getState());
            c.outputWithTimestamp(event, toInstant(event.getContext()
                                                        .getTimestamp()));
        }
    };

    private EventStoreIO() {
        // Prevent instantiation of this utility class.
    }

    public static Coder<Event> getEventCoder() {
        return eventCoder;
    }

    @Internal
    public static Repository eventStorageOf(BoundedContext boundedContext) {
        checkNotNull(boundedContext);
        return boundedContext.getEventBus()
                             .getEventStore()
                             .getStorage();
    }

    public static Query query(TenantId tenantId, EventStore eventStore) {
        checkNotNull(tenantId);
        checkNotNull(eventStore);
        final ERepository repository = eventStore.getStorage();
        final RecordStorage<EventId> storage = repository.recordStorage();
        final RecordStorageIO.FindFn findFn = storage.getIO(EventId.class)
                                                     .findFn(tenantId);
        return new Query(findFn);
    }

    /**
     * Reads events matching an {@link EventStreamQuery}.
     */
    private static class Query
            extends PTransform<PCollection<EventStreamQuery>, PCollection<Event>> {

        private static final long serialVersionUID = 0L;
        private final DoFn<EntityFilters, EntityRecord> findFn;

        private Query(DoFn<EntityFilters, EntityRecord> findFn) {
            this.findFn = findFn;
        }

        public static Query of(DoFn<EntityFilters, EntityRecord> findFn) {
            checkNotNull(findFn);
            final Query result = new Query(findFn);
            return result;
        }

        @Override
        public PCollection<Event> expand(PCollection<EventStreamQuery> input) {
            final PCollection<EntityFilters> filters =
                    input.apply("CreateEntityFilters", ParDo.of(createEntityFilters));
            final PCollection<EntityRecord> entityRecords =
                    filters.apply("FindRecords", ParDo.of(findFn));
            final PCollection<Event> events =
                    entityRecords.apply("ExtractEvents", ParDo.of(extractEvents));
            final PCollectionView<EventStreamQuery> queryView =
                    input.apply("GetEventStreamQuery", View.<EventStreamQuery>asSingleton());
            final PCollection<Event> matchingQuery =
                    events.apply("FilterByEventStreamQuery", ParDo.of(new DoFn<Event, Event>() {
                        private static final long serialVersionUID = 0L;

                        @ProcessElement
                        public void processElement(ProcessContext c) {
                            final Event event = c.element();
                            final EventStreamQuery query = c.sideInput(queryView);
                            final Predicate<Event> filter = new MatchesStreamQuery(query);
                            if (filter.apply(event)) {
                                c.outputWithTimestamp(event, toInstant(event.getContext()
                                                                            .getTimestamp()));
                            }
                        }
                    })
                                                                  .withSideInputs(queryView));
            return matchingQuery;
        }
    }
}
