/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.entity;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.spine3.protobuf.Timestamps;
import org.spine3.test.Tests;
import org.spine3.test.entity.Project;
import org.spine3.test.entity.ProjectId;
import org.spine3.testdata.Sample;
import org.spine3.time.Interval;
import org.spine3.time.Intervals;

import java.lang.reflect.Constructor;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Timestamps.getCurrentTime;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.currentTimeSeconds;

/**
 * @author Alexander Litus
 */
public class EntityShould {

    private Project state = Sample.messageOfType(Project.class);
    private TestEntity entityNew;
    private TestEntity entityWithState;

    @Before
    public void setUp() {
        state = Sample.messageOfType(Project.class);
        entityNew = TestEntity.newInstance(newUuid());
        entityWithState = TestEntity.withState();
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored") // because we expect the exception.
    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_id() {
        new BareBonesEntity(Tests.<Long>nullRef());
    }

    @Test
    public void return_default_state() {
        final Project state = entityNew.getDefaultState();
        assertEquals(Project.getDefaultInstance(), state);
    }

    @Test
    public void return_default_state_for_different_entities() {
        assertEquals(Project.getDefaultInstance(), entityNew.getDefaultState());

        final EntityWithMessageId entityWithMessageId = new EntityWithMessageId();
        final StringValue expected = StringValue.getDefaultInstance();
        assertEquals(expected, entityWithMessageId.getDefaultState());
    }

    @Test
    public void accept_String_id_to_constructor() {
        final String stringId = "stringId";
        final TestEntityWithIdString entityWithStringId = new TestEntityWithIdString(stringId);

        assertEquals(stringId, entityWithStringId.getId());
    }

    @Test
    public void accept_Long_id_to_constructor() {
        final Long longId = 12L;
        final TestEntityWithIdLong entityWithLongId = new TestEntityWithIdLong(longId);

        assertEquals(longId, entityWithLongId.getId());
    }

    @Test
    public void accept_Integer_id_to_constructor() {
        final Integer integerId = 12;
        final TestEntityWithIdInteger entityWithIntegerId = new TestEntityWithIdInteger(integerId);

        assertEquals(integerId, entityWithIntegerId.getId());
    }

    @Test
    public void accept_Message_id_to_constructor() {
        final StringValue messageId = newStringValue("messageId");
        final TestEntityWithIdMessage entityWithMessageID = new TestEntityWithIdMessage(messageId);

        assertEquals(messageId, entityWithMessageID.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void not_accept_to_constructor_id_of_unsupported_type() {
        new TestEntityWithIdUnsupported(new UnsupportedClassVersionError());
    }

    private static class TestEntityWithIdUnsupported extends Entity<UnsupportedClassVersionError, Project> {
        private TestEntityWithIdUnsupported(UnsupportedClassVersionError id) {
            super(id);
        }
    }

    private static class TestEntityWithIdString extends Entity <String, Project> {
        private TestEntityWithIdString(String id) {
            super(id);
        }
    }

    private static class TestEntityWithIdMessage extends Entity <Message, Project> {
        private TestEntityWithIdMessage(Message id) {
            super(id);
        }
    }

    private static class TestEntityWithIdInteger extends Entity <Integer, Project> {
        private TestEntityWithIdInteger(Integer id) {
            super(id);
        }
    }

    private static class TestEntityWithIdLong extends Entity <Long, Project> {
        private TestEntityWithIdLong(Long id) {
            super(id);
        }
    }

    @Test
    public void have_state() {
        final int version = 3;
        final Timestamp whenModified = getCurrentTime();

        entityNew.setState(state, version, whenModified);

        assertEquals(state, entityNew.getState());
        assertEquals(version, entityNew.getVersion());
        assertEquals(whenModified, entityNew.whenModified());
    }

    @Test
    public void validate_state_when_set_it() {
        final TestEntity spyEntityNew = spy(entityNew);
        spyEntityNew.setState(state, 0, getCurrentTime());
        verify(spyEntityNew).validate(eq(state));
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_set_null_state() {
        entityNew.setState(Tests.<Project>nullRef(), 0, getCurrentTime());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_set_null_modification_time() {
        entityNew.setState(state, 0, Tests.<Timestamp>nullRef());
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings({"ResultOfObjectAllocationIgnored", "NewExceptionWithoutArguments"})
    public void throw_exception_if_try_to_create_entity_with_id_of_unsupported_type() {
        new EntityWithUnsupportedId(new Exception());
    }


    private static class BareBonesEntity extends Entity<Long, StringValue> {
        private BareBonesEntity(Long id) {
            super(id);
        }
    }

    @Test
    public void have_zero_version_by_default() {
        assertEquals(0, entityNew.getVersion());
    }

    @Test
    public void increment_version_by_one() {
        final int version = entityNew.incrementVersion();
        assertEquals(1, version);
    }

    @Test
    public void record_modification_time_when_incrementing_version() {
        final long timeBeforeincrement = currentTimeSeconds();
        entityNew.incrementVersion();
        final long timeAfterIncrement = currentTimeSeconds();
        assertThat(entityNew.whenModified().getSeconds(), isBetween(timeBeforeincrement, timeAfterIncrement));
    }

    @Test
    public void update_state() {
        entityNew.incrementState(state);

        assertEquals(state, entityNew.getState());
    }

    @Test
    public void increment_version_when_updating_state() {
        entityNew.incrementState(state);

        assertEquals(1, entityNew.getVersion());
    }

    @Test
    public void record_modification_time_when_updating_state() {
        entityNew.incrementState(state);
        final long expectedTimeSec = currentTimeSeconds();

        assertEquals(expectedTimeSec, entityNew.whenModified()
                                               .getSeconds());
    }

    @Test
    public void return_id_class() {
        final Class<String> actual = EntityLite.TypeInfo.getIdClass(TestEntity.class);

        assertEquals(String.class, actual);
    }

    @Test
    public void generate_non_zero_hash_code_if_entity_has_non_empty_id_and_state() {
        assertFalse(entityWithState.getId().trim().isEmpty());

        final int hashCode = entityWithState.hashCode();

        assertTrue(hashCode != 0);
    }

    @Test
    public void generate_same_hash_code_for_one_instance() {
        assertEquals(entityWithState.hashCode(), entityWithState.hashCode());
    }

    @Test
    public void generate_unique_hash_code_for_different_instances() {
        final TestEntity another = TestEntity.withState();

        assertNotEquals(entityWithState.hashCode(), another.hashCode());
    }

    private static class EntityWithUnsupportedId extends Entity<Exception, Project> {

        protected EntityWithUnsupportedId(Exception id) {
            super(id);
        }
    }

    private static class EntityWithMessageId extends Entity<ProjectId, StringValue> {

        protected EntityWithMessageId() {
            super(Sample.messageOfType(ProjectId.class));
        }
    }

    @Test
    public void obtain_entity_constructor_by_class_and_ID_class() {
        final Constructor<BareBonesEntity> ctor = Entity.getConstructor(BareBonesEntity.class,
                                                                        Long.class);

        assertNotNull(ctor);
    }

    @Test
    public void create_and_initialize_entity_instance() {
        final Long id = 100L;
        final Timestamp before = Timestamps.secondsAgo(1);

        // Create and init the entity.
        final Constructor<BareBonesEntity> ctor = Entity.getConstructor(BareBonesEntity.class, Long.class);
        final Entity<Long, StringValue> entity = Entity.createEntity(ctor, id);

        final Timestamp after = Timestamps.getCurrentTime();

        // The interval with a much earlier start to allow non-zero interval on faster computers.
        final Interval whileWeCreate = Intervals.between(before, after);

        assertEquals(id, entity.getId());
        assertEquals(0, entity.getVersion());
        assertTrue(Intervals.contains(whileWeCreate, entity.whenModified()));
        assertEquals(StringValue.getDefaultInstance(), entity.getState());
        assertFalse(entity.isArchived());
        assertFalse(entity.isDeleted());
    }

    private static Matcher<Long> isBetween(final Long lower, final Long higher) {
        return new BaseMatcher<Long>() {
            @Override
            public boolean matches(Object o) {
                assertThat(o, instanceOf(Long.class));
                final Long number = (Long) o;
                return number >= lower && number <= higher;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(" must be between " + lower + " and " + higher +  ' ');
            }
        };
    }
}

