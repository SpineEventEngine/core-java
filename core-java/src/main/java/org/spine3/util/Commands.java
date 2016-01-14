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
package org.spine3.util;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.UserId;
import org.spine3.client.CommandRequest;
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
import static org.spine3.util.Identifiers.IdConverterRegistry;
import static org.spine3.util.Identifiers.NULL_ID_OR_FIELD;

/**
 * Utility class for working with commands.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class Commands {

    static {
        IdConverterRegistry.getInstance().register(CommandId.class, new CommandIdToStringConverter());
    }

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

    @SuppressWarnings("StringBufferWithoutInitialCapacity")
    public static class CommandIdToStringConverter implements Function<CommandId, String> {
        @Override
        public String apply(@Nullable CommandId commandId) {
            if (commandId == null) {
                return NULL_ID_OR_FIELD;
            }

            return commandId.getUuid();
        }
    }

    /**
     * Creates string representation of the passed command ID.
     *
     * @param commandId the ID to convert
     * @return string value, with the format defined by {@link Identifiers#idToString(Object)}
     * @see Identifiers#idToString(Object)
     */
    public static String idToString(CommandId commandId) {
        final String result = Identifiers.idToString(commandId);
        return result;
    }

    public static Predicate<CommandRequest> wereAfter(final Timestamp from) {
        return new Predicate<CommandRequest>() {
            @Override
            public boolean apply(@Nullable CommandRequest request) {
                checkNotNull(request);
                final Timestamp timestamp = getTimestamp(request);
                return Timestamps.isAfter(timestamp, from);
            }
        };
    }

    public static Predicate<CommandRequest> wereWithinPeriod(final Timestamp from, final Timestamp to) {
        return new Predicate<CommandRequest>() {
            @Override
            public boolean apply(@Nullable CommandRequest request) {
                checkNotNull(request);
                final Timestamp timestamp = getTimestamp(request);
                return Timestamps.isBetween(timestamp, from, to);
            }
        };
    }

    private static Timestamp getTimestamp(CommandRequest request) {
        final Timestamp result = request.getContext().getTimestamp();
        return result;
    }

    /**
     * Sorts the command given command request list by command timestamp value.
     *
     * @param commandRequests the command request list to sort
     */
    public static void sort(List<CommandRequest> commandRequests) {
        Collections.sort(commandRequests, new Comparator<CommandRequest>() {
            @Override
            public int compare(CommandRequest o1, CommandRequest o2) {
                final Timestamp timestamp1 = getTimestamp(o1);
                final Timestamp timestamp2 = getTimestamp(o2);
                return Timestamps.compare(timestamp1, timestamp2);
            }
        });
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

    //@formatter:off
    private Commands() {}
}
