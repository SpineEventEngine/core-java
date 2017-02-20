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

package org.spine3.test;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import org.junit.Test;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregatePart;
import org.spine3.server.aggregate.AggregateRoot;
import org.spine3.server.entity.AbstractVersionableEntity;
import org.spine3.server.procman.ProcessManager;
import org.spine3.server.projection.Projection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateParameterlessCtor;

public class GivenShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateParameterlessCtor(Given.class));
    }

    @Test
    public void create_entity_builder() {
        assertEquals(AnEntity.class, Given.entityOfClass(AnEntity.class)
                                          .getResultClass());
    }

    private static class AnEntity extends AbstractVersionableEntity<String, Timestamp> {
        protected AnEntity(String id) {
            super(id);
        }
    }

    @Test
    public void create_aggregate_builder() {
        assertEquals(AnAggregate.class, Given.aggregateOfClass(AnAggregate.class)
                                             .getResultClass());
    }

    private static class AnAggregate extends Aggregate<Integer, StringValue, StringValue.Builder> {
        protected AnAggregate(Integer id) {
            super(id);
        }
    }

    @Test
    public void create_aggregate_part_builder() {
        assertEquals(AnAggregatePart.class, Given.aggregatePartOfClass(AnAggregatePart.class)
                                                 .getResultClass());
    }

    private static class AnAggregatePart extends AggregatePart<Long, Timestamp, Timestamp.Builder> {
        protected AnAggregatePart(Long id, AnAggregateRoot root) {
            super(id, root);
        }
    }

    @Test
    public void create_projection_builder() {
        assertEquals(AProjection.class, Given.projectionOfClass(AProjection.class)
                                             .getResultClass());
    }

    private static class AProjection extends Projection<String, UInt32Value> {
        protected AProjection(String id) {
            super(id);
        }
    }

    @Test
    public void create_builder_for_process_managers() {
        assertEquals(AProcessManager.class, Given.processManagerOfClass(AProcessManager.class)
                                                 .getResultClass());
    }

    @Test
    public void pass_the_null_tolerance_check() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(Given.class)
                                                                     .addDefaultValue(
                                                                             AProjection.class)
                                                                     .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }

    private static class AProcessManager extends ProcessManager<Timestamp, StringValue> {
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
