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
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.protobuf.Durations;

import static com.google.protobuf.util.TimeUtil.add;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.spine3.server.util.Identifiers.newUuid;
import static org.spine3.testdata.TestEntityStorageRecordFactory.newEntityStorageRecord;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public abstract class StreamProjectionStorageShould {

    private StreamProjectionStorage<String> storage;

    @Before
    public void setUpTest() {
        storage = getStorage();
    }

    protected abstract StreamProjectionStorage<String> getStorage();

    @Test
    public void return_null_if_read_one_record_from_empty_storage() {
        final Message message = storage.read(newUuid());
        assertNull(message);
    }

    @Test
    public void return_null_if_read_event_time_from_empty_storage() {
        final Timestamp time = storage.readLastHandledEventTime();
        assertNull(time);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_with_null_id() {
        //noinspection ConstantConditions
        storage.write(null, EntityStorageRecord.getDefaultInstance());
    }

    @SuppressWarnings("ConstantConditions")
     @Test(expected = NullPointerException.class)
     public void throw_exception_if_write_null_record() {
        storage.write(newUuid(), null);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_event_time() {
        storage.writeLastHandledEventTime(null);
    }

    @Test
    public void write_and_read_message() {
        testWriteAndReadMessage(newUuid());
    }

    @Test
    public void write_and_read_last_event_time() {
        testWriteAndReadLastEventTime(getCurrentTime());
    }

    @Test
    public void write_and_read_several_messages_with_different_ids() {
        testWriteAndReadMessage("id-1");
        testWriteAndReadMessage("id-2");
        testWriteAndReadMessage("id-3");
    }

    @Test
    public void write_and_read_last_event_time_several_times() {
        final Timestamp time1 = getCurrentTime();
        final Timestamp time2 = add(time1, Durations.ofSeconds(10));
        testWriteAndReadLastEventTime(time1);
        testWriteAndReadLastEventTime(time2);
    }

    @Test
    public void rewrite_message_if_write_with_same_id() {
        final String id = "testIdRewrite";
        testWriteAndReadMessage(id);
        testWriteAndReadMessage(id);
    }

    private void testWriteAndReadMessage(String id) {
        final EntityStorageRecord expected = newEntityStorageRecord();
        storage.write(id, expected);

        final EntityStorageRecord actual = storage.read(id);

        assertEquals(expected, actual);
    }

    private void testWriteAndReadLastEventTime(Timestamp expected) {
        storage.writeLastHandledEventTime(expected);

        final Timestamp actual = storage.readLastHandledEventTime();

        assertEquals(expected, actual);
    }
}

