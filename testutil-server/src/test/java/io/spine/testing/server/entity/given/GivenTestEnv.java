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

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.AggregateRoot;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.procman.ProcessManager;
import io.spine.server.projection.Projection;
import io.spine.validate.StringValueVBuilder;
import io.spine.validate.TimestampVBuilder;
import io.spine.validate.UInt32ValueVBuilder;

/**
 * @author Alexander Yevsyukov
 * @author Illia Shepilov
 * @author Dmytro Kuzmin
 */
class GivenTestEnv {

    /** Prevents instantiation of this utility class. */
    private GivenTestEnv() {
    }

    static class AnEntity extends AbstractVersionableEntity<String, Timestamp> {
        protected AnEntity(String id) {
            super(id);
        }
    }

    static class AnAggregate
            extends Aggregate<Integer, StringValue, StringValueVBuilder> {
        protected AnAggregate(Integer id) {
            super(id);
        }
    }


    static class AnAggregatePart extends AggregatePart<Long,
            Timestamp,
            TimestampVBuilder,
            AnAggregateRoot> {
        protected AnAggregatePart(AnAggregateRoot root) {
            super(root);
        }
    }

    static class AProjection extends Projection<String,
            UInt32Value,
            UInt32ValueVBuilder> {
        protected AProjection(String id) {
            super(id);
        }
    }

    static class AProcessManager extends ProcessManager<Timestamp,
            StringValue,
            StringValueVBuilder> {
        protected AProcessManager(Timestamp id) {
            super(id);
        }
    }

    private static class AnAggregateRoot extends AggregateRoot<Long> {
        /**
         * Creates an new instance.
         *
         * @param boundedContext the bounded context to which the aggregate belongs
         * @param id             the ID of the aggregate
         */
        protected AnAggregateRoot(BoundedContext boundedContext, Long id) {
            super(boundedContext, id);
        }
    }
}
