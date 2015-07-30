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
package org.spine3.sample.store.filesystem;

import com.google.protobuf.Message;
import org.spine3.base.Snapshot;
import org.spine3.engine.SnapshotStorage;

import java.io.*;

/**
 * Test file system based implementation of the {@link Message} repository.
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public class FileSystemSnapshotStorage implements SnapshotStorage {

    /**
     * Writes the snapshot of the given aggregate root to storage.
     *
     * @param parentId parent id for snapshot
     * @param snapshot the snapshot to store
     */
    @Override
    public void store(Snapshot snapshot, Message parentId) {
        File file = FileSystemHelper.getSnapshotsFile(parentId);

        FileSystemHelper.writeMessage(file, snapshot);
    }

    /**
     * Returns the last snapshot for the given aggregate root.
     *
     * @param parentId parent id for snapshot
     * @return the {@link Snapshot} object
     */
    @Override
    public Snapshot read(Message parentId) {
        File file = FileSystemHelper.getSnapshotsFile(parentId);

        Snapshot snapshot = readLastSnapshotFromFile(file);
        return snapshot;
    }

    private static Snapshot readLastSnapshotFromFile(File file) {
        if (file.exists()) {
            InputStream inputStream = null;

            try {
                inputStream = FileSystemHelper.getObjectInputStream(file);

                Snapshot snapshot = Snapshot.parseDelimitedFrom(inputStream);
                return snapshot;
            } catch (IOException e) {
                //NOP
            } finally {
                FileSystemHelper.closeSilently(inputStream);
            }
        }
        return null;
    }

}
