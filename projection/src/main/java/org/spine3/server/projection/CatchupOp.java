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

import com.google.protobuf.Message;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.protobuf.ProtoCoder;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.spine3.base.Event;
import org.spine3.envelope.EventEnvelope;
import org.spine3.server.entity.idfunc.EventTargetsFunction;
import org.spine3.server.event.EventFilter;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.users.TenantId;

import java.util.Set;

/**
 * The operation of catching up projections with the specified state class.
 *
 * @param <I> the type of projection identifiers
 * @author Alexander Yevsyukov
 */
public class CatchupOp<I> {

    private final ProjectionRepository<I, ?, ?> repository;
    private final EventStore eventStore;
    private final Set<EventFilter> eventFilters;
    private final PipelineOptions options;

    public CatchupOp(ProjectionRepository<I, ?, ?> repository,
                     PipelineOptions options) {
        this.repository = repository;
        this.options = options;

        this.eventStore = repository.getEventStore();
        this.eventFilters = repository.getEventFilters();

    }

    private Pipeline createPipeline() {
        Pipeline pipeline = Pipeline.create(options);

        // Get tenant IDs.
        final PCollection<TenantId> allTenants = pipeline.apply(
                "ReadAllTenantIdentifiers", readAllTenantIdentifiers());

        // Compose Event Stream Query
        final EventStreamQuery query = repository.createStreamQuery();

        // Read events matching the query for each tenants.
        final PCollection<PCollection<Event>> eventsByTenants =
                allTenants.apply("ReadEvents", ParDo.of(new ReadEvents(query)));

        // Group events by projections.
        final GetProjectionIdentifiers<I> getIdentifiers =
                new GetProjectionIdentifiers<>(repository.getIdSetFunction());


        // Apply events to projections and store them.

        return pipeline;
    }

    private Create.Values<TenantId> readAllTenantIdentifiers() {
        Set<TenantId> tenants = repository.getBoundedContext()
                                          .getTenantIndex()
                                          .getAll();
        final Create.Values<TenantId> values = Create.of(tenants)
                                                     .withCoder(ProtoCoder.of(TenantId.class));
        return values;
    }

    public PipelineResult run() {
        final Pipeline pipeline = createPipeline();
        final PipelineResult result = pipeline.run();
        return result;
    }

    private static class ReadEvents extends DoFn<TenantId, PCollection<Event>> {

        private static final long serialVersionUID = 1L;

        private final EventStreamQuery query;

        private ReadEvents(EventStreamQuery query) {
            this.query = query;
        }

        @ProcessElement
        public void processElement(ProcessContext c) {
            final TenantId tenantId = c.element();

            //TODO:2017-05-12:alexander.yevsyukov: Read events in a tenant operation.
            // Get record storage from EventStore as a constructor parameter.

            final PCollection<Event> events = null;

            c.output(events);
        }
    }

    private static class GetProjectionIdentifiers<I>
            extends DoFn<Event, KV<Event, PCollection<I>>> {

        private static final long serialVersionUID = 1L;
        private final EventTargetsFunction<I, Message> function;

        private GetProjectionIdentifiers(EventTargetsFunction<I, Message> function) {
            this.function = function;
        }

        @ProcessElement
        public void processElement(ProcessContext c) {
            final EventEnvelope event = EventEnvelope.of(c.element());
            final Set<I> idSet = function.apply(event.getMessage(), event.getEventContext());
        }
    }
}
