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

package org.spine3;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandRequest;
import org.spine3.util.Commands;
import org.spine3.engine.Storage;
import org.spine3.util.Messages;

import java.util.List;

/**
 * Stores and loads commands.
 *
 * @author Mikhail Mikhaylov
 */
public class CommandStore {

    private static final Class ENTITY_CLASS = CommandRequest.class;

    private Storage storage;

    public CommandStore(Storage storage) {
        this.storage = storage;
    }

    /**
     * Stores the command request.
     *
     * @param request command request to store
     */
    public void store(CommandRequest request) {
        final Message command = Messages.fromAny(request.getCommand());
        final Message aggregateId = Commands.getAggregateId(command);

        storage.store(
                ENTITY_CLASS,
                request,
                request.getContext().getCommandId(),
                aggregateId,
                request.getContext().getCommandId().getTimestamp(),
                0);
    }

    /**
     * Loads all commands for the given aggregate root id.
     *
     * @param aggregateRootId the id of the aggregate root
     * @return list of commands for the aggregate root
     */
    public List<CommandRequest> getCommands(Message aggregateRootId) {
        return storage.query(ENTITY_CLASS, aggregateRootId);
    }

    /**
     * Loads all commands for the given aggregate root id from given timestamp.
     *
     * @param aggregateRootId the id of the aggregate root
     * @param from            the timestamp to load commands from
     * @return list of commands for the aggregate root
     */
    public List<CommandRequest> getCommands(Message aggregateRootId, Timestamp from) {
        return storage.query(ENTITY_CLASS, aggregateRootId, from);
    }
}
