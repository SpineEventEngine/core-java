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

import java.util.List;

/**
 * A storage for messages associated with entity objects.
 *
 * <p>An entity can have more than one associated message.
 *
 * @param <M> Message type to store
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
public interface EntityMessageStorage<M extends Message> {

    //TODO:2015-09-06:alexander.yevsyukov: Have Id as another parameterizing type.

    /**
     * Reads Messages of type {@link M} with appropriate Parent Id from storage.
     *
     * @param parentId parent id of message
     * @return read message
     */
    List<M> load(Message parentId);

    /**
     * Stores message to storage. Storage should determine parent id by itself.
     *
     * @param message message to store in storage
     */
    void store(M message);

}
