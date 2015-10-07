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

package org.spine3.server.storage.datastore;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.spine3.TypeName;
import org.spine3.server.storage.AggregateStorageRecord;
import org.spine3.server.storage.AggregateStorageShould;
import org.spine3.test.project.ProjectId;

/*
 * NOTE: to run these tests on Windows, start Local Development Datastore Server manually.
 * Reported an issue here:
 * https://code.google.com/p/google-cloud-platform/issues/detail?id=10&thanks=10&ts=1443682670
 * TODO:2015.10.07:alexander.litus: remove OS checking when this issue will be fixed.
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "RefusedBequest"})
public class DatastoreAggregateStorageShould extends AggregateStorageShould {

    private static final TypeName TYPE_NAME = TypeName.of(AggregateStorageRecord.getDescriptor());

    private static final LocalDatastoreManager<AggregateStorageRecord> DATASTORE_MANAGER = LocalDatastoreManager.newInstance(TYPE_NAME);

    private static final DatastoreAggregateStorage<ProjectId> STORAGE = DatastoreAggregateStorage.newInstance(DATASTORE_MANAGER);

    @SuppressWarnings({"AccessOfSystemProperties", "DuplicateStringLiteralInspection"})
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public DatastoreAggregateStorageShould() {
        super(STORAGE);
    }


    @BeforeClass
    public static void setUpClass() {
        if (!IS_WINDOWS) { // Temporary solution
            DATASTORE_MANAGER.start();
        }
    }

    @After
    public void tearDownTest() {
        waitIfNeeded(1);
        DATASTORE_MANAGER.clear();
        waitIfNeeded(1);
    }

    @AfterClass
    public static void tearDownClass() {
        if (!IS_WINDOWS) { // Temporary solution
            DATASTORE_MANAGER.stop();
        }
    }

    @Override
    protected void waitIfNeeded(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ignored) {}
    }
}
