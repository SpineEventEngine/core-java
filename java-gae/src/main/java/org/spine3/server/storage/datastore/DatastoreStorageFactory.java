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
import org.spine3.server.Entity;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.storage.*;

import static org.spine3.protobuf.Messages.getClassDescriptor;
import static org.spine3.util.Classes.getGenericParameterType;

public class DatastoreStorageFactory implements StorageFactory {

    private static final int ENTITY_MESSAGE_TYPE_PARAMETER_INDEX = 1;

    @Override
    public CommandStorage createCommandStorage() {
        final DatastoreManager<CommandStoreRecord> manager = DatastoreManager.newInstance(CommandStoreRecord.getDescriptor());
        return DatastoreCommandStorage.newInstance(manager);
    }

    @Override
    public EventStorage createEventStorage() {
        final DatastoreManager<EventStoreRecord> manager = DatastoreManager.newInstance(EventStoreRecord.getDescriptor());
        return DatastoreEventStorage.newInstance(manager);
    }

    @Override
    public <I> AggregateStorage<I> createAggregateStorage(Class<? extends Aggregate<I, ?>> aggregateClass) {
        final DatastoreManager<AggregateStorageRecord> manager = DatastoreManager.newInstance(AggregateStorageRecord.getDescriptor());
        return DatastoreAggregateStorage.newInstance(manager);
    }

    @Override
    public <I, M extends Message> EntityStorage<I, M> createEntityStorage(Class<? extends Entity<I, M>> entityClass) {

        final Class<Message> messageClass = getGenericParameterType(entityClass, ENTITY_MESSAGE_TYPE_PARAMETER_INDEX);
        // TODO:2015.10.05:alexander.litus: test this
        final Descriptors.Descriptor descriptor = (Descriptors.Descriptor) getClassDescriptor(messageClass);

        final DatastoreManager<M> manager = DatastoreManager.newInstance(descriptor);
        return DatastoreEntityStorage.newInstance(manager);
    }
}
