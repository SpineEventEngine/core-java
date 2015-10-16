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

import com.google.protobuf.Message;

import static com.google.protobuf.Descriptors.Descriptor;

public class LocalDatastoreStorageFactory extends DatastoreStorageFactory {

    /**
     * TODO:2015.10.07:alexander.litus: remove OS checking when this issue is fixed:
     * https://code.google.com/p/google-cloud-platform/issues/detail?id=10&thanks=10&ts=1443682670
     */
    @SuppressWarnings({"AccessOfSystemProperties", "DuplicateStringLiteralInspection"})
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    @SuppressWarnings("RefusedBequest") // overriding getter, no sense to call base method
    @Override
    protected <M extends Message> DatastoreManager<M> getManager(Descriptor descriptor) {
        final LocalDatastoreManager<M> manager = LocalDatastoreManager.newInstance(descriptor);
        return manager;
    }

    @Override
    public void setUp() {

        super.setUp();

        if (!IS_WINDOWS) {
            LocalDatastoreManager.start();
        }
    }

    @Override
    public void tearDown() {

        super.tearDown();

        LocalDatastoreManager.clear();

        if (!IS_WINDOWS) {
            LocalDatastoreManager.stop();
        }
    }

    public static LocalDatastoreStorageFactory getInstance() {
        return Singleton.INSTANCE.value;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final LocalDatastoreStorageFactory value = new LocalDatastoreStorageFactory();
    }
}
