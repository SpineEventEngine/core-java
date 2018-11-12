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

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.server.entity.Repository;
import io.spine.testing.server.aggregate.AggregateCommandTest;
import io.spine.testing.server.aggregate.given.agg.TuAggregatePart;
import io.spine.testing.server.aggregate.given.agg.TuAggregatePartRepository;
import io.spine.testing.server.expected.CommandHandlerExpected;
import io.spine.testing.server.given.entity.TuComments;
import io.spine.testing.server.given.entity.TuTaskId;
import io.spine.testing.server.given.entity.command.TuAddComment;

/**
 * The test class checking handling a command in an aggregate part.
 *
 * @see io.spine.testing.server.aggregate.AggregateCommandTestShould
 */
public class SamplePartCommandTest
        extends AggregateCommandTest<TuTaskId, TuAddComment, TuComments, TuAggregatePart> {

    public static final TuAddComment TEST_COMMAND =
            TuAddComment.newBuilder()
                        .setId(TuAggregatePart.ID)
                        .build();

    public SamplePartCommandTest() {
        super(TuAggregatePart.ID, TEST_COMMAND);
    }

    @Override
    protected Repository<TuTaskId, TuAggregatePart>
    createRepository() {
        return new TuAggregatePartRepository();
    }

    @CanIgnoreReturnValue
    @Override
    public CommandHandlerExpected<TuComments> expectThat(TuAggregatePart entity) {
        return super.expectThat(entity);
    }

    @VisibleForTesting
    public Message storedMessage() {
        return message();
    }
}
