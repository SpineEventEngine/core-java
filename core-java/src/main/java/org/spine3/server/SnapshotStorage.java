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

package org.spine3.server;

import com.google.protobuf.Message;

//TODO:2015-09-16:alexander.yevsyukov: Have generic parameter for type of IDs.

/**
 * Defines the low level data interface of the storage
 * that is used to read and write Snapshots as Protobuf messages.
 *
 * @author Mikhail Mikhaylov
 */
public interface SnapshotStorage {

    /**
     * Stores Snapshot with desired Parent Id.
     *
     * @param parentId parent id to identify snapshots of different Aggregate Root instances
     * @param snapshot snapshot to be stored
     */
    void store(Message parentId, Snapshot snapshot);

    /**
     * Reads snapshot from storage by appropriate parent id.
     *
     * @param parentId parent id to identify snapshots of different Aggregate Root instances
     * @return read snapshot
     */
    Snapshot load(Message parentId);

}
