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
package org.spine3.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.protobuf.Timestamp;
import org.spine3.base.*;
import org.spine3.protobuf.Timestamps;
import org.spine3.time.ZoneOffset;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.util.Identifiers.*;
import static org.spine3.util.Users.newUserId;

/**
 * Utility class for working with {@link CommandId} and {@link CommandContext} objects.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class Commands {

    static {
        IdConverterRegistry.getInstance().register(CommandId.class, new CommandIdToStringConverter());
    }

    private Commands() {
        // Prevent instantiation.
    }

    /**
     * Creates a new {@link CommandId} taking passed {@link UserId} object and current system time.
     *
     * @param userId ID of the user who originates the command
     * @return new command ID
     */
    public static CommandId generateId(UserId userId) {
        checkNotNull(userId);

        final Timestamp currentTime = getCurrentTime();
        return create(userId, currentTime);
    }

    /**
     * Creates a command ID instance by user ID and timestamp.
     *
     * <p>To create new instances of {@link CommandId} use {@link Commands#generateId(UserId)}.
     * This method should be used for testing purposes or for creating command IDs for working with
     * historical data.
     *
     * @param userId the user to whom generate command ID
     * @param timestamp known timestamp for previously generated command ID
     * @return command ID instance
     */
    @VisibleForTesting
    public static CommandId create(UserId userId, Timestamp timestamp) {
        return CommandId.newBuilder()
                .setActor(userId)
                .setTimestamp(timestamp)
                .build();
    }

    /**
     * Creates new Command context with current time
     * @param userId the actor id
     * @param offset the timezone offset
     */
    public static CommandContext createContext(UserId userId, ZoneOffset offset) {
        final CommandId commandId = create(userId, getCurrentTime());
        final CommandContext.Builder result = CommandContext.newBuilder()
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

            final StringBuilder builder = new StringBuilder();

            String userId = NULL_ID_OR_FIELD;

            if (commandId.getActor() != null) {
                userId = commandId.getActor().getValue();
            }

            final String commandTime = timestampToString(commandId.getTimestamp());

            builder.append(userId)
                    .append(USER_ID_AND_TIME_DELIMITER)
                    .append(commandTime);

            return builder.toString();
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

    public static CommandId generateId(String userIdString, Timestamp currentTime) {
        final UserId userId = newUserId(userIdString);
        final CommandId.Builder builder = CommandId.newBuilder()
                .setActor(userId)
                .setTimestamp(currentTime);
        return builder.build();
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

    private static Timestamp getTimestamp(CommandRequestOrBuilder request) {
        final CommandId commandId = request.getContext().getCommandId();
        final Timestamp result = commandId.getTimestamp();
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
}
