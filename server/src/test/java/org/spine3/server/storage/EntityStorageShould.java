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

package org.spine3.server.storage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.testdata.TestEntityStorageRecordFactory.newEntityStorageRecord;

@SuppressWarnings("InstanceMethodNamingConvention")
public abstract class EntityStorageShould {

    private EntityStorage<String> storage;

    @Before
    public void setUpTest() {
        storage = getStorage();
    }

    @After
    public void tearDownTest() throws Exception {
        storage.close();
    }

    /**
     * Used to initialize the storage before each test.
     *
     * @return an empty storage instance
     */
    protected abstract EntityStorage<String> getStorage();

    @Test
    public void return_null_if_no_record_with_such_id() {
        final EntityStorageRecord record = storage.read("nothing");
        assertNull(record);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_read_by_null_id() {
        //noinspection ConstantConditions
        storage.read(null);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_by_null_id() {
        //noinspection ConstantConditions
        storage.write(null, EntityStorageRecord.getDefaultInstance());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_record() {
        storage.write(newUuid(), null);
    }

    @Test
    public void write_and_read_record() {
        writeAndReadRecordTest();
    }

    @Test
    public void write_and_read_several_records_by_different_ids() {
        writeAndReadRecordTest();
        writeAndReadRecordTest();
        writeAndReadRecordTest();
    }

    @Test
    public void rewrite_record_if_write_by_the_same_id() {
        final String id = "test-id-rewrite";
        writeAndReadRecordTest(id);
        writeAndReadRecordTest(id);
    }

    private void writeAndReadRecordTest() {
        writeAndReadRecordTest(newUuid());
    }

    private void writeAndReadRecordTest(String id) {
        final EntityStorageRecord expected = newEntityStorageRecord();
        storage.write(id, expected);

        final EntityStorageRecord actual = storage.read(id);

        assertEquals(expected, actual);
    }
}
