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
import org.junit.Before;
import org.junit.Test;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;
import org.spine3.testdata.TestEntity;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.test.Tests.currentTimeSeconds;
import static org.spine3.testdata.TestAggregateIdFactory.newProjectId;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class EntityShould {

    private static final String ID = newUuid();
    private final Project state = newProject();

    private TestEntity entity = new TestEntity(ID);

    @Before
    public void setUp() {
        entity = new TestEntity(ID);
    }

    @Test
    public void return_default_state() {
        final Project state = entity.getDefaultState();
        assertEquals(Project.getDefaultInstance(), state);
    }

    @Test
    public void return_default_state_for_different_entities() {
        assertEquals(Project.getDefaultInstance(), entity.getDefaultState());

        final EntityWithMessageId entityWithMessageId = new EntityWithMessageId();
        final StringValue expected = StringValue.getDefaultInstance();
        assertEquals(expected, entityWithMessageId.getDefaultState());
    }

    @Test
    public void set_id_in_constructor() {
        assertEquals(ID, entity.getId());
    }

    @Test
    public void store_state() {
        final Project state = newProject();
        final Timestamp whenLastModified = getCurrentTime();
        final int version = 3;

        entity.setState(state, version, whenLastModified);

        assertEquals(state, entity.getState());
        assertEquals(whenLastModified, entity.whenModified());
        assertEquals(version, entity.getVersion());
    }

    @Test
    public void validate_state_when_set_it() {
        entity.setState(state, 0, TimeUtil.getCurrentTime());
        assertTrue(entity.isValidateMethodCalled());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_set_null_state() {
        // noinspection ConstantConditions
        entity.setState(null, 0, TimeUtil.getCurrentTime());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_set_null_modification_time() {
        // noinspection ConstantConditions
        entity.setState(state, 0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings({"ResultOfObjectAllocationIgnored", "NewExceptionWithoutArguments"})
    public void throw_exception_if_try_to_create_entity_with_id_of_unsupported_type() {
        new EntityWithUnsupportedId(new Exception());
    }

    @Test
    public void set_default_state() {
        entity.incrementState(newProject());
        entity.setDefault();
        final long expectedTimeSec = currentTimeSeconds();

        assertEquals(entity.getDefaultState(), entity.getState());
        assertEquals(expectedTimeSec, entity.whenModified()
                                            .getSeconds());
        assertEquals(0, entity.getVersion());
    }

    @Test
    public void have_zero_version_by_default() {
        assertEquals(0, entity.getVersion());
    }

    @Test
    public void increment_version_by_one() {
        final int version = entity.incrementVersion();
        assertEquals(1, version);
    }

    @Test
    public void record_modification_time_when_incrementing_version() {
        entity.incrementVersion();
        final long expectedTimeSec = currentTimeSeconds();

        assertEquals(expectedTimeSec, entity.whenModified()
                                            .getSeconds());
    }

    @Test
    public void update_state() {
        final Project newState = newProject();
        entity.incrementState(newState);
        assertEquals(newState, entity.getState());
    }

    @Test
    public void increment_version_when_updating_state() {
        entity.incrementState(state);
        assertEquals(1, entity.getVersion());
    }

    @Test
    public void record_modification_time_when_updating_state() {
        entity.incrementState(state);
        final long expectedTimeSec = currentTimeSeconds();

        assertEquals(expectedTimeSec, entity.whenModified()
                                            .getSeconds());
    }

    @Test
    public void return_id_class() {
        final Class<String> actual = Entity.getIdClass(TestEntity.class);

        assertEquals(String.class, actual);
    }

    @Test
    public void return_id_simple_class_name() {
        final String expected = entity.getId()
                                      .getClass()
                                      .getSimpleName();
        final String actual = entity.getShortIdTypeName();
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

    private static Project newProject() {
        final Project.Builder project = Project.newBuilder()
                                               .setProjectId(newProjectId())
                                               .setStatus(newUuid());
        return project.build();
    }

    private static class EntityWithUnsupportedId extends Entity<Exception, Project> {

        protected EntityWithUnsupportedId(Exception id) {
            super(id);
        }
    }

    private static class EntityWithMessageId extends Entity<ProjectId, StringValue> {

        protected EntityWithMessageId() {
            super(newProjectId());
        }
    }
}

