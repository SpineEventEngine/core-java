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

import com.google.api.services.datastore.client.*;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.spine3.TypeName;
import org.spine3.server.storage.EntityStorage;

import static com.google.api.services.datastore.DatastoreV1.*;
import static com.google.api.services.datastore.DatastoreV1.CommitRequest.Mode.NON_TRANSACTIONAL;
import static com.google.api.services.datastore.client.DatastoreHelper.*;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.util.Identifiers.idToString;

/**
 * {@link org.spine3.server.storage.EntityStorage} implementation based on Google App Engine Datastore.
 *
 * @author Alexander Litus
 */
public class DatastoreEntityStorage<I, M extends Message> extends EntityStorage<I, M> {

    public static final String VALUE_KEY = "value";
    private static final String LOCALHOST = "http://localhost:8080";
    private static final String DATASET_NAME = "myapp";
    private static final String DATASTORE_HOST_ENV_VAR_NAME = "DATASTORE_HOST";
    private static final String DATASTORE_DATASET_ENV_VAR_NAME = "DATASTORE_DATASET";

    private final LocalDevelopmentDatastore datastore;
    private final TypeName typeName;

    public DatastoreEntityStorage(TypeName typeName) {

        this.typeName = typeName;

        DatastoreOptions options = new DatastoreOptions.Builder()
                .host(LOCALHOST)
                .dataset(DATASET_NAME).build();

        LocalDevelopmentDatastoreOptions localOptions = new LocalDevelopmentDatastoreOptions.Builder()
                .addEnvVar(DATASTORE_HOST_ENV_VAR_NAME, LOCALHOST)
                .addEnvVar(DATASTORE_DATASET_ENV_VAR_NAME, DATASET_NAME).build();

        datastore = LocalDevelopmentDatastoreFactory.get().create(options, localOptions);
    }

    @Override
    public M read(I id) {

        final String idString = idToString(id);

        final Message message = get(idString);

        @SuppressWarnings("unchecked") // save because messages of only this type are written
        final M result = (M) message;
        return result;
    }

    @Override
    public void write(I id, M message) {

        checkNotNull(id);
        checkNotNull(message);

        final String idString = idToString(id);

        store(idString, message);
    }

    public void start() {
        try {
            datastore.start("C:\\gcd", DATASET_NAME);
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    public void clear() {
        try {
            datastore.clear();
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    public void stop() {
        try {
            datastore.stop();
        } catch (LocalDevelopmentDatastoreException e) {
            propagate(e);
        }
    }

    public void store(String id, M message) {

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

    public M get(String id) {

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

    private M entityToMessage(EntityOrBuilder entity) {

        final Any.Builder any = Any.newBuilder();
        final Property property = entity.getProperty(0);

        if (property.getName().equals(VALUE_KEY)) {
            any.setValue(property.getValue().getBlobValue());
        }
        any.setTypeUrl(typeName.toTypeUrl());

        final M result = fromAny(any.build());
        return result;
    }
}
