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
import org.spine3.base.CommandRequest;
import org.spine3.engine.Media;

import java.util.List;

/**
 * Stores and loads the commands.
 *
 * @author Mikhail Melnik
 */
public class CommandStore {

    private Media media;

    public CommandStore(Media media) {
        this.media = media;
    }

    /**
     * Loads all commands for the given aggregate root id.
     *
     * @param aggregateRootId the id of the aggregate root
     * @return list of commands for the aggregate root
     */
    List<CommandRequest> load(Message aggregateRootId) {
        return media.readCommands(aggregateRootId);
    }

    /**
     * Stores the command request.
     *
     * @param request command request to store
     */
    void store(CommandRequest request) {
        media.writeCommand(request);
    }

}
