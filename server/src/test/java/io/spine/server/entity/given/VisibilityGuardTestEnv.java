/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.entity.given;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.test.entity.FullAccessAggregate;
import io.spine.test.entity.FullAccessAggregateVBuilder;
import io.spine.test.entity.HiddenAggregate;
import io.spine.test.entity.HiddenAggregateVBuilder;
import io.spine.test.entity.SubscribableAggregate;
import io.spine.test.entity.SubscribableAggregateVBuilder;

public class VisibilityGuardTestEnv {

    private VisibilityGuardTestEnv() {
        // Prevent instantiation of this utility class.
    }

    public static class Exposed
            extends Aggregate<Long, FullAccessAggregate, FullAccessAggregateVBuilder> {
        public Exposed(Long id) {
            super(id);
        }
    }

    public static class ExposedRepository extends AggregateRepository<Long, Exposed> {
        public ExposedRepository() {
            super();
        }
    }

    public static class Subscribable
            extends Aggregate<Long, SubscribableAggregate, SubscribableAggregateVBuilder> {
        protected Subscribable(Long id) {
            super(id);
        }
    }

    public static class SubscribableRepository extends AggregateRepository<Long, Subscribable> {
        public SubscribableRepository() {
            super();
        }
    }

    public static class Hidden
            extends Aggregate<String, HiddenAggregate, HiddenAggregateVBuilder> {
        public Hidden(String id) {
            super(id);
        }
    }

    public static class HiddenRepository extends AggregateRepository<String, Hidden> {
        public HiddenRepository() {
            super();
        }
    }
}
