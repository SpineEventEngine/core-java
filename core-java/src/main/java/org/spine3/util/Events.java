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
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.CommandId;
import org.spine3.base.EventId;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.util.Identifiers.*;

/**
 * Utility class for working with {@link EventId} objects.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"UtilityClass", "TypeMayBeWeakened"})
public class Events {

    static {
        IdConverterRegistry.getInstance().register(EventId.class, new EventIdToStringConverter());
    }

    /**
     * Generates new {@link EventId} by the passed {@link CommandId} and current system time.
     *
     * @param commandId ID of the command, which originated the event
     * @return new event ID
     */
    public static EventId generateId(CommandId commandId) {
        return createId(commandId, getCurrentTime());
    }

    /**
     * Creates new {@link EventId} by the passed {@link CommandId} and passed timestamp.
     *
     * @param commandId ID of the command, which originated the event
     * @param timestamp the moment of time the event happened
     * @return new event ID
     */
    public static EventId createId(CommandId commandId, Timestamp timestamp) {
        final Duration distance = TimeUtil.distance(commandId.getTimestamp(), checkNotNull(timestamp));
        final long delta = TimeUtil.toNanos(distance);

        final EventId.Builder builder = EventId.newBuilder()
                .setCommandId(checkNotNull(commandId))
                .setDeltaNanos(delta);
        return builder.build();
    }

    /**
     * Converts {@code EventId} into Json string.
     *
     * @param id the id to convert
     * @return Json representation of the id
     */
    @SuppressWarnings("TypeMayBeWeakened") // We want to limit the number of types that can be converted to Json.
    public static String idToString(EventId id) {
        return Identifiers.idToString(id);
    }

    @SuppressWarnings("StringBufferWithoutInitialCapacity")
    public static class EventIdToStringConverter implements Function<EventId, String> {
        @Override
        public String apply(@Nullable EventId eventId) {

            if (eventId == null) {
                return NULL_ID_OR_FIELD;
            }

            final StringBuilder builder = new StringBuilder();

            final CommandId commandId = eventId.getCommandId();

            String userId = NULL_ID_OR_FIELD;

            if (commandId != null && commandId.getActor() != null) {
                userId = commandId.getActor().getValue();
            }

            final String commandTime = (commandId != null) ? timestampToString(commandId.getTimestamp()) : "";

            builder.append(userId)
                    .append(USER_ID_AND_TIME_DELIMITER)
                    .append(commandTime)
                    .append(TIME_DELIMITER)
                    .append(eventId.getDeltaNanos());

            return builder.toString();
        }

    }

    // @formatter:off
    private Events() {}
}
