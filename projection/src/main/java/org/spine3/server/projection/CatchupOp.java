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

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.protobuf.ProtoCoder;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.Create;
import org.spine3.server.event.EventFilter;
import org.spine3.server.event.EventStore;
import org.spine3.users.TenantId;

import java.util.Set;

/**
 * The operation of catching up projections with the specified state class.
 *
 * @author Alexander Yevsyukov
 */
public class CatchupOp {

    private final ProjectionRepository<?, ?, ?> repository;
    private final EventStore eventStore;
    private final Set<EventFilter> eventFilters;
    private final PipelineOptions options;

    public CatchupOp(ProjectionRepository<?, ?, ?> repository,
                     PipelineOptions options) {
        this.repository = repository;
        this.options = options;

        this.eventStore = repository.getEventStore();
        this.eventFilters = repository.getEventFilters();

    }

    private Pipeline createPipeline() {
        Pipeline pipeline = Pipeline.create(options);

        // Get tenant IDs.
        pipeline.apply(readAllTenantIdentifiers());

        // Compose Event Stream Query

        // PCollection<Event> events = ;

        // For each tenant ID get
        // PCollection eventStream = ;

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
}
