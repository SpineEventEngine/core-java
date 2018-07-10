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
import com.google.protobuf.util.Timestamps;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.AggregatePartCommandTest;
import io.spine.server.aggregate.AggregatePartRepository;
import io.spine.server.aggregate.AggregateRoot;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.entity.Repository;
import io.spine.server.entity.given.Given;
import io.spine.server.expected.CommandHandlerExpected;
import io.spine.testutil.server.aggregate.TestUtilAddComment;
import io.spine.testutil.server.aggregate.TestUtilCommentAdded;
import io.spine.testutil.server.aggregate.TestUtilCommentsAggregatePart;
import io.spine.testutil.server.aggregate.TestUtilProjectCommentsAggregatePartVBuilder;
import io.spine.testutil.server.aggregate.TestUtilProjectId;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Vladyslav Lubenskyi
 */
public class AggregatePartCommandTestShouldEnv {

    private static final TestUtilProjectId ID = TestUtilProjectId.newBuilder()
                                                                 .setValue("test id")
                                                                 .build();

    /**
     * Prevents instantiation of this utility class.
     */
    private AggregatePartCommandTestShouldEnv() {
    }

    public static CommentsAggregatePart aggregatePart() {
        CommentsRoot root = aggregateRoot();
        CommentsAggregatePart result =
                Given.aggregatePartOfClass(CommentsAggregatePart.class)
                                   .withRoot(root)
                                   .withId(ID)
                                   .withVersion(5)
                                   .build();
        return result;
    }

    private static CommentsRoot aggregateRoot() {
        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .build();
        return new CommentsRoot(boundedContext, ID);
    }

    /**
     * A dummy aggregate part that handles {@code TestUtilAddComment} command and applies
     * the corresponding {@code TestUtilCommentAdded}.
     */
    public static final class CommentsAggregatePart
            extends AggregatePart<TestUtilProjectId,
                                  TestUtilCommentsAggregatePart,
                                  TestUtilProjectCommentsAggregatePartVBuilder,
                                  CommentsRoot> {

        private CommentsAggregatePart(CommentsRoot root) {
            super(root);
        }

        @Assign
        public TestUtilCommentAdded handle(TestUtilAddComment command) {
            return TestUtilCommentAdded.newBuilder()
                                       .setId(command.getId())
                                       .build();
        }

        @Apply
        void on(TestUtilCommentAdded event) {
            getBuilder().setTimestamp(Timestamps.fromMillis(123));
        }
    }

    private static final class CommentsAggregatePartRepository
            extends AggregatePartRepository<TestUtilProjectId,
                                            CommentsAggregatePart,
                                            CommentsRoot> {
    }

    private static class CommentsRoot extends AggregateRoot<TestUtilProjectId> {

        private CommentsRoot(BoundedContext boundedContext, TestUtilProjectId id) {
            super(boundedContext, id);
        }
    }

    /**
     * The test class for the {@code TestUtilAddComment} command handler in
     * {@code CommentsAggregatePart}.
     */
    public static class TimeCounterTest
            extends AggregatePartCommandTest<TestUtilProjectId,
                                             TestUtilAddComment,
                                             TestUtilCommentsAggregatePart,
                                             CommentsAggregatePart,
                                             CommentsRoot> {

        public static final TestUtilAddComment TEST_COMMAND =
                TestUtilAddComment.newBuilder()
                                  .setId(ID)
                                  .build();

        @Override
        protected TestUtilProjectId newId() {
            return ID;
        }

        @Override
        protected TestUtilAddComment createMessage() {
            return TEST_COMMAND;
        }

        @Override
        protected Repository<TestUtilProjectId, CommentsAggregatePart> createEntityRepository() {
            return new CommentsAggregatePartRepository();
        }

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        public CommandHandlerExpected<TestUtilCommentsAggregatePart>
        expectThat(CommentsAggregatePart entity) {
            return super.expectThat(entity);
        }

        public Message storedMessage() {
            return message();
        }

        @Override
        protected CommentsRoot newRoot(TestUtilProjectId id) {
            return aggregateRoot();
        }

        @Override
        protected CommentsAggregatePart newPart(CommentsRoot root) {
            return aggregatePart();
        }
    }
}
