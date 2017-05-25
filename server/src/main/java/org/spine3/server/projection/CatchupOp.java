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

package org.spine3.server.projection;

import com.google.protobuf.Timestamp;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.CoderRegistry;
import org.apache.beam.sdk.coders.IterableCoder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.FlatMapElements;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.Keys;
import org.apache.beam.sdk.transforms.Max;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.spine3.base.Event;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.EventStoreIO;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.server.tenant.TenantAwareFunction0;
import org.spine3.users.TenantId;

/**
 * Performs of a catch-up for the repository for one tenant.
 *
 * @param <I> the type of projection identifiers
 * @author Alexander Yevsyukov
 */
class CatchupOp<I> {

    private final TenantId tenantId;
    private final ProjectionRepository<I, ?, ?> repository;
    private final EventStore eventStore;
    private final PipelineOptions options;

    public CatchupOp(TenantId tenantId,
                     ProjectionRepository<I, ?, ?> repository,
                     PipelineOptions options) {
        this.tenantId = tenantId;
        this.repository = repository;
        this.options = options;

        this.eventStore = repository.getEventStore();
    }

    @SuppressWarnings({"SerializableInnerClassWithNonSerializableOuterClass"
                               /* OK as no dependencies to the outer class in the body. */,
            "serial" /* OK to rely on default Java mechanism for the tags. */})
    private Pipeline createPipeline() {
        Pipeline pipeline = Pipeline.create(options);

        final Coder<I> idCoder = repository.getIO()
                                           .getIdCoder();
        final Coder<Event> eventCoder = EventStoreIO.getEventCoder();
        final KvCoder<I, Event> eventTupleCoder = KvCoder.of(idCoder, eventCoder);

        final CoderRegistry coderRegistry = pipeline.getCoderRegistry();
        coderRegistry.registerCoderForType(
                new TypeDescriptor<KV<I, Event>>() {},
                eventTupleCoder);

        // Read events matching the query.
        final EventStreamQuery query = createStreamQuery();
        final PCollection<Event> events = pipeline.apply(
                "QueryEvents",
                eventStore.query(tenantId, query)
        );

        // Compose a flat map where a key is a projection ID and a value is an event to apply.
        final PCollection<KV<I, Event>> flatMap = events.apply(
                "MapIdsToEvents",
                FlatMapElements.via(new GetIds<>(repository.getIdSetFunction()))
        ).setCoder(eventTupleCoder);

        // Group events by projection IDs.
        final KvCoder<I, Iterable<Event>> idToEventsCoder =
                KvCoder.of(idCoder, IterableCoder.of(eventCoder));

        final PCollection<KV<I, Iterable<Event>>> groupped =
                flatMap.apply("GroupEvents", GroupByKey.<I, Event>create())
                       .setCoder(idToEventsCoder);

        final PCollection<I> ids = groupped.apply(Keys.<I>create());

        //TODO:2017-05-25:alexander.yevsyukov: Load or create all the entities and get their records.
        // Pass them as side input to `ApplyEvents`.

        // Apply events to projections.
        // 1. Load projection, apply events.
        // 2. Take as a side output the timestamp of the last event.
        final TupleTag<KV<I, EntityRecord>> recordsTag = new TupleTag<KV<I, EntityRecord>>() {
        };
        final TupleTag<Timestamp> timestampTag = new ApplyEvents.TimestampTupleTag();

        final ProjectionRepositoryIO<I, ?, ?> repositoryIO = repository.getIO();
        final PCollectionTuple collectionTuple = groupped.apply(
                "ApplyEvents",
                ParDo.of(new ApplyEvents<>(repositoryIO.loadOrCreate(tenantId), timestampTag))
                     .withOutputTags(recordsTag, TupleTagList.of(timestampTag))
        );

        // Store projections.
        final PCollection<KV<I, EntityRecord>> records = collectionTuple.get(recordsTag);
        records.apply("WriteRecords", repositoryIO.write(tenantId));

        // Sort last timestamps of last events and write last handled event time.
        final PCollection<Timestamp> timestamps = collectionTuple.get(timestampTag);
        final PCollection<Timestamp> lastTimestamp = timestamps.apply(
                "MaxTimestamp",
                Max.<Timestamp, TimestampComparator>globally(TimestampComparator.getInstance())
        );
        lastTimestamp.apply(repositoryIO.writeLastHandledEventTime(tenantId));

        return pipeline;
    }

    private EventStreamQuery createStreamQuery() {
        final TenantAwareFunction0<EventStreamQuery> fn =
                new TenantAwareFunction0<EventStreamQuery>(tenantId) {
            @Override
            public EventStreamQuery apply() {
                final EventStreamQuery result = repository.createStreamQuery();
                return result;
            }
        };
        return fn.execute();
    }

    public PipelineResult run() {
        final Pipeline pipeline = createPipeline();
        final PipelineResult result = pipeline.run();
        return result;
    }
}
