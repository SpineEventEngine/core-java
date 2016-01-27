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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.EntityStorageRecord;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import static com.google.common.base.Throwables.propagate;
import static org.spine3.io.IoUtil.closeSilently;
import static org.spine3.io.file.FileUtil.*;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.server.storage.filesystem.FsStorageUtil.idToStringWithEscaping;
import static org.spine3.server.storage.filesystem.FsStorageUtil.writeMessage;

/**
 * An entity storage based on the file system.
 *
 * @author Alexander Litus
 */
class FsEntityStorage<I> extends EntityStorage<I> {

    private static final String ENTITY_STORE_DIR_NAME = "/entity-store/";

    private final String entityStorageRootPath;

    /**
     * Creates a new storage instance.
     * @param rootDirectoryPath an absolute path to the root storage directory (without the delimiter at the end)
     */
    protected static <I> EntityStorage<I> newInstance(
            String rootDirectoryPath) {
        return new FsEntityStorage<>(rootDirectoryPath);
    }

    private FsEntityStorage(String rootDirectoryPath) {
        this.entityStorageRootPath = rootDirectoryPath + ENTITY_STORE_DIR_NAME;
    }

    @Override
    protected EntityStorageRecord readInternal(I id) {
        final String idString = idToStringWithEscaping(id);
        final String filePath = createEntityFilePath(idString);
        final File file = tryCreateIfDoesNotExist(filePath);
        EntityStorageRecord record = null;
        if (file.exists()) {
            record = (EntityStorageRecord) readMessage(file);
        }
        return record;
    }

    @Override
    protected void writeInternal(I id, EntityStorageRecord record) {
        final String idString = idToStringWithEscaping(id);
        final String filePath = createEntityFilePath(idString);
        deleteFileIfExists(Paths.get(filePath));
        final File file = tryCreateIfDoesNotExist(filePath);
        final Any any = toAny(record);
        writeMessage(file, any);
    }

    /**
     * Reads a {@link Message} from a {@link File}.
     *
     * @param file the {@link File} to read from.
     * @return the message parsed from the file or {@code null}
     */
    @Nullable
    private static Message readMessage(File file) {
        checkFileExists(file, "entity storage");

        final InputStream fileInputStream = open(file);
        final InputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        Any any = Any.getDefaultInstance();
        try {
            any = Any.parseDelimitedFrom(bufferedInputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read message from file: " + file.getAbsolutePath(), e);
        } finally {
            closeSilently(fileInputStream, bufferedInputStream);
        }
        final Message result = (any != null) ? fromAny(any) : null;
        return result;
    }

    private String createEntityFilePath(String entityId) {
        return entityStorageRootPath + entityId;
    }

    private static File tryCreateIfDoesNotExist(String filePath) {
        try {
            return createIfDoesNotExist(filePath);
        } catch (IOException e) {
            throw propagate(e);
        }
    }
}
