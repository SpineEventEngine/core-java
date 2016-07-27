/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.junit.Test;
import org.spine3.test.Tests;
import org.spine3.test.entity.Project;
import org.spine3.test.entity.ProjectId;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.test.Tests.currentTimeSeconds;
import static org.spine3.test.Tests.nullRef;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class EntityShould {

    private final Project state = Given.newProject();

    private final Given.TestEntity entityNew = Given.TestEntity.newInstance(newUuid());

    private final Given.TestEntity entityWithState = Given.TestEntity.withState();

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
    public void set_id_in_constructor() {
        final String id = newUuid();

        final Given.TestEntity entity = Given.TestEntity.newInstance(id);

        assertEquals(id, entity.getId());
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
        entityNew.setState(state, 0, TimeUtil.getCurrentTime());

        assertTrue(entityNew.isValidateMethodCalled());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_set_null_state() {
        entityNew.setState(Tests.<Project>nullRef(), 0, TimeUtil.getCurrentTime());
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

    @Test
    public void set_default_state() {
        final Given.TestEntity entity = Given.TestEntity.withState();
        entity.setDefault();
        final long expectedTimeSec = currentTimeSeconds();

        assertEquals(entity.getDefaultState(), entity.getState());
        assertEquals(expectedTimeSec, entity.whenModified()
                                            .getSeconds());
        assertEquals(0, this.entityNew.getVersion());
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
        entityNew.incrementVersion();
        final long expectedTimeSec = currentTimeSeconds();

        assertEquals(expectedTimeSec, entityNew.whenModified()
                                            .getSeconds());
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
        final Class<String> actual = Entity.getIdClass(Given.TestEntity.class);

        assertEquals(String.class, actual);
    }

    @Test
    public void return_id_simple_class_name() {
        final String expected = entityNew.getId()
                                      .getClass()
                                      .getSimpleName();
        final String actual = entityNew.getShortIdTypeName();
        assertEquals(expected, actual);
    }

    @Test
    public void return_id_protobuf_type_name() {
        final EntityWithMessageId entityWithMessageId = new EntityWithMessageId();
        final String expected = ProjectId.getDescriptor()
                                         .getName();
        final String actual = entityWithMessageId.getShortIdTypeName();
        assertEquals(expected, actual);
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
        final Given.TestEntity another = Given.TestEntity.withState();

        assertNotEquals(entityWithState.hashCode(), another.hashCode());
    }

    @Test
    public void assure_same_entities_are_equal() {
        final Given.TestEntity another = Given.TestEntity.withState(entityWithState);

        assertTrue(entityWithState.equals(another));
    }

    @Test
    public void assure_entity_is_equal_to_itself() {
        // noinspection EqualsWithItself
        assertTrue(entityWithState.equals(entityWithState));
    }

    @Test
    public void assure_entity_is_not_equal_to_null() {
        assertFalse(entityWithState.equals(nullRef()));
    }

    @Test
    public void assure_entity_is_not_equal_to_object_of_another_class() {
        //noinspection EqualsBetweenInconvertibleTypes
        assertFalse(entityWithState.equals(newUuid()));
    }

    @Test
    public void assure_entities_with_different_ids_are_not_equal() {
        final Given.TestEntity another = Given.TestEntity.newInstance(newUuid());

        assertNotEquals(entityWithState.getId(), another.getId());
        assertFalse(entityWithState.equals(another));
    }

    @Test
    public void assure_entities_with_different_states_are_not_equal() {
        final Given.TestEntity another = Given.TestEntity.withState(entityWithState);
        another.setState(Given.newProject(), another.getVersion(), another.whenModified());

        assertNotEquals(entityWithState.getState(), another.getState());
        assertFalse(entityWithState.equals(another));
    }

    @Test
    public void assure_entities_with_different_versions_are_not_equal() {
        final Given.TestEntity another = Given.TestEntity.withState(entityWithState);
        another.setVersion(entityWithState.getVersion() + 5, another.whenModified());

        assertFalse(entityWithState.equals(another));
    }

    @Test
    public void assure_entities_with_different_modification_times_are_not_equal() {
        final Given.TestEntity another = Given.TestEntity.withState(entityWithState);
        another.setVersion(another.getVersion(), Timestamp.newBuilder().setSeconds(5).build());

        assertNotEquals(entityWithState.whenModified(), another.whenModified());
        assertFalse(entityWithState.equals(another));
    }

    private static class EntityWithUnsupportedId extends Entity<Exception, Project> {

        protected EntityWithUnsupportedId(Exception id) {
            super(id);
        }
    }

    private static class EntityWithMessageId extends Entity<ProjectId, StringValue> {

        protected EntityWithMessageId() {
            super(Given.AggregateId.newProjectId());
        }
    }
}

