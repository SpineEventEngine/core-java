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

import com.google.common.base.Predicate;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.UserId;
import org.spine3.protobuf.Messages;
import org.spine3.protobuf.Timestamps;
import org.spine3.time.ZoneOffset;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;

/**
 * Client-side utilities for working with commands.
 *
 * @author Alexander Yevsyukov
 */
public class Commands {

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
     * @param zoneOffset the offset of the timezone in which the user works
     */
    public static CommandContext createContext(UserId userId, ZoneOffset zoneOffset) {
        final CommandId commandId = generateId();
        final CommandContext.Builder result = CommandContext.newBuilder()
                .setActor(userId)
                .setTimestamp(getCurrentTime())
                .setCommandId(commandId)
                .setZoneOffset(zoneOffset);
        return result.build();
    }

    /**
     * Creates a new command with the given {@code message} and the {@code context}.
     *
     * @param message the domain model message to send in the command
     * @param context the context of the command
     * @return a new command request
     */
    public static Command newCommand(Message message, CommandContext context) {
        final Command.Builder request = Command.newBuilder()
                .setMessage(Messages.toAny(message))
                .setContext(context);
        return request.build();
    }

    /**
     * Extracts the message from the passed {@code Command} instance.
     */
    public static Message getMessage(Command request) {
        final Message command = Messages.fromAny(request.getMessage());
        return command;
    }

    private Commands() {}

    public static Predicate<Command> wereAfter(final Timestamp from) {
        return new Predicate<Command>() {
            @Override
            public boolean apply(@Nullable Command request) {
                checkNotNull(request);
                final Timestamp timestamp = getTimestamp(request);
                return Timestamps.isAfter(timestamp, from);
            }
        };
    }

    public static Predicate<Command> wereWithinPeriod(final Timestamp from, final Timestamp to) {
        return new Predicate<Command>() {
            @Override
            public boolean apply(@Nullable Command request) {
                checkNotNull(request);
                final Timestamp timestamp = getTimestamp(request);
                return Timestamps.isBetween(timestamp, from, to);
            }
        };
    }

    private static Timestamp getTimestamp(Command request) {
        final Timestamp result = request.getContext().getTimestamp();
        return result;
    }

    /**
     * Sorts the command given command request list by command timestamp value.
     *
     * @param commands the command list to sort
     */
    public static void sort(List<Command> commands) {
        Collections.sort(commands, new Comparator<Command>() {
            @Override
            public int compare(Command o1, Command o2) {
                final Timestamp timestamp1 = getTimestamp(o1);
                final Timestamp timestamp2 = getTimestamp(o2);
                return Timestamps.compare(timestamp1, timestamp2);
            }
        });
    }
}
