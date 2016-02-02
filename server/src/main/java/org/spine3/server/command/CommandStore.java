/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.command;

import org.spine3.base.Command;
import org.spine3.server.aggregate.AggregateId;
import org.spine3.server.storage.CommandStorage;

import static com.google.common.base.Preconditions.checkState;

/**
 * Manages commands received by the system.
 *
 * //TODO:2016-01-25:alexander.yevsyukov: Add API for updating status and loading.
 *
 * @author Mikhail Mikhaylov
 */
public class CommandStore implements AutoCloseable {

    private final CommandStorage storage;

    public CommandStore(CommandStorage storage) {
        this.storage = storage;
    }

    /**
     * Stores the command request.
     *
     * @param request command request to store
     */
    public void store(Command request) {
        checkState(storage.isOpen(), "Unable to store to closed storage.");

        final AggregateId aggregateId = AggregateId.fromCommand(request);
        //TODO:2016-01-15:alexander.yevsyukov: write with the "RECEIVED" status.
        storage.store(aggregateId, request);
    }

    @Override
    public void close() throws Exception {
        storage.close();
    }

    /**
     * @return true if the store is open, false otherwise
     */
    public boolean isOpen() {
        return storage.isOpen();
    }

    /**
     * @return true if the store is closed, false otherwise
     */
    public boolean isClosed() {
        return !isOpen();
    }
}
