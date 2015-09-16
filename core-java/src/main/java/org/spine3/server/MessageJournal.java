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
import com.google.protobuf.Timestamp;

import java.util.List;

//TODO:2015-09-16:alexander.yevsyukov: Review documentation of methods.

/**
 * A storage for messages associated with entity objects.
 *
 * <p>An entity can have more than one associated message.
 *
 * @param <M> the type of messages to store
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
public interface MessageJournal<I, M extends Message> {

    /**
     * Reads Messages of type {@link M} with appropriate Parent Id from storage.
     *
     * @param entityId the id of the entity to load messages for
     * @return read message
     */
    List<M> load(I entityId);

    /**
     * Stores message to storage. Storage should determine parent id by itself.
     *
     * @param id the ID of the entity
     * @param message message to store in storage
     */
    void store(I id, M message);

    /**
     * Loads messages for the object with the passed ID that have timestamp equal or after the passed value.
     *
     * @param entityId the id of the entity to load messages for
     * @param from     the timestamp from which load messages
     * @return list of messages or an empty list if no messages were found
     */
    List<M> loadSince(I entityId, Timestamp from);

    /**
     * Loads messages for all entities with the timestamp equal or after the passed value.
     *
     * @param from  timestamp to read messages from
     * @return the list of messages with the matching timestamp or an empty list if no messages were found
     */
    List<M> loadAllSince(Timestamp from);
}
