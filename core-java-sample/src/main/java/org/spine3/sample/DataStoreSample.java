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

package org.spine3.sample;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.TypeName;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.gae.datastore.DataStoreStorageFactory;
import org.spine3.sample.order.Order;
import org.spine3.server.SnapshotStorage;
import org.spine3.server.StorageWithTimeline;
import org.spine3.server.StorageWithTimelineAndVersion;

/**
 * Entry point for core-java sample without gRPC. Works with DataStore.
 *
 * @author Mikhail Mikhaylov
 */
public class DataStoreSample extends BaseSample {

    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    public static void main(String[] args) {
        // we use it as DataStoreSample instead of BaseSample here to have an access to helper.
        DataStoreSample sample = new DataStoreSample();

        sample.helper.setUp();

        sample.execute();

        sample.helper.tearDown();
    }

    @Override
    protected Logger getLog() {
        return LogSingleton.INSTANCE.value;
    }

    @Override
    protected StorageWithTimelineAndVersion<EventRecord> provideEventStoreStorage() {
        return DataStoreStorageFactory.createEventStore();
    }

    @Override
    protected StorageWithTimeline<CommandRequest> provideCommandStoreStorage() {
        return DataStoreStorageFactory.createCommandStore();
    }

    /**
     * Provides snapshot storage for sample.
     * NOTE: if we would have a few different Aggregate Root Repositories, we should provide them with Snapshot
     * Storages with different type names.
     *
     * @return SnapshotStorage for Aggregate Root Repository
     */
    @Override
    protected SnapshotStorage provideSnapshotStorage() {
        return DataStoreStorageFactory.createSnapshotStorage(TypeName.of(Order.getDescriptor()));
    }

    private DataStoreSample() {
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(DataStoreSample.class);
    }

}
