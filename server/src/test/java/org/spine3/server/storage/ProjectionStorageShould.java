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

import com.google.protobuf.Timestamp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.protobuf.Durations;

import static com.google.protobuf.util.TimeUtil.add;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.testutil.TestEntityStorageRecordFactory.newEntityStorageRecord;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public abstract class ProjectionStorageShould {

    private ProjectionStorage<String> storage;

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
    protected abstract ProjectionStorage<String> getStorage();

    @Test
    public void return_null_if_no_record_with_such_id() {
        final EntityStorageRecord record = storage.read(newUuid());
        assertNull(record);
    }

    @Test
    public void return_null_if_no_event_time_in_storage() {
        final Timestamp time = storage.readLastHandledEventTime();
        assertNull(time);
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

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_record() {
        //noinspection ConstantConditions
        storage.write(newUuid(), null);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_event_time() {
        //noinspection ConstantConditions
        storage.writeLastHandledEventTime(null);
    }

    @Test
    public void write_and_read_record() {
        writeAndReadRecordTest();
    }

    @Test
    public void write_and_read_last_event_time() {
        writeAndReadLastEventTimeTest(getCurrentTime());
    }

    @Test
    public void write_and_read_several_records_by_different_ids() {
        writeAndReadRecordTest();
        writeAndReadRecordTest();
        writeAndReadRecordTest();
    }

    @Test
    public void write_and_read_last_event_time_several_times() {
        final Timestamp time1 = getCurrentTime();
        final Timestamp time2 = add(time1, Durations.ofSeconds(10));
        writeAndReadLastEventTimeTest(time1);
        writeAndReadLastEventTimeTest(time2);
    }

    @Test
    public void rewrite_record_if_write_with_same_id() {
        final String id = "testIdRewrite";
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

    private void writeAndReadLastEventTimeTest(Timestamp expected) {
        storage.writeLastHandledEventTime(expected);

        final Timestamp actual = storage.readLastHandledEventTime();

        assertEquals(expected, actual);
    }
}

