/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
import org.spine3.io.file.FileUtil;
import org.spine3.protobuf.Messages;
import org.spine3.server.Entity;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.storage.*;
import org.spine3.server.util.Classes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newLinkedList;
import static org.spine3.server.storage.filesystem.FsAggregateStorage.PATH_DELIMITER;

/**
 * A factory for storages based on the file system.
 *
 * @author Alexander Litus
 * @author Alexander Mikhaylov
 * @author Alexander Yevsyukov
 */
public class FileSystemStorageFactory implements StorageFactory {

    private static final int AGGREGATE_MESSAGE_PARAMETER_INDEX = 1;

    private final List<FsEventStorage> eventStorages = newLinkedList();

    /**
     * An absolute path to the root storage directory (without the delimiter at the end)
     */
    private final String rootDirectoryPath;

    /**
     * Creates a new storage factory instance.
     *
     * @param executorClass an execution context class used to choose target directory for a storage.
     */
    public static StorageFactory newInstance(Class executorClass) {
        return new FileSystemStorageFactory(executorClass);
    }

    private FileSystemStorageFactory(Class executorClass) {
        this.rootDirectoryPath = buildRootDirectoryPath(executorClass);
    }

    @Override
    public CommandStorage createCommandStorage() {
        final FsCommandStorage storage = tryCreateCommandStorage();
        return storage;
    }

    @Override
    public EventStorage createEventStorage() {
        final FsEventStorage storage = tryCreateEventStorage();
        eventStorages.add(storage);
        return storage;
    }

    @Override
    public <I> AggregateStorage<I> createAggregateStorage(Class<? extends Aggregate<I, ?>> aggregateClass) {
        final Class<Message> messageClazz =
                Classes.getGenericParameterType(aggregateClass, AGGREGATE_MESSAGE_PARAMETER_INDEX);
        final GenericDescriptor msgClassDescriptor = Messages.getClassDescriptor(messageClazz);
        final String msgDescriptorName = msgClassDescriptor.getName();
        return FsAggregateStorage.newInstance(rootDirectoryPath, msgDescriptorName);
    }

    /**
     * NOTE: the parameter is unused.
     */
    @Override
    public <I, M extends Message> EntityStorage<I, M> createEntityStorage(Class<? extends Entity<I, M>> unused) {
        return FsEntityStorage.newInstance(rootDirectoryPath);
    }

    @Override
    public void init() {
        // NOP
    }

    @Override
    public void close() {

        for (FsEventStorage storage : eventStorages) {
            storage.close();
        }

        FileUtil.deleteIfExists(Paths.get(rootDirectoryPath));
    }

    private static String buildRootDirectoryPath(Class executorClass) {
        final String tempDir = getTempDir().toString();
        final String root = tempDir + PATH_DELIMITER + executorClass.getSimpleName();
        return root;
    }

    /**
     * Creates an empty directory in the default temporary-file directory using {@link Files#createTempDirectory},
     * removes it and returns its parent directory.
     */
    private static Path getTempDir() {
        try {
            final String prefix = "";
            final Path tempDirToRemove = Files.createTempDirectory(prefix);
            final Path result = tempDirToRemove.getParent();
            FileUtil.deleteIfExists(tempDirToRemove);
            return result;
        } catch (IOException e) {
            throw propagate(e);
        }
    }

    private FsEventStorage tryCreateEventStorage() {
        try {
            return FsEventStorage.newInstance(rootDirectoryPath);
        } catch (IOException e) {
            throw propagate(e);
        }
    }

    private FsCommandStorage tryCreateCommandStorage() {
        try {
            return FsCommandStorage.newInstance(rootDirectoryPath);
        } catch (IOException e) {
            throw propagate(e);
        }
    }
}
