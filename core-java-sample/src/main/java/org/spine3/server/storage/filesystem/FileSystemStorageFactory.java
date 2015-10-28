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

package org.spine3.server.storage.filesystem;

import com.google.protobuf.Descriptors.GenericDescriptor;
import com.google.protobuf.Message;
import org.spine3.server.Entity;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.storage.*;

import static org.spine3.protobuf.Messages.getClassDescriptor;
import static org.spine3.util.Classes.getGenericParameterType;

/**
 * A factory for storages based on the file system.
 *
 * @author Alexander Litus
 * @author Alexander Mikhaylov
 * @author Alexander Yevsyukov
 */
public class FileSystemStorageFactory implements StorageFactory {

    private static final int AGGREGATE_MESSAGE_PARAMETER_INDEX = 1;

    private final Class executorClass;

    /**
     * Creates new storage factory instance.
     *
     * @param executorClass execution context class which is used as {@link FileSystemHelper#configure(Class)} method parameter.
     * @see FileSystemHelper#configure(Class)
     */
    public static StorageFactory newInstance(Class executorClass) {
        return new FileSystemStorageFactory(executorClass);
    }

    private FileSystemStorageFactory(Class executorClass) {
        this.executorClass = executorClass;
    }

    @Override
    public CommandStorage createCommandStorage() {
        return FsCommandStorage.newInstance();
    }

    @Override
    public EventStorage createEventStorage() {
        return FsEventStorage.newInstance();
    }

    @Override
    public <I> AggregateStorage<I> createAggregateStorage(Class<? extends Aggregate<I, ?>> aggregateClass) {
        final Class<Message> messageClazz =
                getGenericParameterType(aggregateClass, AGGREGATE_MESSAGE_PARAMETER_INDEX);
        final GenericDescriptor msgClassDescriptor = getClassDescriptor(messageClazz);
        final String msgDescriptorName = msgClassDescriptor.getName();
        return new FsAggregateStorage<>(msgDescriptorName);
    }

    @Override
    public <I, M extends Message> EntityStorage<I, M> createEntityStorage(Class<? extends Entity<I, M>> entityClass) {
        return FsEntityStorage.newInstance();
    }

    @Override
    public void setUp() {
        FileSystemHelper.configure(executorClass);
    }

    @Override
    public void tearDown() {
        FileSystemHelper.deleteAll();
    }
}
