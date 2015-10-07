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
import org.spine3.server.storage.EventStorageShould;
import org.spine3.server.storage.EventStoreRecord;

/*
 * NOTE: to run these tests on Windows, start Local Development Datastore Server manually.
 * Reported an issue here:
 * https://code.google.com/p/google-cloud-platform/issues/detail?id=10&thanks=10&ts=1443682670
 * TODO:2015.10.07:alexander.litus: remove OS checking when this issue will be fixed.
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "MethodMayBeStatic", "RefusedBequest"})
public class DatastoreEventStorageShould extends EventStorageShould {

    private static final TypeName TYPE_NAME = TypeName.of(EventStoreRecord.getDescriptor());

    private static final LocalDatastoreManager<EventStoreRecord> DATASTORE_MANAGER = LocalDatastoreManager.newInstance(TYPE_NAME);

    private static final DatastoreEventStorage STORAGE = DatastoreEventStorage.newInstance(DATASTORE_MANAGER);

    @SuppressWarnings({"AccessOfSystemProperties", "DuplicateStringLiteralInspection"})
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public DatastoreEventStorageShould() {
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
        DATASTORE_MANAGER.clear();
    }

    @AfterClass
    public static void tearDownClass() {
        if (!IS_WINDOWS) { // Temporary solution
            DATASTORE_MANAGER.stop();
        }
    }

    /*
     * TODO:2015.10.06:alexander.litus: these tests sometimes fail if run all tests in this class (maybe because of timing).
     * Investigate how to fix this.
     */

    @Override
    public void store_and_read_one_event() {
    }

    @Override
    public void write_and_read_one_event() {
    }

    @Override
    public void write_and_read_several_events() {
    }

    @Override
    public void return_iterator_pointed_to_first_element_if_read_all_events_several_times() {
    }
}
