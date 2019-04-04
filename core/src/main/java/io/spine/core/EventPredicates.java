/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.core;

import com.google.protobuf.Timestamp;
import io.spine.time.Timestamps2;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Timestamps.checkValid;
import static com.google.protobuf.util.Timestamps.compare;

/**
 * Predicates for working with {@code Event}s.
 */
public final class EventPredicates {

    /** Prevent instantiation of this utility class. */
    private EventPredicates() {
    }

    /**
     * Creates a predicate for filtering events after the passed timestamp.
     */
    public static Predicate<Event> isAfter(Timestamp timestamp) {
        checkValid(timestamp);
        Predicate<Event> result = new IsAfter(timestamp);
        return result;
    }

    /**
     * Creates a predicate for filtering events after the passed timestamp.
     */
    public static Predicate<Event> isBefore(Timestamp timestamp) {
        checkValid(timestamp);
        Predicate<Event> result = new IsBefore(timestamp);
        return result;
    }

    /**
     * Creates a predicate to filter event records within a given time range.
     */
    public static Predicate<Event> isBetween(Timestamp start, Timestamp finish) {
        IsBetween.checkArguments(start, finish);
        Predicate<Event> result = new IsBetween(start, finish);
        return result;
    }

    /** The predicate to filter event records after some point in time. */
    private static class IsAfter implements Predicate<Event> {

        private final Timestamp timestamp;

        private IsAfter(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean test(@Nullable Event record) {
            if (record == null) {
                return false;
            }
            Timestamp ts = Events.getTimestamp(record);
            boolean result = compare(ts, this.timestamp) > 0;
            return result;
        }
    }

    /** The predicate to filter event records before some point in time. */
    private static class IsBefore implements Predicate<Event> {

        private final Timestamp timestamp;

        private IsBefore(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean test(@Nullable Event record) {
            if (record == null) {
                return false;
            }

            Timestamp ts = Events.getTimestamp(record);
            boolean result = compare(ts, this.timestamp) < 0;
            return result;
        }
    }

    /** The predicate to filter event records within a given time range. */
    public static class IsBetween implements Predicate<Event> {

        private final Timestamp start;
        private final Timestamp finish;

        private IsBetween(Timestamp start, Timestamp finish) {
            checkArguments(start, finish);
            this.start = start;
            this.finish = finish;
        }

        public static void checkArguments(Timestamp start, Timestamp finish) {
            checkNotNull(start);
            checkNotNull(finish);
            checkValid(start);
            checkValid(finish);
            checkArgument(compare(start, finish) < 0, "`start` must be before `finish`");
        }

        @Override
        public boolean test(@Nullable Event event) {
            if (event == null) {
                return false;
            }

            Timestamp ts = Events.getTimestamp(event);
            boolean result = Timestamps2.isBetween(ts, start, finish);
            return result;
        }
    }
}
