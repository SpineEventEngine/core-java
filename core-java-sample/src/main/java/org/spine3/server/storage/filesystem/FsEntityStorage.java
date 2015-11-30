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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.protobuf.Messages;
import org.spine3.server.storage.EntityStorage;
import org.spine3.util.IoUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.server.storage.filesystem.FsUtil.idToStringWithEscaping;
import static org.spine3.util.IoUtil.*;

/**
 * An entity storage based on the file system.
 *
 * @author Alexander Litus
 */
class FsEntityStorage<I, M extends Message> extends EntityStorage<I, M> {

    private static final String ENTITY_STORE_DIR_NAME = "/entity-store/";

    private final String entityStorageRootPath;

    /**
     * Creates a new storage instance.
     * @param rootDirectoryPath an absolute path to the root storage directory (without the delimiter at the end)
     */
    protected static <I, M extends Message> EntityStorage<I, M> newInstance(String rootDirectoryPath) {
        return new FsEntityStorage<>(rootDirectoryPath);
    }

    private FsEntityStorage(String rootDirectoryPath) {
        this.entityStorageRootPath = rootDirectoryPath + ENTITY_STORE_DIR_NAME;
    }

    @Override
    public M read(I id) {

        final String idString = idToStringWithEscaping(id);
        final File file = getEntityStoreFile(idString);
        Message message = null;

        if (file.exists()) {
            message = readMessage(file);
        }

        @SuppressWarnings("unchecked") // We ensure type by writing this kind of messages.
        final M result = (M) message;
        return result;
    }

    @Override
    @SuppressWarnings("DuplicateStringLiteralInspection")
    public void write(I id, M message) {

        checkNotNull(id, "id");
        checkNotNull(message, "message");

        final String idString = idToStringWithEscaping(id);

        final File file = getEntityStoreFile(idString);
        final Any any = toAny(message);

        FsUtil.writeMessage(file, any);
    }

    /**
     * Reads {@link com.google.protobuf.Message} from {@link java.io.File}.
     *
     * @param file the {@link java.io.File} to read from.
     */
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

        final Message result = (any != null) ? Messages.fromAny(any) : null;
        return result;
    }

    /**
     * @return entity file path.
     */
    public File getEntityStoreFile(String entityId) {
        final String filePath = entityStorageRootPath + entityId;
        try {
            return IoUtil.createIfDoesNotExist(filePath);
        } catch (IOException e) {
            throw propagate(e);
        }
    }
}
