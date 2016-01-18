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
import org.spine3.base.EventId;
import org.spine3.base.EventRecord;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.spine3.util.Identifiers.IdConverterRegistry;
import static org.spine3.util.Identifiers.NULL_ID_OR_FIELD;

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
     * Generates a new random UUID-based {@code EventId}.
     */
    public static EventId generateId() {
        final String value = UUID.randomUUID().toString();
        return EventId.newBuilder().setUuid(value).build();
    }

    /**
     * Sorts the given event record list by the event timestamps.
     *
     * @param eventRecords the event record list to sort
     */
    public static void sort(List<EventRecord> eventRecords) {
        Collections.sort(eventRecords, EventRecords.EVENT_RECORD_COMPARATOR);
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

            return eventId.getUuid();
        }

    }

    // @formatter:off
    private Events() {}
}
