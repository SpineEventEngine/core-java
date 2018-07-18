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

package io.spine.testing.server.aggregate.given;

import com.google.protobuf.Timestamp;
import io.spine.server.aggregate.Aggregate;
import io.spine.testing.server.aggregate.AggregateBuilder;
import io.spine.validate.TimestampVBuilder;

/**
 * @author Dmytro Dashenkov
 * @author Dmytro Kuzmin
 */
public class AggregateBuilderTestEnv {

    /** Prevents instantiation of this utility class. */
    private AggregateBuilderTestEnv() {
    }

    public static AggregateBuilder<TestAggregate, Integer, Timestamp> givenAggregate() {
        AggregateBuilder<TestAggregate, Integer, Timestamp> result = new AggregateBuilder<>();
        result.setResultClass(TestAggregate.class);
        return result;
    }

    public static class TestAggregate extends Aggregate<Integer, Timestamp, TimestampVBuilder> {
        protected TestAggregate(Integer id) {
            super(id);
        }
    }
}
