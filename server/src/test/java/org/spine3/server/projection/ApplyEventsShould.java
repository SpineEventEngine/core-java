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

import com.google.common.testing.SerializableTester;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Timestamp;
import org.apache.beam.sdk.values.TupleTag;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.Subscribe;
import org.spine3.server.BoundedContext;
import org.spine3.server.entity.EntityRecord;

/**
 * @author Alexander Yevsyukov
 */
public class ApplyEventsShould {

    private ApplyEvents<Long> applyEvents;

    @Before
    public void setUp() {
        final BoundedContext boundedContext = BoundedContext.newBuilder().build();
        final ProjectionRepository<Long, ?, ?> projectionRepository = new SumRepo(boundedContext);
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
        SerializableTester.reserialize(applyEvents);
    }

    private static class Sum extends Projection<Long, DoubleValue> {

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

        private SumRepo(BoundedContext boundedContext) {
            super(boundedContext,
                  false /* Do not catch-up automatically so that we can test serialization of
                           `ApplyEvents` only. */);
        }
    }
}
