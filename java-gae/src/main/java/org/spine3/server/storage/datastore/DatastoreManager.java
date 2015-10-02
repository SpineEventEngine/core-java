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

import com.google.api.services.datastore.DatastoreV1.*;
import com.google.api.services.datastore.client.Datastore;
import com.google.api.services.datastore.client.DatastoreException;
import com.google.api.services.datastore.client.DatastoreFactory;
import com.google.api.services.datastore.client.DatastoreOptions;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.spine3.TypeName;

import static com.google.api.services.datastore.DatastoreV1.CommitRequest.Mode.NON_TRANSACTIONAL;
import static com.google.api.services.datastore.client.DatastoreHelper.*;
import static com.google.common.base.Throwables.propagate;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;

/**
 * Provides the access to Google Cloud Datastore.
 *
 * @author Alexander Litus
 */
public class DatastoreManager {

    @SuppressWarnings("DuplicateStringLiteralInspection") // temporary
    private static final String VALUE_KEY = "value";
    private static final String LOCALHOST = "http://localhost:8080";
    protected static final String DATASET_NAME = "myapp";

    private static final DatastoreOptions OPTIONS = new DatastoreOptions.Builder()
            .host(LOCALHOST)
            .dataset(DATASET_NAME).build();

    private final Datastore datastore;
    private TypeName typeName;


    private DatastoreManager() {
        this(DatastoreFactory.get());
    }

    protected DatastoreManager(DatastoreFactory factory) {
        this.datastore = factory.create(OPTIONS);
    }

    protected static DatastoreManager instance() {
        return Singleton.INSTANCE.value;
    }

    public <M extends Message> void store(String id, M message) {

        final ByteString serializedMessage = toAny(message).getValue();
        final Key.Builder key = makeKey(typeName.nameOnly(), id);
        final Value.Builder value = makeValue(serializedMessage);
        final Property.Builder property = makeProperty(VALUE_KEY, value);
        Entity.Builder entity = Entity.newBuilder()
                .setKey(key)
                .addProperty(property);

        final Mutation.Builder mutation = Mutation.newBuilder().addInsert(entity);
        CommitRequest commitRequest = CommitRequest.newBuilder()
                .setMode(NON_TRANSACTIONAL)
                .setMutation(mutation)
                .build();

        try {
            datastore.commit(commitRequest);
        } catch (DatastoreException e) {
            propagate(e);
        }
    }

    public <M extends Message> M read(String id) {

        final Key.Builder key = makeKey(typeName.nameOnly(), id);
        LookupRequest request = LookupRequest.newBuilder().addKey(key).build();

        LookupResponse response = null;
        try {
            response = datastore.lookup(request);
        } catch (DatastoreException e) {
            propagate(e);
        }

        if (response == null || response.getFoundCount() == 0) {
            @SuppressWarnings("unchecked")
            final M empty = (M) Any.getDefaultInstance();// cast is save because Any is Message
            return empty;
        }

        Entity entity = response.getFound(0).getEntity();
        final M message = entityToMessage(entity);

        return message;
    }

    private <M extends Message> M entityToMessage(EntityOrBuilder entity) {

        final Any.Builder any = Any.newBuilder();
        final Property property = entity.getProperty(0);

        if (property.getName().equals(VALUE_KEY)) {
            any.setValue(property.getValue().getBlobValue());
        }
        any.setTypeUrl(typeName.toTypeUrl());

        final M result = fromAny(any.build());
        return result;
    }

    protected Datastore getDatastore() {
        return datastore;
    }

    protected void setTypeName(TypeName typeName) {
        this.typeName = typeName;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final DatastoreManager value = new DatastoreManager();
    }
}
