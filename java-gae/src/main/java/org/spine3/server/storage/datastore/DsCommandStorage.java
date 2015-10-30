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

import org.spine3.server.storage.CommandStorage;
import org.spine3.server.storage.CommandStoreRecord;

import static com.google.api.services.datastore.DatastoreV1.Property;
import static com.google.api.services.datastore.client.DatastoreHelper.makeProperty;
import static com.google.api.services.datastore.client.DatastoreHelper.makeValue;

/**
 * Storage for command records based on Google Cloud Datastore.
 *
 * @see DatastoreStorageFactory
 * @see LocalDatastoreStorageFactory
 * @author Alexander Litus
 */
class DsCommandStorage extends CommandStorage {

    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static final String COMMAND_ID_PROPERTY_NAME = "commandId";

    private final DsStorage<CommandStoreRecord> storage;

    private DsCommandStorage(DsStorage<CommandStoreRecord> storage) {
        this.storage = storage;
    }

    protected static CommandStorage newInstance(DsStorage<CommandStoreRecord> storage) {
        return new DsCommandStorage(storage);
    }

    @Override
    protected void write(CommandStoreRecord record) {

        final String id = record.getCommandId();
        final Property.Builder idProperty = makeProperty(COMMAND_ID_PROPERTY_NAME, makeValue(id));
        storage.storeWithAutoId(idProperty, record, record.getTimestamp());
    }
}
