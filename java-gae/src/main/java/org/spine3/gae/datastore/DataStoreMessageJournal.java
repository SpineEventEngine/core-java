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
import org.spine3.server.MessageJournal;
import org.spine3.util.Identifiers;

import java.util.List;

import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static org.spine3.gae.datastore.DataStoreHelperOldImpl.PARENT_ID_KEY;
import static org.spine3.gae.datastore.DataStoreHelperOldImpl.prepareFilter;

/**
 * {@code MessageJournal} based on App Engine Datastore.
 *
 * {@inheritDoc}
 *
 * @author Mikhail Mikhaylov
 */
public class DataStoreMessageJournal<I, M extends Message> implements MessageJournal<I, M> {

    private final DataStoreHelperOldImpl dataStoreHelper;

    private final TypeName type;

    public static <I, M extends Message> DataStoreMessageJournal<I, M> newInstance(Class<M> messageClass) {
        // Cast the class as the descriptor returned for a message class cannot be of outer class (which has type FileDescriptor).
        final Descriptors.Descriptor classDescriptor = (Descriptors.Descriptor)Messages.getClassDescriptor(messageClass);
        return new DataStoreMessageJournal<>(TypeName.of(classDescriptor));
    }

    private DataStoreMessageJournal(TypeName type) {
        this.type = type;
        dataStoreHelper = new DataStoreHelperOldImpl();
    }

    @Override
    public void store(I entityId, M message) {
        final Entity dataStoreEntity = Converters.convert(message);

        dataStoreHelper.put(dataStoreEntity);
    }

    @Override
    public List<M> loadAllSince(Timestamp timestamp) {
        final Query.Filter filter = prepareFilter(timestamp);
        final List<M> result = dataStoreHelper.loadByFilter(type.toString(), filter);
        return result;
    }

    @Override
    public List<M> loadSince(I entityId, Timestamp timestamp) {
        final Query.Filter filter = prepareFilter(entityId, timestamp);
        final List<M> result = dataStoreHelper.loadByFilter(type.toString(), filter);
        return result;
    }

    @Override
    public List<M> load(I entityId) {
        final String id = Identifiers.idToString(entityId);
        final Query.FilterPredicate filter = new Query.FilterPredicate(
                PARENT_ID_KEY, EQUAL, id);
        final List<M> result = dataStoreHelper.loadByFilter(type.toString(), filter);
        return result;
    }

}
