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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.TypeName;
import org.spine3.sample.order.Order;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.datastore.DatastoreStorageFactory;
import org.spine3.server.storage.datastore.LocalDatastoreManager;

/**
 * Entry point for core-java sample without gRPC. Works with local Google Cloud Datastore.
 *
 * NOTE: to run this sample on Windows, start local Google Cloud Datastore server manually.
 * Reported an issue here:
 * https://code.google.com/p/google-cloud-platform/issues/detail?id=10&thanks=10&ts=1443682670
 * TODO:2015.10.07:alexander.litus: remove OS checking when this issue is fixed.
 *
 * @author Mikhail Mikhaylov
 */
public class DataStoreSample extends BaseSample {

    private static final TypeName TYPE_NAME = TypeName.of(Order.getDescriptor());
    private static final LocalDatastoreManager<Order> DATASTORE_MANAGER = LocalDatastoreManager.newInstance(TYPE_NAME);

    @SuppressWarnings({"AccessOfSystemProperties", "DuplicateStringLiteralInspection"})
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static void main(String[] args) {

        if (!IS_WINDOWS) { // Temporary solution
            DATASTORE_MANAGER.start();
        }

        BaseSample sample = new DataStoreSample();

        try {
            sample.execute();
        } finally {

            DATASTORE_MANAGER.clear();

            if (!IS_WINDOWS) { // Temporary solution
                DATASTORE_MANAGER.stop();
            }
        }
    }

    @Override
    protected StorageFactory storageFactory() {
        return new DatastoreStorageFactory();
    }

    @Override
    protected Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private DataStoreSample() {
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(DataStoreSample.class);
    }

}
