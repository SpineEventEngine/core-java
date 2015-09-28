/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server;

import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.test.project.Project;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.*;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "ResultOfObjectAllocationIgnored", "MagicNumber",
"ClassWithTooManyMethods", "ReturnOfNull", "DuplicateStringLiteralInspection", "ConstantConditions"})
public class EntityShould {

    private static final Project stubState = Project.newBuilder().setStatus("stub_state").build();
    private static final String TEST_ID = "test_id";

    private TestEntity entity;

    @Before
    public void setUp() {
        entity = new TestEntity(TEST_ID);
    }

    @Test
    public void have_null_fields_by_default() {

        assertNull(entity.getState());
        assertNull(entity.whenModified());
        assertEquals(0, entity.getVersion());
    }

    @Test
    public void set_id_in_constructor() {
        assertEquals(TEST_ID, entity.getId());
    }

    @Test
    public void store_state() {

        final Project state = Project.newBuilder().setStatus("status").build();
        final Timestamp whenLastModified = getCurrentTime();
        final int version = 3;

        entity.setState(state, version, whenLastModified);

        assertEquals(state, entity.getState());
        assertEquals(whenLastModified, entity.whenModified());
        assertEquals(version, entity.getVersion());
    }

    @Test
    public void validate_state_when_set_it() {

        entity.setState(stubState, 0, null);
        assertTrue(entity.isValidateMethodCalled);
    }

    @Test
    public void set_default_state() {

        entity.setDefault();
        final long expectedTimeSec = currentTimeMillis() / 1000L;

        assertEquals(entity.getDefaultState(), entity.getState());
        assertEquals(expectedTimeSec, entity.whenModified().getSeconds());
        assertEquals(0, entity.getVersion());
    }

    @Test
    public void increment_version_by_one() {

        assertEquals(0, entity.getVersion());
        final int version = entity.incrementVersion();
        assertEquals(1, version);
    }

    @Test
    public void record_modification_time_when_incrementing_version() {

        entity.incrementVersion();
        final long expectedTimeSec = currentTimeMillis() / 1000L;

        assertEquals(expectedTimeSec, entity.whenModified().getSeconds());
    }

    @Test
    public void update_state() {

        final Project newState = Project.newBuilder().setStatus("new_state").build();
        entity.incrementState(newState);
        assertEquals(newState, entity.getState());
    }

    @Test
    public void increment_version_when_updating_state() {

        entity.incrementState(stubState);
        assertEquals(1, entity.getVersion());
    }

    @Test
    public void record_modification_time_when_updating_state() {

        entity.incrementState(stubState);
        final long expectedTimeSec = currentTimeMillis() / 1000L;

        assertEquals(expectedTimeSec, entity.whenModified().getSeconds());
    }


    public static class TestEntity extends Entity<String, Project> {

        private static final Project DEFAULT_STATE = Project.newBuilder().setStatus("default state").build();
        private boolean isValidateMethodCalled = false;

        protected TestEntity(String id) {
            super(id);
        }

        @Override
        protected Project getDefaultState() {
            return DEFAULT_STATE;
        }

        @Override
        protected void validate(Project state) throws IllegalStateException {
            super.validate(state);
            isValidateMethodCalled = true;
        }
    }
}
