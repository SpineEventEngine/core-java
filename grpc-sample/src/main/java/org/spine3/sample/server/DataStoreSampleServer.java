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

package org.spine3.sample.server;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.TypeName;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.gae.datastore.DataStoreStorageFactory;
import org.spine3.sample.order.Order;
import org.spine3.server.MessageJournal;
import org.spine3.server.aggregate.SnapshotStorage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.datastore.DatastoreStorageFactory;

/**
 * File system Server implementation.
 * No main method provided because of requirement to initialize
 * helper for each working with data store thread.
 *
 * @author Mikhail Mikhaylov
 */
public class DataStoreSampleServer extends BaseSampleServer {

    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    @Override
    protected void start() throws Exception {
        helper.setUp();
        super.start();
    }

    @Override
    protected void stop() {
        super.stop();
        helper.tearDown();
    }

    @Override
    protected StorageFactory getStorageFactory() {
        return new DatastoreStorageFactory();
    }

    @Override
    protected Logger getLog() {
        return LogSingleton.INSTANCE.value;
    }

    @Override
    protected MessageJournal<String, EventRecord> provideEventStoreStorage() {
        return DataStoreStorageFactory.createEventStoreStorage();
    }

    @Override
    protected MessageJournal<String, CommandRequest> provideCommandStoreStorage() {
        return DataStoreStorageFactory.createCommandStoreStorage();
    }

    @Override
    protected SnapshotStorage provideSnapshotStorage() {
        return DataStoreStorageFactory.createSnapshotStorage(TypeName.of(Order.getDescriptor()));
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(DataStoreSampleServer.class);
    }
}
