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

package org.spine3.server.storage.filesystem;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.junit.*;
import org.spine3.server.storage.AggregateStorageRecord;
import org.spine3.test.project.ProjectId;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.*;
import static org.spine3.server.storage.filesystem.Helper.cleanTestData;
import static org.spine3.server.storage.filesystem.Helper.configure;

/**
 * @author Mikhail Mikhaylov
 */
// TODO[alexander.litus]: impl storage.releaseResources method to fix tests
@Ignore
@SuppressWarnings({"InstanceMethodNamingConvention", "DuplicateStringLiteralInspection", "ConstantConditions"})
public class FileSystemAggregateStorageShould {

    private static final String AGGREGATE_ID = "aggregateId";

    private static final ProjectId PROJECT_ID = ProjectId.newBuilder().setId(AGGREGATE_ID).build();

    @SuppressWarnings("unchecked")
    private static final FileSystemAggregateStorage<ProjectId> STORAGE =
            new FileSystemAggregateStorage(ProjectId.getDescriptor().getName());

    @Before
    public void setUpTest() {
        STORAGE.releaseResources();
        cleanTestData();
        configure(FileSystemAggregateStorageShould.class);
    }

    @After
    public void tearDownTest() {
        STORAGE.releaseResources();
        cleanTestData();
    }

    @AfterClass
    public static void tearDownClass() {
        Helper.cleanTestData();
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_history_from_empty_storage() {

        final Iterator<AggregateStorageRecord> iterator = STORAGE.historyBackward(PROJECT_ID);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_by_null_id() {

        final Iterator<AggregateStorageRecord> iterator = STORAGE.historyBackward(null);
        assertFalse(iterator.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null_record() {
        STORAGE.write(null);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_record_with_null_aggregate_id() {

        final AggregateStorageRecord record = AggregateStorageRecord.newBuilder().setAggregateId(null).build();
        STORAGE.write(record);
    }

    @Test
    public void save_and_read_one_record() {

        final AggregateStorageRecord expected = newAggregateStorageRecord(getCurrentTime(), PROJECT_ID.getId());
        STORAGE.write(expected);

        final Iterator<AggregateStorageRecord> iterator = STORAGE.historyBackward(PROJECT_ID);

        assertTrue(iterator.hasNext());

        final AggregateStorageRecord actual = iterator.next();

        assertEquals(expected, actual);
    }

    @Test
    public void save_records_and_return_sorted_by_timestamp_descending() {

        final List<AggregateStorageRecord> records = getSequentialRecords(PROJECT_ID.getId());

        for (AggregateStorageRecord record : records) {
            STORAGE.write(record);
        }

        final Iterator<AggregateStorageRecord> iterator = STORAGE.historyBackward(PROJECT_ID);
        final List<AggregateStorageRecord> actual = newArrayList(iterator);

        Collections.reverse(records); // expected records should be in reverse order

        assertEquals(records, actual);
    }

    /*
     * Returns records sorted by timestamp ascending
     */
    private static List<AggregateStorageRecord> getSequentialRecords(String aggregateId) {

        final Duration delta = Duration.newBuilder().setSeconds(10).build();

        final Timestamp timestampFirst = getCurrentTime();
        final Timestamp timestampSecond = TimeUtil.add(timestampFirst, delta);
        final Timestamp timestampLast = TimeUtil.add(timestampSecond, delta);

        final AggregateStorageRecord recordFirst = newAggregateStorageRecord(timestampFirst, aggregateId);
        final AggregateStorageRecord recordSecond = newAggregateStorageRecord(timestampSecond, aggregateId);
        final AggregateStorageRecord recordLast = newAggregateStorageRecord(timestampLast, aggregateId);

        return newArrayList(recordFirst, recordSecond, recordLast);
    }

    private static AggregateStorageRecord newAggregateStorageRecord(Timestamp timestamp, String aggregateId) {
        final AggregateStorageRecord.Builder builder = AggregateStorageRecord.newBuilder()
                .setAggregateId(aggregateId)
                .setTimestamp(timestamp);
        return builder.build();
    }
}

