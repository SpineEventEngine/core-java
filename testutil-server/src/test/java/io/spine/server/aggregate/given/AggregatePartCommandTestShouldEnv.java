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
import io.spine.test.testutil.TUAddComment;
import io.spine.test.testutil.TUCommentAdded;
import io.spine.test.testutil.TUCommentsAggregatePart;
import io.spine.test.testutil.TUCommentsAggregatePartVBuilder;
import io.spine.test.testutil.TUProjectId;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Vladyslav Lubenskyi
 */
public class AggregatePartCommandTestShouldEnv {

    public static final TUProjectId ID = TUProjectId.newBuilder()
                                                    .setValue("test id")
                                                    .build();

    /**
     * Prevents instantiation of this utility class.
     */
    private AggregatePartCommandTestShouldEnv() {
    }

    public static CommentsAggregatePart aggregatePart(CommentsRoot root) {
        CommentsAggregatePart result =
                Given.aggregatePartOfClass(CommentsAggregatePart.class)
                                   .withRoot(root)
                                   .withId(root.getId())
                                   .withVersion(5)
                                   .build();
        return result;
    }

    public static CommentsRoot aggregateRoot(TUProjectId id) {
        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .build();
        return new CommentsRoot(boundedContext, id);
    }

    /**
     * A dummy aggregate part that handles {@code TestUtilAddComment} command and applies
     * the corresponding {@code TestUtilCommentAdded}.
     */
    public static final class CommentsAggregatePart
            extends AggregatePart<TUProjectId,
                                  TUCommentsAggregatePart,
                                  TUCommentsAggregatePartVBuilder,
                                  CommentsRoot> {

        private CommentsAggregatePart(CommentsRoot root) {
            super(root);
        }

        @Assign
        public TUCommentAdded handle(TUAddComment command) {
            return TUCommentAdded.newBuilder()
                                 .setId(command.getId())
                                 .build();
        }

        @Apply
        void on(TUCommentAdded event) {
            getBuilder().setTimestamp(Timestamps.fromMillis(123));
        }
    }

    private static final class CommentsAggregatePartRepository
            extends AggregatePartRepository<TUProjectId,
                                            CommentsAggregatePart,
                                            CommentsRoot> {
    }

    private static class CommentsRoot extends AggregateRoot<TUProjectId> {

        private CommentsRoot(BoundedContext boundedContext, TUProjectId id) {
            super(boundedContext, id);
        }
    }

    /**
     * The test class for the {@code TestUtilAddComment} command handler in
     * {@code CommentsAggregatePart}.
     */
    public static class TimeCounterTest
            extends AggregatePartCommandTest<TUProjectId,
                                             TUAddComment,
                                             TUCommentsAggregatePart,
                                             CommentsAggregatePart,
                                             CommentsRoot> {

        public static final TUAddComment TEST_COMMAND =
                TUAddComment.newBuilder()
                            .setId(ID)
                            .build();

        @Override
        protected TUProjectId newId() {
            return ID;
        }

        @Override
        protected TUAddComment createMessage() {
            return TEST_COMMAND;
        }

        @Override
        protected Repository<TUProjectId, CommentsAggregatePart> createEntityRepository() {
            return new CommentsAggregatePartRepository();
        }

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        public CommandHandlerExpected<TUCommentsAggregatePart>
        expectThat(CommentsAggregatePart entity) {
            return super.expectThat(entity);
        }

        public Message storedMessage() {
            return message();
        }

        @Override
        protected CommentsRoot newRoot(TUProjectId id) {
            return aggregateRoot(id);
        }

        public CommentsAggregatePart createPart(TUProjectId id) {
            return newPart(id);
        }


        @Override
        protected CommentsAggregatePart newPart(CommentsRoot root) {
            return aggregatePart(root);
        }
    }
}
