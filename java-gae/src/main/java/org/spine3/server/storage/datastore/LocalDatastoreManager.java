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
import com.google.protobuf.Message;

import static com.google.common.base.Throwables.propagate;
import static com.google.protobuf.Descriptors.Descriptor;

/**
 * Provides access to local Google Cloud Datastore. For usage in tests and samples.
 *
 * @author Alexander Litus
 */
public class LocalDatastoreManager<M extends Message> extends DatastoreManager<M> {

    /**
     * TODO:2015.10.07:alexander.litus: remove OS checking when this issue is fixed:
     * https://code.google.com/p/google-cloud-platform/issues/detail?id=10&thanks=10&ts=1443682670
     */
    @SuppressWarnings({"AccessOfSystemProperties", "DuplicateStringLiteralInspection"})
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    @SuppressWarnings("CallToSystemGetenv")
    private static final String GCD_HOME = System.getenv("GCD_HOME");

    private static final String LOCALHOST = "http://localhost:8080";

    private static final String LOCAL_DATASET_NAME = "spine-local-dataset";

    private static final String OPTION_TESTING_MODE = "testing";

    private static final DatastoreOptions DEFAULT_OPTIONS = new DatastoreOptions.Builder()
            .host(LOCALHOST)
            .dataset(LOCAL_DATASET_NAME)
            .build();


    private static final LocalDevelopmentDatastore LOCAL_DATASTORE = LocalDevelopmentDatastoreFactory.get().create(DEFAULT_OPTIONS);

    private LocalDatastoreManager(Descriptor descriptor) {
        super(descriptor, LOCAL_DATASTORE);
    }

    /**
     * Creates a new manager instance.
     * @param descriptor the descriptor of the type of messages to save to the storage.
     */
    public static <M extends Message> LocalDatastoreManager<M> newInstance(Descriptor descriptor) {
        return new LocalDatastoreManager<>(descriptor);
    }


    /**
     * Starts the local Datastore server in testing mode.
     * <p>
     * NOTE: does not work on Windows. Reported an issue
     * <a href="https://code.google.com/p/google-cloud-platform/issues/detail?id=10&thanks=10&ts=1443682670">here</a>.
     *
     * @throws RuntimeException if {@link com.google.api.services.datastore.client.LocalDevelopmentDatastore#start(String, String, String...)}
     *                          throws LocalDevelopmentDatastoreException.
     * @see <a href="https://cloud.google.com/DATASTORE/docs/tools/devserver#local_development_server_command-line_arguments">
     * Documentation</a> ("testing" option)
     */
    public static void start() {

        if (IS_WINDOWS) {
            throw new UnsupportedOperationException("Cannot start server on Windows. See method docs for details.");
        }

        try {
            LOCAL_DATASTORE.start(GCD_HOME, LOCAL_DATASET_NAME, OPTION_TESTING_MODE);
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    /**
     * Clears all data in the local Datastore.
     *
     * @throws RuntimeException if {@link com.google.api.services.datastore.client.LocalDevelopmentDatastore#clear()} throws LocalDevelopmentDatastoreException.
     */
    public static void clear() {
        try {
            LOCAL_DATASTORE.clear();
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    /**
     * Stops the local Datastore server.
     *
     * @throws RuntimeException if {@link com.google.api.services.datastore.client.LocalDevelopmentDatastore#stop()} throws LocalDevelopmentDatastoreException.
     */
    public static void stop() {
        try {
            LOCAL_DATASTORE.stop();
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }
}
