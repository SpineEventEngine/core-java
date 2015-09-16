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
 * Extends {@link Storage} and provides an ability to work with Timestamp.
 *
 * @param <M> Message type to store
 * @author Mikhail Mikhaylov
 */
public interface StorageWithTimeline<M extends Message> extends Storage<M> {

    /**
     * Loads messages with the timestamp equal or after the passed value.
     *
     * @param from  timestamp to read messages from
     * @return the list of messages with the matching timestamp or an empty list if no messages were found
     */
    List<M> load(Timestamp from);

    /**
     * Loads messages for the object with the passed ID that have timestamp equal or after the passed value.
     *
     * @param id the id of the object to load messages for
     * @param from     timestamp to read messages from
     * @return list of messages or an empty list if no messages were found
     */
    List<M> load(Message id, Timestamp from);
}
