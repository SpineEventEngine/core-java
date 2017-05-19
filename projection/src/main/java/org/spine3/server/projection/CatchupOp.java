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

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.FlatMapElements;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.Max;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;
import org.spine3.base.Event;
import org.spine3.base.Events;
import org.spine3.envelope.EventEnvelope;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.RecordBasedRepository.BeamIO.ReadFunction;
import org.spine3.server.entity.idfunc.EventTargetsFunction;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.users.TenantId;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableList.builder;

/**
 * The operation of catching up projections with the specified state class.
 *
 * @param <I> the type of projection identifiers
 * @author Alexander Yevsyukov
 */
public class CatchupOp<I> {

    private static final TimestampComparator timestampComparator = new TimestampComparator();
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

        // Read events matching the query.
        final EventStreamQuery query = repository.createStreamQuery();
        final PCollection<Event> events =
                pipeline.apply("QueryEvents",
                               eventStore.query(tenantId, query));

        // Compose a flat map where a key is a projection ID and a value is an event to apply.
        final PCollection<KV<I, Event>> flatMap =
                events.apply("MapIdsToEvents",
                             FlatMapElements.via(new GetIds<>(repository.getIdSetFunction())));

        // Group events by projection IDs.
        final PCollection<KV<I, Iterable<Event>>> groupped =
                flatMap.apply("GroupEvents", GroupByKey.<I, Event>create());

        // Apply events to projections.
        // 1. Load projection, apply events.
        // 2. Take as a side output the timestamp of the last event.
        final TupleTag<KV<I, EntityRecord>> recordsTag = new TupleTag<KV<I, EntityRecord>>(){};
        final TupleTag<Timestamp> timestampTag = new TupleTag<Timestamp>(){};

        final ProjectionRepository.BeamIO<I, ?, ?> repositoryIO = repository.getIO();
        final PCollectionTuple collectionTuple =
                groupped.apply("ApplyEvents",
                               ParDo.of(new ApplyEvents<>(repositoryIO.loadOrCreate(tenantId),
                                                          timestampTag))
                                    .withOutputTags(recordsTag, TupleTagList.of(timestampTag)));

        // Store projections.
        final PCollection<KV<I, EntityRecord>> records = collectionTuple.get(recordsTag);
        records.apply("WriteRecords", repositoryIO.write(tenantId));

        // Sort last timestamps of last events and write last handled event time.
        final PCollection<Timestamp> timestamps = collectionTuple.get(timestampTag);
        final PCollection<Timestamp> lastTimestamp = timestamps.apply("MaxTimestamp",
                Max.<Timestamp, TimestampComparator>globally(timestampComparator));
        lastTimestamp.apply(repositoryIO.writeLastHandledEventTime(tenantId));

        return pipeline;
    }

    public PipelineResult run() {
        final Pipeline pipeline = createPipeline();
        final PipelineResult result = pipeline.run();
        return result;
    }

    /** Obtains projection IDs for an event via supplied function. */
    private static class GetIds<I> extends SimpleFunction<Event, Iterable<KV<I, Event>>> {

        private static final long serialVersionUID = 0L;
        private final EventTargetsFunction<I, Message> fn;

        private GetIds(EventTargetsFunction<I, Message> fn) {
            this.fn = fn;
        }

        @Override
        public Iterable<KV<I, Event>> apply(Event input) {
            final EventEnvelope event = EventEnvelope.of(input);
            final Set<I> idSet = fn.apply(event.getMessage(), event.getEventContext());
            final ImmutableList.Builder<KV<I, Event>> builder = builder();
            for (I id : idSet) {
                builder.add(KV.of(id, input));
            }
            return builder.build();
        }
    }

    /**
     * Applies events to a projection and emits a timestamp of last event as a side output.
     */
    private static class ApplyEvents<I> extends DoFn<KV<I, Iterable<Event>>, KV<I, EntityRecord>> {

        private static final long serialVersionUID = 0L;
        private final ReadFunction<I, ?, ?> loadOrCreate;
        private final TupleTag<Timestamp> timestampTag;

        private ApplyEvents(ReadFunction<I, ?, ?> loadOrCreate, TupleTag<Timestamp> timestampTag) {
            this.loadOrCreate = loadOrCreate;
            this.timestampTag = timestampTag;
        }

        @ProcessElement
        public void processElement(ProcessContext c) {
            final I id = c.element().getKey();
            final List<Event> events = Lists.newArrayList(c.element().getValue());
            Collections.sort(events, Events.eventComparator());

            // Timestamps of all applied events.
            final List<Timestamp> timestamps = Lists.newArrayList();

            @SuppressWarnings("unchecked")
                // the types are preserved since the the function is returned by a projection repo.
            final Projection<I, ?> projection = (Projection<I, ?>) loadOrCreate.apply(id);

            // Apply events
            for (Event event : events) {
                projection.handle(Events.getMessage(event), event.getContext());
                timestamps.add(event.getContext().getTimestamp());
            }

            // Add the resulting record to output.
            @SuppressWarnings("unchecked")
            // the types are preserved since the the function is returned by a projection repo.
            final EntityRecord record =
                    ((Converter<? super Projection<I, ?>, EntityRecord>)loadOrCreate.getConverter())
                            .convert(projection);
            c.output(KV.of(id, record));

            // Get the latest event timestamp.
            Collections.sort(timestamps, Timestamps.comparator());
            final Timestamp lastEventTimestamp = timestamps.get(timestamps.size() - 1);

            c.sideOutput(timestampTag, lastEventTimestamp);
        }
    }

    private static class TimestampComparator implements Comparator<Timestamp>, Serializable {

        private static final long serialVersionUID = 0L;

        @Override
        public int compare(Timestamp t1, Timestamp t2) {
            return Timestamps.comparator().compare(t1, t2);
        }

        @Override
        public String toString() {
            return "TimestampComparator";
        }
    }
}
