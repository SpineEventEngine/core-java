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

package org.spine3.gae.datastore;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.Snapshot;
import org.spine3.storage.SnapshotStorage;
import org.spine3.protobuf.JsonFormat;
import org.spine3.protobuf.Messages;
import org.spine3.TypeName;

import static org.spine3.gae.datastore.DataStoreHelper.*;

/**
 * DataStore-based {@link SnapshotStorage} implementation.
 */
public class DataStoreSnapshotStorage implements SnapshotStorage {

    private final DataStoreHelper dataStoreHelper;

    private final TypeName entityKind;

    /**
     * Requires unique class name to be used as snapshot kind.
     *
     * @param entityKind class name for snapshots to be stored
     */
    public DataStoreSnapshotStorage(TypeName entityKind) {
        this.entityKind = entityKind;
        dataStoreHelper = new DataStoreHelper();
    }

    @Override
    public void store(Snapshot snapshot, Message parentId) {
        final Entity dataStoreEntity = new Entity(entityKind.toString(), JsonFormat.printToString(parentId));

        final Any any = Messages.toAny(snapshot);

        dataStoreEntity.setProperty(VALUE_KEY, new Blob(any.getValue().toByteArray()));
        dataStoreEntity.setProperty(TYPE_KEY, any.getTypeUrl());

        dataStoreHelper.put(dataStoreEntity);
    }

    @Override
    public Snapshot read(Message parentId) {
        return dataStoreHelper.readMessageFromDataStore(entityKind.toString(), JsonFormat.printToString(parentId));
    }
}
