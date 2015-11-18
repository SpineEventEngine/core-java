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
 * Creates storages based on the local Google {@link LocalDevelopmentDatastore}.
 */
@SuppressWarnings("CallToSystemGetenv")
public class LocalDatastoreStorageFactory extends DatastoreStorageFactory {

    private static final String DEFAULT_DATASET_NAME = "spine-local-dataset";
    private static final String DEFAULT_HOST = "http://localhost:8080";

    private static final DatastoreOptions DEFAULT_LOCAL_OPTIONS = new DatastoreOptions.Builder()
            .dataset(DEFAULT_DATASET_NAME)
            .host(DEFAULT_HOST)
            .build();

    private static final String OPTION_TESTING_MODE = "--testing";

    public static final String VAR_NAME_GCD_HOME = "GCD_HOME";

    private static final String GCD_HOME_PATH = retrieveGcdHome();

    private static final String ENVIRONMENT_NOT_CONFIGURED_MESSAGE = VAR_NAME_GCD_HOME + " environment variable is not configured. " +
                    "See https://github.com/SpineEventEngine/core-java/wiki/Configuring-Local-Datastore-Environment";

    private final LocalDevelopmentDatastore localDatastore;

    /**
     * Returns a default factory instance. A {@link LocalDevelopmentDatastore} is created with default {@link DatastoreOptions}:
     * <ul>
     *     <li>Dataset name: {@code spine-local-dataset}</li>
     *     <li>Host: {@code http://localhost:8080}</li>
     * </ul>
     */
    public static LocalDatastoreStorageFactory getDefaultInstance() {
        return DefaultInstanceSingleton.INSTANCE.value;
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

    /**
     * Intended to start the local Datastore server in testing mode.
     * <p>
     * NOTE: does nothing for now because of several issues:
     * <ul>
     *     <li>
     *         This <a href="https://github.com/GoogleCloudPlatform/google-cloud-datastore/commit/a077c5b4d6fa2826fd6c376b692686894b719fd9">commit</a>
     *         seems to fix the first issue, but there is no release with this fix available yet.
     *     </li>
     *     <li>
     *         Also fails to start on Windows:
     *         <a href="https://code.google.com/p/google-cloud-platform/issues/detail?id=10&thanks=10&ts=1443682670">issue</a>.
     *     </li>
     * </ul>
     * <p>
     * Until these issues are not fixed, it is required to start the local Datastore Server manually.
     * See <a href="https://github.com/SpineEventEngine/core-java/wiki/Configuring-Local-Datastore-Environment">docs</a> for details.<br>
     *
     * @throws RuntimeException if {@link LocalDevelopmentDatastore#start(String, String, String...)}
     *                          throws LocalDevelopmentDatastoreException.
     * @see <a href="https://cloud.google.com/DATASTORE/docs/tools/devserver#local_development_server_command-line_arguments">
     * Documentation</a> ("testing" option)
     */
    @Override
    public void setUp() {
        super.setUp();
        if (false) // TODO:2015-11-12:alexander.litus: Remove the condition when issues specified above are fixed
        try {
            localDatastore.start(GCD_HOME_PATH, DEFAULT_DATASET_NAME, OPTION_TESTING_MODE);
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    /**
     * Clears all data in the local Datastore.
     * <p>
     * NOTE: does not stop the server because of several issues. See {@link #setUp()} method doc for details.
     *
     * @throws RuntimeException if {@link LocalDevelopmentDatastore#stop()} throws LocalDevelopmentDatastoreException.
     */
    @Override
    public void tearDown() {
        super.tearDown();
        clear();
        if (false) // TODO:2015-11-12:alexander.litus: remove the condition when issues specified in setUp method javadoc are fixed
        try {
            localDatastore.stop();
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

    private static String retrieveGcdHome() {
        final String gcdHome = System.getenv(VAR_NAME_GCD_HOME);
        checkState(gcdHome != null, ENVIRONMENT_NOT_CONFIGURED_MESSAGE);
        return gcdHome;
    }

    private enum DefaultInstanceSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final LocalDatastoreStorageFactory value = new LocalDatastoreStorageFactory(DefaultDatastoreSingleton.INSTANCE.value);
    }

    private enum DefaultDatastoreSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final LocalDevelopmentDatastore value = LocalDevelopmentDatastoreFactory.get().create(DEFAULT_LOCAL_OPTIONS);
    }
}
