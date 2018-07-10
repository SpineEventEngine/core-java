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

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import io.spine.base.Identifier;
import io.spine.server.BoundedContext;
import io.spine.server.expected.CommandExpected;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.AggregatePartCommandTest;
import io.spine.server.aggregate.AggregatePartRepository;
import io.spine.server.aggregate.AggregateRoot;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.entity.Repository;
import io.spine.server.entity.given.Given;
import io.spine.validate.UInt32ValueVBuilder;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Vladyslav Lubenskyi
 */
public class AggregatePartCommandTestShouldEnv {

    private static final long ID = 1L;

    /**
     * Prevents instantiation of this utility class.
     */
    private AggregatePartCommandTestShouldEnv() {
    }

    public static TimerCounter aggregatePart() {
        TimerCounterRoot root = aggregateRoot();
        UInt32Value int32Value = UInt32Value.newBuilder()
                                            .setValue(42)
                                            .build();
        TimerCounter result = Given.aggregatePartOfClass(TimerCounter.class)
                                   .withRoot(root)
                                   .withId(AggregatePartCommandTestShouldEnv.class.getName())
                                   .withVersion(5)
                                   .withState(int32Value)
                                   .build();
        return result;
    }

    private static TimerCounterRoot aggregateRoot() {
        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .build();
        return new TimerCounterRoot(boundedContext, Identifier.newUuid());
    }

    /**
     * A dummy aggregate that counts how many times the {@link StringValue} event occurred.
     */
    public static final class TimerCounter
            extends AggregatePart<String, UInt32Value, UInt32ValueVBuilder, TimerCounterRoot> {

        private TimerCounter(TimerCounterRoot root) {
            super(root);
        }

        @Assign
        public StringValue handle(StringValue command) {
            return command;
        }

        @Apply
        void on(StringValue event) {
            getBuilder().setValue(getState().getValue() + 1);
        }
    }

    private static final class TimeCounterRepository
            extends AggregatePartRepository<String, TimerCounter, TimerCounterRoot> {
    }

    private static class TimerCounterRoot extends AggregateRoot<String> {

        private TimerCounterRoot(BoundedContext boundedContext, String id) {
            super(boundedContext, id);
        }
    }

    /**
     * The test class for the {@code StringValue} command handler in {@code TimePrinter}.
     */
    public static class TimeCounterTest
            extends AggregatePartCommandTest<String,
                                             StringValue,
                                             UInt32Value,
                                             TimerCounter,
                                             TimerCounterRoot> {

        public static final StringValue TEST_COMMAND = StringValue.newBuilder()
                                                                  .setValue("test command")
                                                                  .build();

        @Override
        protected String newId() {
            return AggregatePartCommandTestShouldEnv.class.getName();
        }

        @Override
        protected StringValue createMessage() {
            return TEST_COMMAND;
        }

        @Override
        protected Repository<String, TimerCounter> createEntityRepository() {
            return new TimeCounterRepository();
        }

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        public CommandExpected<UInt32Value> expectThat(TimerCounter entity) {
            return super.expectThat(entity);
        }

        public Message storedMessage() {
            return message();
        }

        @Override
        protected TimerCounterRoot newRoot(String id) {
            return aggregateRoot();
        }

        @Override
        protected TimerCounter newPart(TimerCounterRoot root) {
            return aggregatePart();
        }
    }
}
