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

package io.spine.server.aggregate;

import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import io.spine.Identifier;
import io.spine.client.ActorRequestFactory;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandTest;
import io.spine.server.entity.given.Given;
import io.spine.time.ZoneOffsets;
import io.spine.validate.UInt32ValueVBuilder;
import org.junit.Before;
import org.junit.Test;

import static io.spine.core.given.GivenUserId.newUuid;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class AggregatePartCommandTestShould {

    private AggregatePartCommandTest<Timestamp, TimerCounter> aggregatePartCommandTest;

    private static ActorRequestFactory newRequestFactory(Class<?> clazz) {
        return ActorRequestFactory.newBuilder()
                                  .setActor(newUuid())
                                  .setZoneOffset(ZoneOffsets.UTC)
                                  .setTenantId(TenantId.newBuilder()
                                                       .setValue(clazz.getSimpleName())
                                                       .build())
                                  .build();
    }

    @Before
    public void setUp() {
        aggregatePartCommandTest = new TimerCountingTest();
    }

    @Test
    public void create_an_aggregate_part_in_setUp() {
        assertFalse(aggregatePartCommandTest.aggregatePart()
                                            .isPresent());

        aggregatePartCommandTest.setUp();

        assertTrue(aggregatePartCommandTest.aggregatePart()
                                           .isPresent());
    }

    /**
     * Ensures existence of the constructor in {@link AggregatePartCommandTest} class.
     *
     * <p>We do this by simply invoking the constructor in the derived class.
     * We do not perform checks because they are done in the test suite that checks
     * {@link CommandTest} class.
     */
    @SuppressWarnings("ResultOfObjectAllocationIgnored") // because we don't need the result.
    @Test
    public void has_constructor_with_ActorRequestFactory() {
        new TimerCountingTest(newRequestFactory(getClass()));
    }

    /**
     * A dummy aggregate part that counts the number of commands it receives as {@code Timestamp}s.
     */
    private static final class TimerCounter
            extends AggregatePart<String, UInt32Value, UInt32ValueVBuilder, TimerCounterRoot> {
        private TimerCounter(TimerCounterRoot root) {
            super(root);
        }

        @Assign
        public Timestamp handle(Timestamp timestamp) {
            return timestamp;
        }

        @Apply
        private void on(Timestamp timestamp) {
            getBuilder().setValue(getState().getValue() + 1);
        }
    }

    /**
     * The test harness class that tests how {@code TimerCounterPart} handles its command.
     */
    private static class TimerCountingTest
            extends AggregatePartCommandTest<Timestamp, TimerCounter> {

        private static final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                                           .build();

        private TimerCountingTest(ActorRequestFactory requestFactory) {
            super(requestFactory);
        }

        private TimerCountingTest() {
            super();
        }

        @Override
        protected TimerCounter createAggregatePart() {
            final TimerCounterRoot root = new TimerCounterRoot(boundedContext, Identifier.newUuid());
            final TimerCounter result = Given.aggregatePartOfClass(TimerCounter.class)
                                             .withRoot(root)
                                             .withId(getClass().getName())
                                             .withVersion(5)
                                             .withState(UInt32Value.newBuilder()
                                                                   .setValue(42)
                                                                   .build())
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
