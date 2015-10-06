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

import com.google.api.services.datastore.client.LocalDevelopmentDatastore;
import com.google.api.services.datastore.client.LocalDevelopmentDatastoreException;
import com.google.api.services.datastore.client.LocalDevelopmentDatastoreFactory;
import com.google.protobuf.Message;
import org.spine3.TypeName;

import static com.google.common.base.Throwables.propagate;

public class LocalDatastoreManager<M extends Message> extends DatastoreManager<M> {

    private static final String PATH_TO_GCD = "C:\\gcd";

    private LocalDatastoreManager(TypeName typeName) {
        super(LocalDevelopmentDatastoreFactory.get(), typeName);
    }

    protected static <M extends Message> LocalDatastoreManager<M> newInstance(TypeName typeName) {
        return new LocalDatastoreManager<>(typeName);
    }

    public void start() {
        final LocalDevelopmentDatastore datastore = getLocalDatastore();
        try {
            datastore.start(PATH_TO_GCD, DATASET_NAME);
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    public void clear() {
        final LocalDevelopmentDatastore datastore = getLocalDatastore();
        try {
            datastore.clear();
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    public void stop() {
        final LocalDevelopmentDatastore datastore = getLocalDatastore();
        try {
            datastore.stop();
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    private LocalDevelopmentDatastore getLocalDatastore() {
        return (LocalDevelopmentDatastore) getDatastore();
    }
}
