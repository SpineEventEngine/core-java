/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.base;

import com.google.common.base.Predicate;
import com.google.protobuf.Timestamp;
import org.spine3.protobuf.Timestamps2;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Predicates for working with {@code Event}s.
 *
 * @author Alexander Yevsyukov
 */
public class EventPredicates {

    private EventPredicates() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates a predicate for filtering events after the passed timestamp.
     */
    public static Predicate<Event> isAfter(Timestamp timestamp) {
        checkNotNull(timestamp);
        final Predicate<Event> result = new IsAfter(timestamp);
        return result;
    }

    /**
     * Creates a predicate for filtering events after the passed timestamp.
     */
    public static Predicate<Event> isBefore(Timestamp timestamp) {
        checkNotNull(timestamp);
        final Predicate<Event> result = new IsBefore(timestamp);
        return result;
    }

    /**
     * Creates a predicate to filter event records within a given time range.
     */
    public static Predicate<Event> isBetween(Timestamp start, Timestamp finish) {
        checkNotNull(start);
        checkNotNull(finish);
        final Predicate<Event> result = new IsBetween(start, finish);
        return result;
    }

    /** The predicate to filter event records after some point in time. */
    private static class IsAfter implements Predicate<Event> {

        private final Timestamp timestamp;

        public IsAfter(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean apply(@Nullable Event record) {
            if (record == null) {
                return false;
            }
            final Timestamp ts = Events.getTimestamp(record);
            final boolean result = Timestamps2.compare(ts, this.timestamp) > 0;
            return result;
        }
    }

    /** The predicate to filter event records before some point in time. */
    private static class IsBefore implements Predicate<Event> {

        private final Timestamp timestamp;

        public IsBefore(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean apply(@Nullable Event record) {
            if (record == null) {
                return false;
            }

            final Timestamp ts = Events.getTimestamp(record);
            final boolean result = Timestamps2.compare(ts, this.timestamp) < 0;
            return result;
        }
    }

    /** The predicate to filter event records within a given time range. */
    public static class IsBetween implements Predicate<Event> {

        private final Timestamp start;
        private final Timestamp finish;

        public IsBetween(Timestamp start, Timestamp finish) {
            checkNotNull(start);
            checkNotNull(finish);
            checkArgument(Timestamps2.compare(start, finish) < 0,
                          "`start` must be before `finish`");
            this.start = start;
            this.finish = finish;
        }

        @Override
        public boolean apply(@Nullable Event event) {
            if (event == null) {
                return false;
            }

            final Timestamp ts = Events.getTimestamp(event);
            final boolean result = Timestamps2.isBetween(ts, start, finish);
            return result;
        }
    }
}
