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
import com.google.protobuf.Message;
import io.spine.server.entity.Repository;
import io.spine.testing.server.aggregate.AggregateEventReactionTest;
import io.spine.testing.server.aggregate.given.agg.TuReactingAggregatePart;
import io.spine.testing.server.aggregate.given.agg.TuReactingAggregatePartRepository;
import io.spine.testing.server.expected.EventReactorExpected;
import io.spine.testing.server.given.entity.TuComments;
import io.spine.testing.server.given.entity.TuTaskId;
import io.spine.testing.server.given.entity.event.TuCommentAdded;

/**
 * The test class for the {@link TuCommentAdded} event handler in {@link TuReactingAggregatePart}.
 *
 * @see io.spine.testing.server.aggregate.AggregateEventReactionTestShould
 */
public class SamplePartEventReactionTest
        extends AggregateEventReactionTest<TuTaskId,
                                           TuCommentAdded,
                                           TuComments,
                                           TuReactingAggregatePart> {

    public static final TuCommentAdded TEST_EVENT =
            TuCommentAdded.newBuilder()
                          .setId(TuReactingAggregatePart.ID)
                          .build();

    public SamplePartEventReactionTest() {
        super(TuReactingAggregatePart.ID, TEST_EVENT);
    }

    @Override
    @VisibleForTesting
    public EventReactorExpected<TuComments> expectThat(TuReactingAggregatePart entity) {
        return super.expectThat(entity);
    }

    @Override
    protected Repository<TuTaskId, TuReactingAggregatePart> createRepository() {
        return new TuReactingAggregatePartRepository();
    }

    @VisibleForTesting
    public Message storedMessage() {
        return message();
    }
}
