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

package io.spine.server.aggregate.given.part;

import com.google.protobuf.StringValue;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.validate.StringValueVBuilder;

/**
 * An aggregate part with {@link StringValue} state, which belongs to an aggregate
 * represented by {@link AnAggregateRoot}.
 *
 * @author Alexander Yevsyukov
 */
public class TaskDescriptionPart
        extends AggregatePart<String,
                              StringValue,
                              StringValueVBuilder,
                              AnAggregateRoot> {

    public TaskDescriptionPart(AnAggregateRoot root) {
        super(root);
    }

    @Assign
    AggProjectCreated handle(AggCreateProject msg) {
        AggProjectCreated result = AggProjectCreated
                .newBuilder()
                .setProjectId(msg.getProjectId())
                .setName(msg.getName())
                .build();
        return result;
    }

    @Apply
    void apply(AggTaskAdded event) {
        getBuilder().setValue("Description value");
    }
}
