/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.testing.server.aggregate;

import com.google.protobuf.Timestamp;
import io.spine.testing.server.aggregate.given.SampleCommandTest;
import io.spine.testing.server.aggregate.given.SamplePartCommandTest;
import io.spine.testing.server.aggregate.given.SamplePartRejectionThrowingTest;
import io.spine.testing.server.aggregate.given.SampleRejectionThrowingTest;
import io.spine.testing.server.aggregate.given.agg.TuAggregate;
import io.spine.testing.server.aggregate.given.agg.TuAggregatePart;
import io.spine.testing.server.aggregate.given.agg.TuAggregateRoot;
import io.spine.testing.server.expected.CommandHandlerExpected;
import io.spine.testing.server.given.entity.TuComments;
import io.spine.testing.server.given.entity.TuProject;
import io.spine.testing.server.given.entity.rejection.Rejections;
import io.spine.testing.server.given.entity.rejection.Rejections.TuFailedToAssignProject;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.validate.Validate.isNotDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vladyslav Lubenskyi
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("AggregateCommandTest should")
class AggregateCommandTestShould {

    @Nested
    @DisplayName("for aggregate")
    class ForAggregate {

        private SampleCommandTest aggregateCommandTest;
        private SampleRejectionThrowingTest aggregateRejectionCommandTest;

        @BeforeEach
        void setUp() {
            ModelTests.dropAllModels();
            aggregateCommandTest = new SampleCommandTest();
            aggregateRejectionCommandTest = new SampleRejectionThrowingTest();
        }

        @Test
        @DisplayName("store tested command")
        void shouldStoreCommand() {
            assertEquals(aggregateCommandTest.storedMessage(), SampleCommandTest.TEST_COMMAND);
        }

        @Test
        @DisplayName("dispatch tested command")
        void shouldDispatchCommand() {
            TuAggregate testAggregate = TuAggregate.newInstance();
            aggregateCommandTest.expectThat(testAggregate);
            Timestamp newState = testAggregate.state()
                                              .getTimestamp();
            assertTrue(isNotDefault(newState));
        }

        @Test
        @DisplayName("track rejection")
        void trackGeneratedRejection() {
            TuAggregate testAggregate = TuAggregate.newInstance();
            CommandHandlerExpected<TuProject> expected =
                    aggregateRejectionCommandTest.expectThat(testAggregate);
            expected.throwsRejection(TuFailedToAssignProject.class);
        }
    }

    @Nested
    @DisplayName("for aggregate part")
    class ForPart {

        private SamplePartCommandTest partCommandTest;
        private SamplePartRejectionThrowingTest partRejectionCommandTest;

        @BeforeEach
        void setUp() {
            ModelTests.dropAllModels();
            partCommandTest = new SamplePartCommandTest();
            partRejectionCommandTest = new SamplePartRejectionThrowingTest();
        }

        @Test
        @DisplayName("store tested command")
        void shouldStoreCommand() {
            assertEquals(partCommandTest.storedMessage(), SamplePartCommandTest.TEST_COMMAND);
        }

        @Test
        @DisplayName("dispatch tested command")
        void shouldDispatchCommand() {
            TuAggregateRoot root = TuAggregateRoot.newInstance(TuAggregatePart.ID);
            TuAggregatePart part = TuAggregatePart.newInstance(root);
            partCommandTest.expectThat(part);
            Timestamp newState = part.state()
                                     .getTimestamp();
            assertTrue(isNotDefault(newState));
        }

        @Test
        @DisplayName("track rejection")
        void trackGeneratedRejection() {
            TuAggregateRoot root = TuAggregateRoot.newInstance(TuAggregatePart.ID);
            TuAggregatePart part = TuAggregatePart.newInstance(root);
            CommandHandlerExpected<TuComments> expected =
                    partRejectionCommandTest.expectThat(part);
            expected.throwsRejection(Rejections.TuFailedToRemoveComment.class);
        }
    }
}
