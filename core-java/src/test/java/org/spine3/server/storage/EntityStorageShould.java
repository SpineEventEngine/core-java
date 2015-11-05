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

package org.spine3.server.storage;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.server.Entity;
import org.spine3.test.project.ProjectId;
import org.spine3.testdata.TestAggregateIdFactory;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertEquals;
import static org.spine3.testdata.TestAggregateIdFactory.createProjectId;

@SuppressWarnings({"InstanceMethodNamingConvention", "AbstractClassWithoutAbstractMethods",
        "ConstructorNotProtectedInAbstractClass", "DuplicateStringLiteralInspection"})
public abstract class EntityStorageShould {

    private final EntityStorage<String, ProjectId> storage;

    public EntityStorageShould(EntityStorage<String, ProjectId> storage) {
        this.storage = storage;
    }


    @Test
    public void return_empty_message_if_read_one_record_from_empty_storage() {

        final Message message = storage.read("nothing");
        assertEquals(Any.getDefaultInstance(), message);
    }

    @Test
    public void return_empty_message_if_read_one_record_by_null_id() {
        //noinspection ConstantConditions
        final Message message = storage.read(null);
        assertEquals(Any.getDefaultInstance(), message);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_by_null_id() {
        //noinspection ConstantConditions
        storage.write(null, TestAggregateIdFactory.createProjectId("testId"));
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_message() {
        //noinspection ConstantConditions
        storage.write("nothing", null);
    }

    @Test
    public void save_and_read_message() {
        testSaveAndReadMessage("testId");
    }

    @Test
    public void save_and_read_several_messages() {

        testSaveAndReadMessage("testId_1");
        testSaveAndReadMessage("testId_2");
        testSaveAndReadMessage("testId_3");
    }

    private void testSaveAndReadMessage(String messageId) {

        final ProjectId expected = TestAggregateIdFactory.createProjectId(messageId);

        storage.write(expected.getId(), expected);

        final ProjectId actual = storage.read(expected.getId());

        assertEquals(expected, actual);
    }

    public static class TestEntity extends Entity<String, ProjectId> {

        private static final ProjectId DEFAULT_STATE = createProjectId("defaultProjectId");

        protected TestEntity(String id) {
            super(id);
        }

        @Nonnull
        @Override
        protected ProjectId getDefaultState() {
            return DEFAULT_STATE;
        }

        @Override
        protected void validate(ProjectId state) throws IllegalStateException {
            super.validate(state);
        }
    }
}
