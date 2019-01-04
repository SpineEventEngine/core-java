/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.testing.server.entity.given;

import io.spine.server.BoundedContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.AggregateRoot;
import io.spine.server.procman.ProcessManager;
import io.spine.server.projection.Projection;
import io.spine.testing.server.given.entity.TuComments;
import io.spine.testing.server.given.entity.TuCommentsVBuilder;
import io.spine.testing.server.given.entity.TuPmState;
import io.spine.testing.server.given.entity.TuPmStateVBuilder;
import io.spine.testing.server.given.entity.TuProject;
import io.spine.testing.server.given.entity.TuProjectId;
import io.spine.testing.server.given.entity.TuProjectPart;
import io.spine.testing.server.given.entity.TuProjectPartVBuilder;
import io.spine.testing.server.given.entity.TuProjectVBuilder;
import io.spine.testing.server.given.entity.TuTaskId;

class GivenTestEnv {

    /** Prevents instantiation of this utility class. */
    private GivenTestEnv() {
    }

    static class AnAggregate
            extends Aggregate<TuProjectId, TuProject, TuProjectVBuilder> {
        protected AnAggregate(TuProjectId id) {
            super(id);
        }
    }

    static class AnAggregatePart
            extends AggregatePart<TuProjectId, TuProjectPart, TuProjectPartVBuilder, ARoot> {
        protected AnAggregatePart(ARoot root) {
            super(root);
        }
    }

    static class AProjection
            extends Projection<TuTaskId, TuComments, TuCommentsVBuilder> {
        protected AProjection(TuTaskId id) {
            super(id);
        }
    }

    static class AProcessManager
            extends ProcessManager<TuProjectId, TuPmState, TuPmStateVBuilder> {
        protected AProcessManager(TuProjectId id) {
            super(id);
        }
    }

    private static class ARoot extends AggregateRoot<TuProjectId> {
        protected ARoot(BoundedContext boundedContext, TuProjectId id) {
            super(boundedContext, id);
        }
    }
}
