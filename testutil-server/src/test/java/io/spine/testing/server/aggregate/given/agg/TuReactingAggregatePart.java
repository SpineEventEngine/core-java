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

package io.spine.testing.server.aggregate.given.agg;

import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.Apply;
import io.spine.server.event.React;
import io.spine.testing.server.entity.given.Given;
import io.spine.testing.server.given.entity.TuComments;
import io.spine.testing.server.given.entity.TuCommentsVBuilder;
import io.spine.testing.server.given.entity.TuTaskId;
import io.spine.testing.server.given.entity.event.TuCommentAdded;
import io.spine.testing.server.given.entity.event.TuCommentLimitReached;

/**
 * A sample aggregate part that handles commands.
 */
public final class TuReactingAggregatePart
        extends AggregatePart<TuTaskId, TuComments, TuCommentsVBuilder, TuAggregateRoot> {

    public static final TuTaskId ID = TuTaskId.newBuilder()
                                              .setValue("agg-part-id")
                                              .build();

    private TuReactingAggregatePart(TuAggregateRoot root) {
        super(root);
    }

    public static TuReactingAggregatePart newInstance(TuAggregateRoot root) {
        TuReactingAggregatePart result =
                Given.aggregatePartOfClass(TuReactingAggregatePart.class)
                     .withRoot(root)
                     .withId(root.getId())
                     .withVersion(5)
                     .build();
        return result;
    }

    @React
    TuCommentLimitReached on(TuCommentAdded event) {
        return TuCommentLimitReached.newBuilder()
                                    .setId(event.getId())
                                    .build();
    }

    @Apply
    private void on(TuCommentLimitReached event) {
        builder().setCommentLimitReached(true);
    }
}
