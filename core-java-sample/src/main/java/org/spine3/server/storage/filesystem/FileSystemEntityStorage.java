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

import com.google.protobuf.Message;
import org.spine3.server.storage.EntityStorage;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.server.storage.filesystem.FileSystemHelper.readEntity;
import static org.spine3.server.storage.filesystem.FileSystemHelper.writeEntity;
import static org.spine3.util.Identifiers.idToString;

public class FileSystemEntityStorage<I, M extends Message> extends EntityStorage<I, M> {

    protected static <I, M extends Message> EntityStorage<I, M> newInstance() {
        return new FileSystemEntityStorage<>();
    }

    private FileSystemEntityStorage() {}

    @Override
    public M read(I id) {

        final String idString = idToString(id);

        Message message = readEntity(idString);

        @SuppressWarnings("unchecked") // We ensure type by writing this kind of messages.
        final M result = (M) message;
        return result;
    }

    @Override
    public void write(I id, M message) {

        checkNotNull(id);
        checkNotNull(message);

        final String idString = idToString(id);

        writeEntity(idString, message);
    }
}
