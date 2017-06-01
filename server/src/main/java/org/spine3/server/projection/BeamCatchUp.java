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

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.spine3.users.TenantId;

import java.util.Set;

/**
 * Beam-based catch-up support.
 *
 * @author Alexander Yevsyukov
 */
class BeamCatchUp {

    private BeamCatchUp() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Iteratively performs catch-up for the passed projections.
     */
    static <I> void forAllTenants(ProjectionRepository<I, ?, ?> repository) {
        final Set<TenantId> allTenants = repository.getBoundedContext()
                                                   .getTenantIndex()
                                                   .getAll();
        final PipelineOptions options = PipelineOptionsFactory.create();

        //TODO:2017-05-21:alexander.yevsyukov: re-write using PCollection<TenantId> as the input.
        // This would allow performing this in parallel. It should not be difficult, but would
        // require changing the way `CatchupOp` is started. Now `EventStreamQuery` is obtained
        // from the repository, but we'll need to do it via a DoFn, which works with
        // ProjectionStorageIO for obtaining EventStreamQuery, which includes a timestamp of last
        // handled event. OR, we would implement storing timestamps differently (per projection)
        // and composing EventStreamQuery would be two calls for corresponding RecordStorageIOs.

        for (TenantId tenantId : allTenants) {
            final TenantCatchup<I> catchupOp = new TenantCatchup<>(tenantId, repository, options);
            final PipelineResult result = catchupOp.run();
            result.waitUntilFinish();
        }
    }
}
