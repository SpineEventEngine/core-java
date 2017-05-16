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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.FlatMapElements;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.Keys;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TupleTag;
import org.spine3.base.Event;
import org.spine3.envelope.EventEnvelope;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.EntityStorageConverter;
import org.spine3.server.entity.idfunc.EventTargetsFunction;
import org.spine3.server.event.EventPredicate;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.users.TenantId;

import java.util.LinkedList;
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

    private Pipeline createPipeline() {
        Pipeline pipeline = Pipeline.create(options);

        // Read events matching the query.
        final EventStreamQuery query = repository.createStreamQuery();
        final EventPredicate.Query predicate = EventPredicate.Query.of(query);
        final PCollection<Event> events =
                pipeline.apply("QueryEvents",
                               eventStore.query(tenantId, predicate));

        // Compose a flat map where a key is a projection ID and a value is an event to apply.
        final PCollection<KV<I, Event>> flatMap =
                events.apply("MapIdsToEvents",
                             FlatMapElements.via(new GetIds<>(repository.getIdSetFunction())));

        // Group events by projection IDs.
        final PCollection<KV<I, Iterable<Event>>> groupped =
                flatMap.apply("GroupEvents", GroupByKey.<I, Event>create());

        // Get IDs for all the projections.
        final PCollection<I> ids = groupped.apply(Keys.<I>create());
        final PCollection<List<I>> idList = ids.apply(Combine.globally(new GatherIds<I>()));

        final PCollection<KV<I, EntityRecord>> recordsById = idList.apply(
                ParDo.of(new DoFn<List<I>, KV<I, EntityRecord>>() {
                    private static final long serialVersionUID = 0L;
                    //TODO:2017-05-16:alexander.yevsyukov: Load map by ID.
                }));

        // Apply events to projections.
        //TODO:2017-05-15:alexander.yevsyukov: Implement.
        // 1. Load projection, apply events.
        // 2. Take as a side output the timestamp of the last event.

        // Store projections.
        //TODO:2017-05-15:alexander.yevsyukov: Implement.

        // Sort last timestamps of last events and #writeLastHandledEventTime().

        return pipeline;
    }

    private static class ReadProjectionRecords<I> extends DoFn<List<I>, KV<I, EntityRecord>> {
        private static final long serialVersionUID = 0L;
        private final TenantId tenantId;

        private ReadProjectionRecords(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        @ProcessElement
        public void processElement(ProcessContext c) {
            final List<I> ids = c.element();

        }

    }

    private static class GatherIds<I> extends Combine.CombineFn<I, GatherIds.Accum<I>, List<I>> {

        private static final long serialVersionUID = 0L;

        @Override
        public Accum<I> createAccumulator() {
            return new Accum<>();
        }

        @Override
        public Accum<I> addInput(Accum<I> accumulator, I input) {
            accumulator.list.add(input);
            return accumulator;
        }

        @Override
        public Accum<I> mergeAccumulators(Iterable<Accum<I>> accumulators) {
            final Accum<I> result = new Accum<>();
            for (Accum<I> accumulator : accumulators) {
                result.list.addAll(accumulator.list);
            }
            return result;
        }

        @Override
        public List<I> extractOutput(Accum<I> accumulator) {
            return accumulator.list;
        }

        static class Accum<I> {
            private final LinkedList<I> list = Lists.newLinkedList();
        }
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

    private static class ApplyEvents<I> extends DoFn<KV<I, Iterable<Event>>, EntityRecord> {

        private final EntityStorageConverter<I, ?, ?> entityConverter;
        private final TupleTag<Timestamp> timestampTag;

        private ApplyEvents(EntityStorageConverter<I, ?, ?> entityConverter,
                            TupleTag<Timestamp> timestampTag) {
            this.entityConverter = entityConverter;
            this.timestampTag = timestampTag;
        }

        @ProcessElement
        public void processElement(ProcessContext c) {
            final I id = c.element().getKey();
            final Iterable<Event> events = c.element().getValue();

            final Projection<I, ?> projection = null; //TODO:2017-05-15:alexander.yevsyukov: Load projection
            // Apply events

        }
    }
}
