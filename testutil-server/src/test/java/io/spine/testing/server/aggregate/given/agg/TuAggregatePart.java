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
import io.spine.server.command.Assign;
import io.spine.testing.server.entity.given.Given;
import io.spine.testing.server.given.entity.TuComments;
import io.spine.testing.server.given.entity.TuCommentsVBuilder;
import io.spine.testing.server.given.entity.TuTaskId;
import io.spine.testing.server.given.entity.command.TuAddComment;
import io.spine.testing.server.given.entity.command.TuRemoveComment;
import io.spine.testing.server.given.entity.event.TuCommentAdded;
import io.spine.testing.server.given.entity.event.TuCommentReceivedByEmail;
import io.spine.testing.server.given.entity.event.TuCommentRemoved;
import io.spine.testing.server.given.entity.rejection.TuFailedToRemoveComment;

import static com.google.protobuf.util.Timestamps.fromMillis;

/**
 * A sample aggregate part that handles commands.
 */
public final class TuAggregatePart
        extends AggregatePart<TuTaskId, TuComments, TuCommentsVBuilder, TuAggregateRoot> {

    public static final TuTaskId ID = TuTaskId.newBuilder()
                                              .setValue("agg-part-id")
                                              .build();

    private TuAggregatePart(TuAggregateRoot root) {
        super(root);
    }

    public static TuAggregatePart newInstance(TuAggregateRoot root) {
        TuAggregatePart result =
                Given.aggregatePartOfClass(TuAggregatePart.class)
                     .withRoot(root)
                     .withId(root.id())
                     .withVersion(5)
                     .build();
        return result;
    }

    @Assign
    TuCommentAdded handle(TuAddComment command) {
        return TuCommentAdded.newBuilder()
                             .setId(command.getId())
                             .build();
    }

    @Assign
    TuCommentRemoved handle(TuRemoveComment command) throws TuFailedToRemoveComment {
        throw TuFailedToRemoveComment
                .newBuilder()
                .setId(id())
                .build();
    }

    @Apply
    private void on(TuCommentAdded event) {
        builder().setId(event.getId())
                 .setTimestamp(fromMillis(1234567));
    }

    @Apply(allowImport = true)
    private void on(TuCommentReceivedByEmail event) {
        builder().setId(event.getId())
                 .setCommentsReceivedByEmail(state().getCommentsReceivedByEmail() + 1)
                 .setTimestamp(fromMillis(1234567));
    }
}
