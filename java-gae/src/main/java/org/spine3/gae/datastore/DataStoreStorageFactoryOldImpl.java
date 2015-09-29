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

package org.spine3.gae.datastore;

import org.spine3.TypeName;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.server.MessageJournal;
import org.spine3.server.aggregate.SnapshotStorage;

/**
 * This class provides factory method for DataStore-based storages.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class DataStoreStorageFactoryOldImpl {

    private DataStoreStorageFactoryOldImpl() {
    }

    /**
     * Creates a new storage instance.
     *
     * @return Storage with Timeline and Version
     */
    public static MessageJournal<String, EventRecord> createEventStoreStorage() {
        return DataStoreMessageJournal.newInstance(EventRecord.class);
    }

    /**
     * Creates new snapshot storage instance.
     *
     * @param stateTypeName aggregate root state type name to be used as snapshot kind
     * @return Snapshot Storage
     */
    public static SnapshotStorage createSnapshotStorage(TypeName stateTypeName) {
        return new DataStoreSnapshotStorage(stateTypeName);
    }

    /**
     * Creates new storage for command requests.
     *
     * @return Storage with Timeline
     */
    public static MessageJournal<String, CommandRequest> createCommandStoreStorage() {
        return DataStoreMessageJournal.newInstance(CommandRequest.class);
    }
}
