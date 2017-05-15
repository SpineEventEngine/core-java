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
import com.google.protobuf.Message;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.FlatMapElements;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.spine3.base.Event;
import org.spine3.envelope.EventEnvelope;
import org.spine3.server.entity.idfunc.EventTargetsFunction;
import org.spine3.server.event.EventPredicate;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.users.TenantId;

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
                events.apply("GetProjectionIds",
                             FlatMapElements.via(new GetIds<>(repository.getIdSetFunction())));

        // Group events by projection IDs.
        final PCollection<KV<I, Iterable<Event>>> groupped =
                flatMap.apply("GroupEvents", GroupByKey.<I, Event>create());

        // Apply events to projections.
        //TODO:2017-05-15:alexander.yevsyukov: Implement.

        // Store projections.
        //TODO:2017-05-15:alexander.yevsyukov: Implement.

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
}
