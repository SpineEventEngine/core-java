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

package org.spine3.client;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.UserId;
import org.spine3.protobuf.Messages;
import org.spine3.time.ZoneOffset;

import java.util.UUID;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;

/**
 * Utilities for working with {@link CommandRequest}.
 *
 * @author Alexander Yevsyukov
 */
public class CommandRequests {

    /**
     * Creates a new {@link CommandId} based on random UUID.
     *
     * @return new command ID
     */
    public static CommandId generateId() {
        final String value = UUID.randomUUID().toString();
        return CommandId.newBuilder().setUuid(value).build();
    }

    /**
     * Creates new command context with the current time
     * @param userId the actor id
     * @param offset the timezone offset
     */
    public static CommandContext createContext(UserId userId, ZoneOffset offset) {
        final CommandId commandId = generateId();
        final CommandContext.Builder result = CommandContext.newBuilder()
                .setActor(userId)
                .setTimestamp(getCurrentTime())
                .setCommandId(commandId)
                .setZoneOffset(offset);
        return result.build();
    }

    /**
     * Creates a new command request with the given {@code command} converted to {@link Any} and the {@code context}.
     *
     * @param command the command to convert to {@link Any} and set to the request
     * @param context the context to set to the request
     * @return a new command request
     */
    public static CommandRequest newCommandRequest(Message command, CommandContext context) {
        final CommandRequest.Builder request = CommandRequest.newBuilder()
                .setCommand(Messages.toAny(command))
                .setContext(context);
        return request.build();
    }

    private CommandRequests() {}
}
