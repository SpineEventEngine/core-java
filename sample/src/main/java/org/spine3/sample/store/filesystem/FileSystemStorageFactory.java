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

import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.server.SnapshotStorage;
import org.spine3.server.StorageWithTimeline;
import org.spine3.server.StorageWithTimelineAndVersion;

/**
 * This class provides factory methods for creating storages based on file system.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class FileSystemStorageFactory {

    private FileSystemStorageFactory() {
    }

    /**
     * Creates new instance of the event store.
     *
     * @return new storage instance
     */
    public static StorageWithTimelineAndVersion<EventRecord> createEventStore() {
        return FileSystemStorage.newInstance(EventRecord.class);
    }

    /**
     * Creates new snapshot storage.
     *
     * @return new storage instance
     */
    public static SnapshotStorage createSnapshotStorage() {
        return new FileSystemSnapshotStorage();
    }

    /**
     * Creates new command store.
     *
     * @return new storage instance
     */
    public static StorageWithTimeline<CommandRequest> createCommandStore() {
        return FileSystemStorage.newInstance(CommandRequest.class);
    }
}
