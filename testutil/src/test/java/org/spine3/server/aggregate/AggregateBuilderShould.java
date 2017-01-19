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

package org.spine3.server.aggregate;

import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.protobuf.Timestamps;

import static org.junit.Assert.assertEquals;

public class AggregateBuilderShould {

    private static AggregateBuilder<TestAggregate, Integer, Timestamp> givenAggregate() {
        final AggregateBuilder<TestAggregate, Integer, Timestamp> result = new AggregateBuilder<>();
        result.setClass(TestAggregate.class);
        return result;
    }

    @Test
    public void create_aggregate() {
        final int id = 2048;
        final int version = 2017;
        final Timestamp whenModified = Timestamps.getCurrentTime();
        final Timestamp state = Timestamps.minutesAgo(60);

        final Aggregate aggregate = givenAggregate()
                .withId(id)
                .withVersion(version)
                .withState(state)
                .modifiedOn(whenModified)
                .build();

        assertEquals(TestAggregate.class, aggregate.getClass());
        assertEquals(id, aggregate.getId());
        assertEquals(state, aggregate.getState());
        assertEquals(version, aggregate.getVersion());
        assertEquals(whenModified, aggregate.whenModified());
    }

    private static class TestAggregate extends Aggregate<Integer, Timestamp, Timestamp.Builder> {
        protected TestAggregate(Integer id) {
            super(id);
        }
    }
}
