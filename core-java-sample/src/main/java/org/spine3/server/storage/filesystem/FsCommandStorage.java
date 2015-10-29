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

import org.spine3.server.storage.CommandStorage;
import org.spine3.server.storage.CommandStoreRecord;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A storage for aggregate root events and snapshots based on the file system.
 *
 * @author Alexander Litus
 */
class FsCommandStorage extends CommandStorage {

    private final FsDepository depository;

    /**
     * Creates new storage instance.
     * @param depository to use for storing data.
     */
    protected static CommandStorage newInstance(FsDepository depository) {
        return new FsCommandStorage(depository);
    }

    private FsCommandStorage(FsDepository depository) {
        this.depository = depository;
    }

    @Override
    protected void write(CommandStoreRecord record) {

        checkNotNull(record);

        File file = depository.getCommandStoreFile();
        FsDepository.writeMessage(file, record);
    }
}
