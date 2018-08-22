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

package io.spine.testing.server.aggregate.given.agg;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.event.React;
import io.spine.testing.server.entity.given.Given;
import io.spine.testing.server.given.entity.TuProject;
import io.spine.testing.server.given.entity.TuProjectId;
import io.spine.testing.server.given.entity.TuProjectVBuilder;
import io.spine.testing.server.given.entity.event.TuProjectAssigned;
import io.spine.testing.server.given.entity.event.TuProjectCreated;

import static com.google.protobuf.util.Timestamps.fromMillis;

/**
 * A dummy aggregate that reacts on the {@code TUProjectCreated} event and emits
 * {@code TUProjectAssigned} event as a result.
 */
public final class TuReactingAggregate
        extends Aggregate<TuProjectId, TuProject, TuProjectVBuilder> {

    public static final TuProjectId ID = TuProjectId.newBuilder()
                                                     .setValue("test id")
                                                     .build();

    TuReactingAggregate(TuProjectId id) {
        super(id);
    }

    public static TuReactingAggregate newInstance() {
        TuReactingAggregate result = Given.aggregateOfClass(TuReactingAggregate.class)
                                          .withId(ID)
                                          .withVersion(64)
                                          .build();
        return result;
    }

    @React
    public TuProjectAssigned handle(TuProjectCreated event) {
        return TuProjectAssigned.newBuilder()
                                .setId(event.getId())
                                .build();
    }

    @Apply
    void on(TuProjectAssigned event) {
        getBuilder().setTimestamp(fromMillis(123456));
    }
}
