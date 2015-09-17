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

/**
 * A storage for messages associated with entity objects.
 *
 * <p>An entity can have more than one associated message.
 *
 * @param <I> the type of entity IDs
 * @param <M> the type of messages associated with entities
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
public interface MessageJournal<I, M extends Message> {

    /**
     * Loads all messages for the entity with the passed ID.
     *
     * @param entityId the ID of the entity to load messages for
     * @return list of messages or empty list if no messages were found
     */
    List<M> load(I entityId);

    /**
     * Store a message for the entity with the passed ID.
     *
     * @param entityId the ID of the entity
     * @param message a message to store
     */
    void store(I entityId, M message);

    /**
     * Loads messages for the entity recorded on or after specified time.
     *
     * @param entityId the id of the entity to load messages for
     * @param timestamp the timestamp from which load messages
     * @return list of messages or an empty list if no matching messages were found
     */
    List<M> loadSince(I entityId, Timestamp timestamp);

    /**
     * Loads messages for all entities with the timestamp equal or after the passed value.
     *
     * @param timestamp the timestamp from which load messages
     * @return the list of messages with the matching timestamp or an empty list if no messages were found
     */
    List<M> loadAllSince(Timestamp timestamp);
}
