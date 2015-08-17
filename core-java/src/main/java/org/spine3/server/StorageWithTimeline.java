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
     * Reads Messages of type {@link M} from storage from chosen timestamp.
     *
     * @param from     timestamp to read messages from
     * @return read message
     */
    List<M> read(Timestamp from);

    /**
     * Reads Messages of type {@link M} with appropriate Parent Id from storage from chosen timestamp.
     *
     * @param parentId parent id of message
     * @param from     timestamp to read messages from
     * @return read message
     */
    List<M> read(Message parentId, Timestamp from);
}
