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

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.TypeName;
import org.spine3.protobuf.Messages;
import org.spine3.server.StorageWithTimelineAndVersion;

import java.util.List;

import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static org.spine3.gae.datastore.DataStoreHelper.PARENT_ID_KEY;
import static org.spine3.gae.datastore.DataStoreHelper.prepareFilter;
import static org.spine3.protobuf.Messages.toJson;

/**
 * DataStore-based {@link StorageWithTimelineAndVersion} implementation.
 *
 * @param <M> Message type to store
 */
public class DataStoreStorage<M extends Message> implements StorageWithTimelineAndVersion<M> {

    private final DataStoreHelper dataStoreHelper;

    private final TypeName type;

    public static <M extends Message> DataStoreStorage<M> newInstance(Class<M> messageClass) {
        final Descriptors.Descriptor classDescriptor = Messages.getClassDescriptor(messageClass);
        return new DataStoreStorage<>(TypeName.of(classDescriptor));
    }

    private DataStoreStorage(TypeName type) {
        this.type = type;
        dataStoreHelper = new DataStoreHelper();
    }

    @Override
    public void store(Message message) {
        final Entity dataStoreEntity = Converters.convert(message);

        dataStoreHelper.put(dataStoreEntity);
    }

    @Override
    public List<M> load(Message parentId, int sinceVersion) {
        return dataStoreHelper.loadByFilter(type.toString(),
                prepareFilter(parentId, sinceVersion));
    }

    @Override
    public List<M> load(Timestamp from) {
        return dataStoreHelper.loadByFilter(type.toString(), prepareFilter(from));
    }

    @Override
    public List<M> load(Message parentId, Timestamp from) {
        return dataStoreHelper.loadByFilter(type.toString(),
                prepareFilter(parentId, from));
    }

    @Override
    public List<M> load(Message id) {
        return dataStoreHelper.loadByFilter(type.toString(), new Query.FilterPredicate(
                PARENT_ID_KEY, EQUAL, toJson(id)));
    }

}
