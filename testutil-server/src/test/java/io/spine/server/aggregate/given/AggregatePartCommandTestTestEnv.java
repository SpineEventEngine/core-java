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

package io.spine.server.aggregate.given;

import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import io.spine.base.Identifier;
import io.spine.client.ActorRequestFactory;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.AggregatePartCommandTest;
import io.spine.server.aggregate.AggregateRoot;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.entity.given.Given;
import io.spine.validate.UInt32ValueVBuilder;

/**
 * @author Alexander Yevsyukov
 * @author Dmytro Kuzmin
 */
public class AggregatePartCommandTestTestEnv {

    /** Prevents instantiation of this utility class. */
    private AggregatePartCommandTestTestEnv() {
    }

    /**
     * A dummy aggregate part that counts the number of commands it receives as {@code Timestamp}s.
     */
    public static final class TimerCounter
            extends AggregatePart<String, UInt32Value, UInt32ValueVBuilder, TimerCounterRoot> {
        private TimerCounter(TimerCounterRoot root) {
            super(root);
        }

        @Assign
        public Timestamp handle(Timestamp timestamp) {
            return timestamp;
        }

        @Apply
        void on(Timestamp timestamp) {
            getBuilder().setValue(getState().getValue() + 1);
        }
    }

    /**
     * The test harness class that tests how {@code TimerCounterPart} handles its command.
     */
    public static class TimerCountingTest
            extends AggregatePartCommandTest<Timestamp, TimerCounter> {

        private static final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                                           .build();

        public TimerCountingTest(ActorRequestFactory requestFactory) {
            super(requestFactory);
        }

        public TimerCountingTest() {
            super();
        }

        @Override
        protected TimerCounter createAggregatePart() {
            TimerCounterRoot root = new TimerCounterRoot(boundedContext, Identifier.newUuid());
            final UInt32Value int32Value = UInt32Value.newBuilder()
                                                      .setValue(42)
                                                      .build();
            TimerCounter result = Given.aggregatePartOfClass(TimerCounter.class)
                                       .withRoot(root)
                                       .withId(getClass().getName())
                                       .withVersion(5)
                                       .withState(int32Value)
                                       .build();
            return result;
        }
    }

    private static class TimerCounterRoot extends AggregateRoot<String> {
        TimerCounterRoot(BoundedContext boundedContext, String id) {
            super(boundedContext, id);
        }
    }
}
