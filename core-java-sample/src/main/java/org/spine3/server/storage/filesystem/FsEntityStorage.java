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
import org.spine3.server.storage.EntityStorage;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.server.storage.filesystem.FileSystemDepository.*;

/**
 * An entity storage based on the file system.
 *
 * @author Alexander Litus
 */
class FsEntityStorage<I, M extends Message> extends EntityStorage<I, M> {

    /**
     * Creates new storage instance.
     */
    protected static <I, M extends Message> EntityStorage<I, M> newInstance() {
        return new FsEntityStorage<>();
    }

    private FsEntityStorage() {}

    @Override
    public M read(I id) {

        final String idString = idToStringWithEscaping(id);

        final String path = getEntityStoreFilePath(idString);
        File file = new File(path);

        Message message = Any.getDefaultInstance();

        if (file.exists()) {
            message = readMessage(file);
        }

        @SuppressWarnings("unchecked") // We ensure type by writing this kind of messages.
        final M result = (M) message;
        return result;
    }

    @Override
    public void write(I id, M message) {

        checkNotNull(id);
        checkNotNull(message);

        final String idString = idToStringWithEscaping(id);

        final String path = getEntityStoreFilePath(idString);
        File file = new File(path);
        final Any any = toAny(message);
        writeMessage(file, any);
    }
}
