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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

/**
 * Creates storages based on local GAE Datastore.
 */
public class LocalDatastoreStorageFactory extends DatastoreStorageFactory {

    /**
     * TODO:2015.10.07:alexander.litus: remove OS checking when this issue is fixed:
     * https://code.google.com/p/google-cloud-platform/issues/detail?id=10&thanks=10&ts=1443682670
     */
    @SuppressWarnings({"AccessOfSystemProperties", "DuplicateStringLiteralInspection"})
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    @SuppressWarnings("RefusedBequest") // overriding getter, no sense to call base method
    @Override
    protected <M extends Message> DatastoreDepository<M> createDepository(Descriptors.Descriptor descriptor) {
        final LocalDatastoreDepository<M> depository = LocalDatastoreDepository.newInstance(descriptor);
        return depository;
    }

    @Override
    public void setUp() {

        super.setUp();

        /**
         * NOTE: start local Datastore Server manually on Windows.<br>
         * See <a href="https://github.com/SpineEventEngine/core-java/wiki/Configuring-Local-Datastore-Environment">docs</a> for details.<br>
         */
        if (!IS_WINDOWS) {
            LocalDatastoreDepository.start();
        }
    }

    @Override
    public void tearDown() {

        super.tearDown();

        LocalDatastoreDepository.clear();

        if (!IS_WINDOWS) {
            LocalDatastoreDepository.stop();
        }
    }

    /**
     * @return the factory instance object.
     */
    public static LocalDatastoreStorageFactory getInstance() {
        return Singleton.INSTANCE.value;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final LocalDatastoreStorageFactory value = new LocalDatastoreStorageFactory();
    }
}
