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

package org.spine3.sample.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.server.SnapshotStorage;
import org.spine3.server.StorageWithTimeline;
import org.spine3.server.StorageWithTimelineAndVersion;

/**
 * File system Server implementation.
 *
 * @author Mikhail Mikhaylov
 */
public class FileSystemSampleServer extends BaseSampleServer {
    private static final String STORAGE_PATH = "./storage";

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws Exception {
        FileSystemHelper.configure(STORAGE_PATH);
        final BaseSampleServer server = new FileSystemSampleServer();
        server.registerEventSubscribers();
        server.prepareEngine();
        server.start();
    }

    @Override
    protected Logger getLog() {
        return LogSingleton.INSTANCE.value;
    }

    @Override
    protected StorageWithTimelineAndVersion<EventRecord> provideEventStoreStorage() {
        return FileSystemStorageFactory.createEventStore();
    }

    @Override
    protected StorageWithTimeline<CommandRequest> provideCommandStoreStorage() {
        return FileSystemStorageFactory.createCommandStore();
    }

    @Override
    protected SnapshotStorage provideSnapshotStorage() {
        return FileSystemStorageFactory.createSnapshotStorage();
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FileSystemSampleServer.class);
    }
}
