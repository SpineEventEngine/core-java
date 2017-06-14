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

package io.spine.server.projection;

import com.google.common.testing.SerializableTester;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Subscribe;
import io.spine.base.Event;
import io.spine.server.BoundedContext;
import io.spine.server.entity.EntityRecord;
import io.spine.validate.DoubleValueVBuilder;
import org.apache.beam.sdk.values.TupleTag;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alexander Yevsyukov
 */
public class ApplyEventsShould {

    private ApplyEvents<Long> applyEvents;

    @Before
    public void setUp() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        final ProjectionRepository<Long, ?, ?> projectionRepository = new SumRepo();
        boundedContext.register(projectionRepository);

        final TupleTag<Iterable<Event>> eventsTag = new TupleTag<>();
        final TupleTag<EntityRecord> entityRecordsTag = new TupleTag<>();
        final TupleTag<Timestamp> tag = new ApplyEvents.TimestampTupleTag();
        applyEvents = new ApplyEvents<>(eventsTag,
                                        entityRecordsTag,
                                        projectionRepository.entityConverter(),
                                        tag);
    }

    @Test
    public void serialize() {
        SerializableTester.reserializeAndAssert(applyEvents);
    }

    private static class Sum extends Projection<Long, DoubleValue, DoubleValueVBuilder> {

        private Sum(Long id) {
            super(id);
        }

        @Subscribe
        public void on(Int32Value value) {
            add(value.getValue());
        }

        @Subscribe
        public void on(DoubleValue value) {
            add(value.getValue());
        }

        private void add(double value) {
            final double newValue = getState().getValue() + value;
            final DoubleValue.Builder builder = getState().toBuilder()
                                                          .setValue(newValue);
            updateState(builder.build());
        }
    }

    private static class SumRepo extends ProjectionRepository<Long, Sum, DoubleValue> {

        private SumRepo() {
            super();
        }
    }
}
