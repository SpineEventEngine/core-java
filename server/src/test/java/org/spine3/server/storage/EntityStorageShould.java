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

import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.Identifiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.spine3.testdata.TestEntityStorageRecordFactory.newEntityStorageRecord;

@SuppressWarnings("InstanceMethodNamingConvention")
public abstract class EntityStorageShould {

    private EntityStorage<String> storage;

    @Before
    public void setUpTest() {
        storage = getStorage();
    }

    protected abstract EntityStorage<String> getStorage();

    @Test
    public void return_null_if_read_one_record_from_empty_storage() {
        final Message message = storage.read("nothing");
        assertNull(message);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_with_null_id() {
        //noinspection ConstantConditions
        storage.write(null, EntityStorageRecord.getDefaultInstance());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_record() {
        storage.write(Identifiers.newUuid(), null);
    }

    @Test
    public void write_and_read_message() {
        testWriteAndReadMessage("testId");
    }

    @Test
    public void write_and_read_several_messages_with_different_ids() {
        testWriteAndReadMessage("id-1");
        testWriteAndReadMessage("id-2");
        testWriteAndReadMessage("id-3");
    }

    @Test
    public void rewrite_message_if_write_with_same_id() {
        final String id = "test-id-rewrite";
        testWriteAndReadMessage(id);
        testWriteAndReadMessage(id);
    }

    private void testWriteAndReadMessage(String id) {
        final EntityStorageRecord expected = newEntityStorageRecord();
        storage.write(id, expected);

        final EntityStorageRecord actual = storage.read(id);

        assertEquals(expected, actual);
    }
}
