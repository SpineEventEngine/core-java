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

import com.google.protobuf.Message;
import org.spine3.server.Entity;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.storage.*;

import static com.google.protobuf.Descriptors.Descriptor;
import static org.spine3.protobuf.Messages.getClassDescriptor;
import static org.spine3.util.Classes.getGenericParameterType;

/**
 * Creates local GAE Datastores.
 */
public class LocalDatastoreStorageFactory implements StorageFactory {

    private static final int ENTITY_MESSAGE_TYPE_PARAMETER_INDEX = 1;

    /**
     * TODO:2015.10.07:alexander.litus: remove OS checking when this issue is fixed:
     * https://code.google.com/p/google-cloud-platform/issues/detail?id=10&thanks=10&ts=1443682670
     */
    @SuppressWarnings({"AccessOfSystemProperties", "DuplicateStringLiteralInspection"})
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");


    @Override
    public CommandStorage createCommandStorage() {
        final LocalDatastoreManager<CommandStoreRecord> manager =
                LocalDatastoreManager.newInstance(CommandStoreRecord.getDescriptor());
        return DatastoreCommandStorage.newInstance(manager);
    }

    @Override
    public EventStorage createEventStorage() {
        final LocalDatastoreManager<EventStoreRecord> manager =
                LocalDatastoreManager.newInstance(EventStoreRecord.getDescriptor());
        return DatastoreEventStorage.newInstance(manager);
    }

    /**
     * The parameter is unused.
     */
    @Override
    public <I> AggregateStorage<I> createAggregateStorage(Class<? extends Aggregate<I, ?>> unused) {
        final LocalDatastoreManager<AggregateStorageRecord> manager =
                LocalDatastoreManager.newInstance(AggregateStorageRecord.getDescriptor());
        return DatastoreAggregateStorage.newInstance(manager);
    }

    @Override
    public <I, M extends Message> EntityStorage<I, M> createEntityStorage(Class<? extends Entity<I, M>> entityClass) {

        final Class<Message> messageClass = getGenericParameterType(entityClass, ENTITY_MESSAGE_TYPE_PARAMETER_INDEX);

        final Descriptor descriptor = (Descriptor) getClassDescriptor(messageClass);

        final LocalDatastoreManager<M> manager = LocalDatastoreManager.newInstance(descriptor);;
        return DatastoreEntityStorage.newInstance(manager);
    }

    @Override
    public void setUp() {
        if (!IS_WINDOWS) {
            LocalDatastoreManager.start();
        }
    }

    @Override
    public void tearDown() {

        LocalDatastoreManager.clear();

        if (!IS_WINDOWS) {
            LocalDatastoreManager.stop();
        }
    }

    public static LocalDatastoreStorageFactory getInstance() {
        return Singleton.INSTANCE.value;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final LocalDatastoreStorageFactory value = new LocalDatastoreStorageFactory();
    }
}
