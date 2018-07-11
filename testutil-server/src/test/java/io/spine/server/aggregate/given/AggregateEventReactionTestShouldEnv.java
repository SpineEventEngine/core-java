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
import io.spine.core.React;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateEventReactionTest;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.entity.Repository;
import io.spine.server.entity.given.Given;
import io.spine.server.expected.EventHandlerExpected;
import io.spine.test.testutil.TUProjectAggregate;
import io.spine.test.testutil.TUProjectAggregateVBuilder;
import io.spine.test.testutil.TUProjectAssigned;
import io.spine.test.testutil.TUProjectCreated;
import io.spine.test.testutil.TUProjectId;
import org.junit.jupiter.api.BeforeEach;

import static com.google.protobuf.util.Timestamps.fromMillis;

/**
 * @author Vladyslav Lubenskyi
 */
public class AggregateEventReactionTestShouldEnv {

    private static final TUProjectId ID = TUProjectId.newBuilder()
                                                     .setValue("test id")
                                                     .build();

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
     * A dummy aggregate that reacts on the {@code TUProjectCreated} event and emits
     * {@code TUProjectAssigned} event as a result.
     */
    public static final class EventReactingAggregate
            extends Aggregate<TUProjectId,
                              TUProjectAggregate,
                              TUProjectAggregateVBuilder> {

        EventReactingAggregate(TUProjectId id) {
            super(id);
        }

        @React
        public TUProjectAssigned handle(TUProjectCreated event) {
            return TUProjectAssigned.newBuilder()
                                    .setId(event.getId())
                                    .build();
        }

        @Apply
        void on(TUProjectAssigned event) {
            getBuilder().setTimestamp(fromMillis(123456));
        }
    }

    private static class EventReactingAggregateRepository
            extends AggregateRepository<TUProjectId, EventReactingAggregate> {
    }

    /**
     * The test class for the {@code TUProjectAssigned} event handler in
     * {@code EventReactingAggregate}.
     */
    public static class EventReactingAggregateTest
            extends AggregateEventReactionTest<TUProjectId,
                                               TUProjectCreated,
                                               TUProjectAggregate,
                                               EventReactingAggregate> {

        public static final TUProjectCreated TEST_EVENT =
                TUProjectCreated.newBuilder()
                                .setId(ID)
                                .build();

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        protected TUProjectId newId() {
            return ID;
        }

        @Override
        protected TUProjectCreated createMessage() {
            return TEST_EVENT;
        }

        @Override
        protected Repository<TUProjectId, EventReactingAggregate> createEntityRepository() {
            return new EventReactingAggregateRepository();
        }

        @Override
        public EventHandlerExpected<TUProjectAggregate> expectThat(EventReactingAggregate entity) {
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
