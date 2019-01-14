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

package io.spine.server.entity.given;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.test.shared.EmptyAggregate;
import io.spine.server.test.shared.EmptyAggregateVBuilder;
import io.spine.test.entity.Project;
import io.spine.test.entity.ProjectId;
import io.spine.testdata.Sample;
import io.spine.testing.server.entity.given.Given;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static io.spine.base.Identifier.newUuid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

public class EntityTestEnv {

    /** Prevents instantiation of this utility class. */
    private EntityTestEnv() {
    }

    public static Matcher<Long> isBetween(Long lower, Long higher) {
        return new BaseMatcher<Long>() {
            @Override
            public boolean matches(Object o) {
                assertThat(o, instanceOf(Long.class));
                Long number = (Long) o;
                return number >= lower && number <= higher;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(" must be between " + lower + " and " + higher + ' ');
            }
        };
    }

    public static class TestEntityWithIdString
            extends AbstractEntity<String, Project> {
        public TestEntityWithIdString(String id) {
            super(id);
        }
    }

    public static class TestEntityWithIdMessage
            extends AbstractEntity<Message, Project> {
        public TestEntityWithIdMessage(Message id) {
            super(id);
        }
    }

    public static class TestEntityWithIdInteger
            extends AbstractEntity<Integer, Project> {
        public TestEntityWithIdInteger(Integer id) {
            super(id);
        }
    }

    public static class TestEntityWithIdLong
            extends AbstractEntity<Long, Project> {
        public TestEntityWithIdLong(Long id) {
            super(id);
        }
    }

    public static class BareBonesEntity extends AbstractEntity<Long, StringValue> {
        public BareBonesEntity(Long id) {
            super(id);
        }
    }

    public static class EntityWithMessageId
            extends AbstractEntity<ProjectId, StringValue> {

        public EntityWithMessageId() {
            super(Sample.messageOfType(ProjectId.class));
        }
    }

    // TODO:2018-07-25:vladyslav.lubenskyi: https://github.com/SpineEventEngine/core-java/issues/788
    // Figure out a way not to use Aggregate here.
    public static class TestAggregate
            extends Aggregate<String, EmptyAggregate, EmptyAggregateVBuilder> {

        protected TestAggregate(String id) {
            super(id);
        }

        public static TestAggregate copyOf(TestAggregate entity) {
            TestAggregate result =
                    Given.aggregateOfClass(TestAggregate.class)
                         .withId(entity.getId())
                         .withState(entity.getState())
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
}
