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

package org.spine3.server.storage;

import org.junit.Test;
import org.spine3.server.entity.Entity;
import org.spine3.test.storage.Project;
import org.spine3.test.storage.ProjectId;

import static org.junit.Assert.assertEquals;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.testdata.TestEntityStorageRecordFactory.newEntityStorageRecord;

/**
 * Entity storage tests.
 *
 * @param <I> the type of Entity IDs
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public abstract class EntityStorageShould<I> extends AbstractStorageShould<I, EntityStorageRecord> {

    @Override
    protected abstract RecordStorage<I> getStorage();

    /**
     * Used to get a storage in tests with different ID types.
     *
     * <p>NOTE: the storage is closed after each test.
     *
     * @param <Id> the type of Entity IDs
     * @return an empty storage instance
     */
    protected abstract <Id> RecordStorage<Id> getStorage(Class<? extends Entity<Id, ?>> entityClass);

    @Override
    protected EntityStorageRecord newStorageRecord() {
        return newEntityStorageRecord();
    }

    @Test
    public void write_and_read_record_by_Message_id() {
        final RecordStorage<ProjectId> storage = getStorage(TestEntityWithIdMessage.class);
        final ProjectId id = Given.AggregateId.newProjectId(newUuid());
        writeAndReadRecordTest(id, storage);
    }

    @Test
    public void write_and_read_record_by_String_id() {
        final RecordStorage<String> storage = getStorage(TestEntityWithIdString.class);
        final String id = newUuid();
        writeAndReadRecordTest(id, storage);
    }

    @Test
    public void write_and_read_record_by_Long_id() {
        final RecordStorage<Long> storage = getStorage(TestEntityWithIdLong.class);
        final long id = 10L;
        writeAndReadRecordTest(id, storage);
    }

    @Test
    public void write_and_read_record_by_Integer_id() {
        final RecordStorage<Integer> storage = getStorage(TestEntityWithIdInteger.class);
        final int id = 10;
        writeAndReadRecordTest(id, storage);
    }

    protected <Id> void writeAndReadRecordTest(Id id, RecordStorage<Id> storage) {
        final EntityStorageRecord expected = newStorageRecord();
        storage.write(id, expected);

        final EntityStorageRecord actual = storage.read(id);

        assertEquals(expected, actual);
        close(storage);
    }

    private static class TestEntityWithIdMessage extends Entity<ProjectId, Project> {
        private TestEntityWithIdMessage(ProjectId id) {
            super(id);
        }
    }

    private static class TestEntityWithIdString extends Entity<String, Project> {
        private TestEntityWithIdString(String id) {
            super(id);
        }
    }

    private static class TestEntityWithIdInteger extends Entity<Integer, Project> {
        private TestEntityWithIdInteger(Integer id) {
            super(id);
        }
    }

    private static class TestEntityWithIdLong extends Entity<Long, Project> {
        private TestEntityWithIdLong(Long id) {
            super(id);
        }
    }
}
