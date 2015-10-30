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

import com.google.api.services.datastore.client.Datastore;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import org.spine3.TypeName;
import org.spine3.server.storage.EntityStorage;

import static com.google.api.services.datastore.DatastoreV1.*;
import static com.google.api.services.datastore.client.DatastoreHelper.makeKey;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.server.storage.datastore.DatastoreWrapper.messageToEntity;
import static org.spine3.util.Identifiers.idToString;

/**
 * {@link EntityStorage} implementation based on Google App Engine Datastore.
 *
 * @see DatastoreStorageFactory
 * @see LocalDatastoreStorageFactory
 * @author Alexander Litus
 */
class DsEntityStorage<I, M extends Message> extends EntityStorage<I, M> {

    private final DatastoreWrapper<M> datastore;
    private final TypeName typeName;

    protected static <I, M extends Message> DsEntityStorage<I, M> newInstance(Descriptor descriptor, Datastore datastore) {
        return new DsEntityStorage<>(descriptor, datastore);
    }

    /**
     * Creates a new storage instance.
     * @param descriptor the descriptor of the type of messages to save to the storage.
     * @param datastore the datastore implementation to use.
     */
    private DsEntityStorage(Descriptor descriptor, Datastore datastore) {
        this.typeName = TypeName.of(descriptor);
        this.datastore = new DatastoreWrapper<>(datastore);
    }

    @Override
    public M read(I id) {

        final String idString = idToString(id);
        final Key.Builder key = makeKey(typeName.nameOnly(), idString);
        final LookupRequest request = LookupRequest.newBuilder().addKey(key).build();

        final LookupResponse response = datastore.lookup(request);

        if (response == null || response.getFoundCount() == 0) {
            @SuppressWarnings("unchecked") // cast is save because Any is Message
            final M empty = (M) Any.getDefaultInstance();
            return empty;
        }
        final EntityResult entity = response.getFound(0);
        final M result = datastore.entityToMessage(entity, typeName.toTypeUrl());
        return result;
    }

    @Override
    public void write(I id, M message) {

        checkNotNull(id, "Id is null.");
        checkNotNull(message, "Message is null.");

        final String idString = idToString(id);
        final Key.Builder kindKey = makeKey(typeName.nameOnly(), idString);
        final Entity.Builder entity = messageToEntity(message, kindKey);
        final Mutation.Builder mutation = Mutation.newBuilder().addInsert(entity);
        datastore.commit(mutation);
    }
}
