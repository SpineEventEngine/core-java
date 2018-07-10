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
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.core.React;
import io.spine.server.expected.MessageProducingExpected;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateEventReactionTest;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.entity.Repository;
import io.spine.server.entity.given.Given;
import io.spine.validate.StringValueVBuilder;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Vladyslav Lubenskyi
 */
public class AggregateEventReactionTestShouldEnv {

    private static final long ID = 1L;

    /**
     * Prevents direct instantiation.
     */
    private AggregateEventReactionTestShouldEnv() {
    }

    public static EventReactingAggregate aggregate() {
        EventReactingAggregate result = Given.aggregateOfClass(EventReactingAggregate.class)
                                             .withId(ID)
                                             .withVersion(64)
                                             .build();
        return result;
    }

    /**
     * A dummy aggregate that reacts on the {@code Timestamp} event and emits {@code StringValue}
     * event as a result.
     */
    public static final class EventReactingAggregate
            extends Aggregate<Long, StringValue, StringValueVBuilder> {

        EventReactingAggregate(Long id) {
            super(id);
        }

        @React
        public StringValue handle(Timestamp command) {
            return StringValue.newBuilder()
                              .setValue(Timestamps.toString(command))
                              .build();
        }

        @Apply
        void on(StringValue event) {
            getBuilder().setValue(event.getValue());
        }
    }

    private static class EventReactingAggregateRepository
            extends AggregateRepository<Long, EventReactingAggregate> {
    }

    /**
     * The test class for the {@code EventReactingAggregate} only command reactor.
     */
    public static class EventReactingAggregateTest
            extends AggregateEventReactionTest<Long,
                                               Timestamp,
                                               StringValue,
                                               EventReactingAggregate> {

        public static final Timestamp TEST_EVENT = Timestamp.newBuilder()
                                                            .setNanos(125)
                                                            .build();

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        protected Long newId() {
            return ID;
        }

        @Override
        protected Timestamp createMessage() {
            return TEST_EVENT;
        }

        @Override
        protected Repository<Long, EventReactingAggregate> createEntityRepository() {
            return new EventReactingAggregateRepository();
        }

        @Override
        public MessageProducingExpected<StringValue> expectThat(EventReactingAggregate entity) {
            return super.expectThat(entity);
        }

        public Message storedMessage() {
            return message();
        }

        /**
         * Exposes internal configuration method.
         */
        public void init() {
            configureBoundedContext();
        }
    }
}
