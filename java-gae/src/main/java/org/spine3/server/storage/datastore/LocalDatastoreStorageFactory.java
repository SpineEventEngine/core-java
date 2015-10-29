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

import com.google.api.services.datastore.client.DatastoreOptions;
import com.google.api.services.datastore.client.LocalDevelopmentDatastore;
import com.google.api.services.datastore.client.LocalDevelopmentDatastoreException;
import com.google.api.services.datastore.client.LocalDevelopmentDatastoreFactory;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;

/**
 * Creates storages based on local Google {@link LocalDevelopmentDatastore}.
 */
@SuppressWarnings("CallToSystemGetenv")
public class LocalDatastoreStorageFactory extends DatastoreStorageFactory {

    /**
     * TODO:2015.10.07:alexander.litus: remove OS checking when this issue is fixed:
     * https://code.google.com/p/google-cloud-platform/issues/detail?id=10&thanks=10&ts=1443682670
     */
    @SuppressWarnings("AccessOfSystemProperties")
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private static final String DEFAULT_DATASET_NAME = "spine-local-dataset";
    private static final String DEFAULT_HOST = "http://localhost:8080";

    private static final DatastoreOptions DEFAULT_LOCAL_OPTIONS = new DatastoreOptions.Builder()
            .dataset(DEFAULT_DATASET_NAME)
            .host(DEFAULT_HOST)
            .build();

    private static final String GCD_HOME = retrieveGcdHome();

    private static final String OPTION_TESTING_MODE = "testing";

    private static final String ENVIRONMENT_NOT_CONFIGURED_MESSAGE = "GCD_HOME environment variable is not configured. " +
                    "See https://github.com/SpineEventEngine/core-java/wiki/Configuring-Local-Datastore-Environment";

    private final LocalDevelopmentDatastore localDatastore;

    /**
     * Creates a new factory instance. A {@link LocalDevelopmentDatastore} is created with default {@link DatastoreOptions}:
     * <ul>
     *     <li>Dataset name: {@code spine-local-dataset}</li>
     *     <li>Host: {@code http://localhost:8080}</li>
     * </ul>
     */
    public static LocalDatastoreStorageFactory newInstance() {
        final LocalDevelopmentDatastore datastore = LocalDevelopmentDatastoreFactory.get().create(DEFAULT_LOCAL_OPTIONS);
        return new LocalDatastoreStorageFactory(datastore);
    }

    /**
     * Creates a new factory instance.
     * @param options {@link DatastoreOptions} used to create a {@link LocalDevelopmentDatastore}
     */
    public static LocalDatastoreStorageFactory newInstance(DatastoreOptions options) {
        final LocalDevelopmentDatastore datastore = LocalDevelopmentDatastoreFactory.get().create(options);
        return new LocalDatastoreStorageFactory(datastore);
    }

    private LocalDatastoreStorageFactory(LocalDevelopmentDatastore datastore) {
        super(datastore);
        localDatastore = datastore;
    }

    @Override
    public void setUp() {

        super.setUp();

        /**
         * NOTE: start local Datastore Server manually on Windows.<br>
         * See <a href="https://github.com/SpineEventEngine/core-java/wiki/Configuring-Local-Datastore-Environment">docs</a> for details.<br>
         */
        if (!IS_WINDOWS) {
            start();
        }
    }

    @Override
    public void tearDown() {

        super.tearDown();

        clear();

        if (!IS_WINDOWS) {
            stop();
        }
    }

    /**
     * Starts the local Datastore server in testing mode.
     * <p>
     * NOTE: does not work on Windows. Reported an issue
     * <a href="https://code.google.com/p/google-cloud-platform/issues/detail?id=10&thanks=10&ts=1443682670">here</a>.
     *
     * NOTE: start local Datastore Server manually on Windows.<br>
     * See <a href="https://github.com/SpineEventEngine/core-java/wiki/Configuring-Local-Datastore-Environment">docs</a> for details.<br>
     *
     * @throws RuntimeException if {@link LocalDevelopmentDatastore#start(String, String, String...)}
     *                          throws LocalDevelopmentDatastoreException.
     * @see <a href="https://cloud.google.com/DATASTORE/docs/tools/devserver#local_development_server_command-line_arguments">
     * Documentation</a> ("testing" option)
     */
    private void start() {

        if (IS_WINDOWS) {
            throw new UnsupportedOperationException("Cannot start server on Windows. See method docs for details.");
        }

        try {
            localDatastore.start(GCD_HOME, DEFAULT_DATASET_NAME, OPTION_TESTING_MODE);
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    /**
     * Clears all data in the local Datastore.
     *
     * @throws RuntimeException if {@link LocalDevelopmentDatastore#clear()} throws LocalDevelopmentDatastoreException.
     */
    public void clear() {
        try {
            localDatastore.clear();
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    /**
     * Stops the local Datastore server.
     *
     * @throws RuntimeException if {@link LocalDevelopmentDatastore#stop()} throws LocalDevelopmentDatastoreException.
     */
    private void stop() {
        try {
            localDatastore.stop();
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    private static String retrieveGcdHome() {
        final String gcdHome = System.getenv("GCD_HOME");
        checkState(gcdHome != null, ENVIRONMENT_NOT_CONFIGURED_MESSAGE);
        return gcdHome;
    }
}
