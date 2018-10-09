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

import com.google.protobuf.Message;
import io.spine.server.entity.Repository;
import io.spine.testing.server.aggregate.AggregatePartEventImportTest;
import io.spine.testing.server.aggregate.given.agg.TuAggregatePart;
import io.spine.testing.server.aggregate.given.agg.TuAggregatePartRepository;
import io.spine.testing.server.aggregate.given.agg.TuAggregateRoot;
import io.spine.testing.server.expected.EventReactorExpected;
import io.spine.testing.server.given.entity.TuComments;
import io.spine.testing.server.given.entity.TuTaskId;
import io.spine.testing.server.given.entity.event.TuCommentRecievedByEmail;

/**
 * The test class for checking an import of an event into an aggregate part.
 *
 * @see io.spine.testing.server.aggregate.AggregatePartEventImportTestShould
 */
public class SamplePartEventImportTest
        extends AggregatePartEventImportTest<TuTaskId,
                                             TuCommentRecievedByEmail,
                                             TuComments,
                                             TuAggregatePart,
                                             TuAggregateRoot> {

    public static final TuCommentRecievedByEmail TEST_EVENT =
            TuCommentRecievedByEmail.newBuilder()
                                    .setId(TuAggregatePart.ID)
                                    .build();

    public SamplePartEventImportTest() {
        super(TuAggregatePart.ID, TEST_EVENT);
    }

    @Override
    protected Repository<TuTaskId, TuAggregatePart> createEntityRepository() {
        return new TuAggregatePartRepository();
    }

    @Override
    public EventReactorExpected<TuComments>
    expectThat(TuAggregatePart entity) {
        return super.expectThat(entity);
    }

    public Message storedMessage() {
        return message();
    }

    @Override
    protected TuAggregateRoot newRoot(TuTaskId id) {
        return TuAggregateRoot.newInstance(id);
    }

    public TuAggregatePart createPart(TuTaskId id) {
        return newPart(id);
    }

    @Override
    protected TuAggregatePart newPart(TuAggregateRoot root) {
        return TuAggregatePart.newInstance(root);
    }
}
