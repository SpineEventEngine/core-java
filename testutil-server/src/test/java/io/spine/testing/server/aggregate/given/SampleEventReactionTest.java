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

package io.spine.testing.server.aggregate.given;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.server.entity.Repository;
import io.spine.testing.server.aggregate.AggregateEventReactionTest;
import io.spine.testing.server.aggregate.given.agg.TuReactingAggregate;
import io.spine.testing.server.aggregate.given.agg.TuReactingAggregateRepository;
import io.spine.testing.server.expected.EventReactorExpected;
import io.spine.testing.server.given.entity.TuProject;
import io.spine.testing.server.given.entity.TuProjectId;
import io.spine.testing.server.given.entity.event.TuProjectCreated;

/**
 * The test class for the {@link TuProjectCreated} event handler in {@link TuReactingAggregate}.
 *
 * @see io.spine.testing.server.aggregate.AggregateEventReactionTestShould
 */
public class SampleEventReactionTest
        extends AggregateEventReactionTest<TuProjectId,
                                           TuProjectCreated,
                                           TuProject,
                                           TuReactingAggregate> {

    public static final TuProjectCreated TEST_EVENT =
            TuProjectCreated.newBuilder()
                            .setId(TuReactingAggregate.ID)
                            .build();

    public SampleEventReactionTest() {
        super(TuReactingAggregate.ID, TEST_EVENT);
    }

    @Override
    @VisibleForTesting
    public EventReactorExpected<TuProject> expectThat(TuReactingAggregate entity) {
        return super.expectThat(entity);
    }

    @Override
    protected Repository<TuProjectId, TuReactingAggregate> createRepository() {
        return new TuReactingAggregateRepository();
    }

    @VisibleForTesting
    public Message storedMessage() {
        return message();
    }
}
