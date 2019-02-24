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

package io.spine.server.entity.given.entity;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.test.shared.EmptyAggregate;
import io.spine.server.test.shared.EmptyAggregateVBuilder;
import io.spine.testing.server.entity.given.Given;

import static io.spine.base.Identifier.newUuid;

// TODO:2018-07-25:vladyslav.lubenskyi: https://github.com/SpineEventEngine/core-java/issues/788
// Figure out a way not to use Aggregate here.
public class TestAggregate
        extends Aggregate<String, EmptyAggregate, EmptyAggregateVBuilder> {

    protected TestAggregate(String id) {
        super(id);
    }

    public static TestAggregate copyOf(TestAggregate entity) {
        TestAggregate result =
                Given.aggregateOfClass(TestAggregate.class)
                     .withId(entity.id())
                     .withState(entity.state())
                     .modifiedOn(entity.whenModified())
                     .withVersion(entity.getVersion()
                                        .getNumber())
                     .build();
        return result;
    }

    public static TestAggregate withState() {
        String id = newUuid();
        EmptyAggregate state = EmptyAggregate
                .newBuilder()
                .setId(id)
                .build();
        TestAggregate result =
                Given.aggregateOfClass(TestAggregate.class)
                     .withId(id)
                     .withState(state)
                     .withVersion(3)
                     .build();
        return result;
    }
}
