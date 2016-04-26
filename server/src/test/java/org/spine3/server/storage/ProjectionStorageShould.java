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
import static org.spine3.testdata.TestEntityStorageRecordFactory.newEntityStorageRecord;

/**
 * Projection storage tests.
 *
 * @param <I> the type of IDs of storage records
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public abstract class ProjectionStorageShould<I> extends AbstractStorageShould<I, EntityStorageRecord> {

    private ProjectionStorage<I> storage;

    @Before
    public void setUpProjectionStorageTest() {
        storage = getStorage();
    }

    @After
    public void tearDownProjectionStorageTest() {
        close(storage);
    }

    @Override
    protected abstract ProjectionStorage<I> getStorage();

    @Override
    protected EntityStorageRecord newStorageRecord() {
        return newEntityStorageRecord();
    }

    @Test
    public void return_null_if_no_event_time_in_storage() {
        final Timestamp time = storage.readLastHandledEventTime();

        assertNull(time);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_event_time() {
        //noinspection ConstantConditions
        storage.writeLastHandledEventTime(null);
    }

    @Test
    public void write_and_read_last_event_time() {
        writeAndReadLastEventTimeTest(getCurrentTime());
    }

    @Test
    public void write_and_read_last_event_time_several_times() {
        final Timestamp time1 = getCurrentTime();
        final Timestamp time2 = add(time1, Durations.ofSeconds(10));
        writeAndReadLastEventTimeTest(time1);
        writeAndReadLastEventTimeTest(time2);
    }

    private void writeAndReadLastEventTimeTest(Timestamp expected) {
        storage.writeLastHandledEventTime(expected);

        final Timestamp actual = storage.readLastHandledEventTime();

        assertEquals(expected, actual);
    }
}
