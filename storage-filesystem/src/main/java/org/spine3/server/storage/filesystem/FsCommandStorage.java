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

import org.spine3.io.file.FileUtil;
import org.spine3.server.storage.CommandStorage;
import org.spine3.server.storage.CommandStorageRecord;

import java.io.File;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.server.storage.filesystem.FsUtil.writeMessage;

/**
 * A storage for aggregate root events and snapshots based on the file system.
 *
 * @author Alexander Litus
 */
class FsCommandStorage extends CommandStorage {

    private static final String COMMAND_STORE_FILE_NAME = "/command-store";
    private final File commandStoreFile;

    /**
     * Creates new storage instance.
     * @param rootDirectoryPath an absolute path to the root storage directory (without the delimiter at the end)
     * @throws IOException - if failed to create command storage file
     */
    protected static FsCommandStorage newInstance(String rootDirectoryPath) throws IOException {
        return new FsCommandStorage(rootDirectoryPath);
    }

    private FsCommandStorage(String rootDirectoryPath) throws IOException {
        commandStoreFile = FileUtil.createIfDoesNotExist(rootDirectoryPath + COMMAND_STORE_FILE_NAME);
    }

    @Override
    protected void write(CommandStorageRecord record) {

        checkNotNull(record);
        writeMessage(commandStoreFile, record);
    }

    @Override
    public void close() throws IOException {
        //NOP
    }
}
