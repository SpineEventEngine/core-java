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

package io.spine.server.event;

import com.google.protobuf.Timestamp;
import io.spine.annotation.SPI;
import io.spine.base.Event;
import io.spine.base.Events;
import io.spine.time.Timestamps2;
import org.apache.beam.sdk.transforms.SerializableFunction;

import static com.google.protobuf.util.Timestamps.checkValid;
import static io.spine.base.EventPredicates.IsBetween.checkArguments;

/**
 * A serializable replacement for {@code Predicate<Event>} for Beam-related functionality.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public interface EventPredicate extends SerializableFunction<Event, Boolean> {

    class Always {
        @SuppressWarnings("InnerClassTooDeeplyNested")
        private enum Value implements EventPredicate {
            ALWAYS_TRUE {
                @Override
                public Boolean apply(Event input) {
                    return Boolean.TRUE;
                }

                @Override
                public String toString() {
                    return "EventPredicate.Always.isTrue()";
                }
            },

            ALWAYS_FALSE {
                @Override
                public Boolean apply(Event input) {
                    return Boolean.FALSE;
                }

                @Override
                public String toString() {
                    return "EventPredicate.Always.isFalse()";
                }
            }
        }

        private Always() {
            // Prevent instantiation of this utility class.
        }

        /**
         * Obtains a predicate that accepts all the events.
         */
        public static EventPredicate isTrue() {
            return Value.ALWAYS_TRUE;
        }

        /**
         * Obtains a predicate that rejects all the events.
         */
        public static EventPredicate isFalse() {
            return Value.ALWAYS_FALSE;
        }
    }

    /**
     * Provides time-related predicates.
     */
    class Time {
        private Time() {
            // Prevent instantiation of this utility class.
        }

        /**
         * Obtains a predicate that accepts events with timestamps after the passed.
         */
        public static IsAfter isAfter(Timestamp timestamp) {
            checkValid(timestamp);
            return new IsAfter(timestamp);
        }

        /**
         * Obtains a predicate that accepts events with timestamps before the passed.
         */
        public static IsBefore isBefore(Timestamp timestamp) {
            checkValid(timestamp);
            return new IsBefore(timestamp);
        }

        /**
         * Obtains a predicate that accepts events which timestamp is
         * {@linkplain Timestamps2#isBetween(Timestamp, Timestamp, Timestamp) within} the passed
         * timestamp range.
         */
        public static IsBetween isBetween(Timestamp start, Timestamp finish) {
            checkArguments(start, finish);
            return new IsBetween(start, finish);
        }
    }

    /**
     * Abstract base for time-related event predicates.
     */
    abstract class TimePredicate implements EventPredicate {
        private static final long serialVersionUID = 0L;
        static Timestamp timeOf(Event input) {
            return Events.getTimestamp(input);
        }
    }

    /**
     * Accepts events with the timestamp after the passed.
     */
    class IsAfter extends TimePredicate {
        private static final long serialVersionUID = 0L;
        private final Timestamp timestamp;

        private IsAfter(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public Boolean apply(Event input) {
            final Timestamp ts = timeOf(input);
            final boolean result = Timestamps2.compare(ts, this.timestamp) > 0;
            return result;
        }
    }

    /**
     * Accepts events that are before the passed timestamp.
     */
    class IsBefore extends TimePredicate {
        private static final long serialVersionUID = 0L;
        private final Timestamp timestamp;

        private IsBefore(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public Boolean apply(Event input) {
            final Timestamp ts = timeOf(input);
            final boolean result = Timestamps2.compare(ts, this.timestamp) > 0;
            return result;
        }
    }

    /**
     * Accepts events which timestamp is
     * {@linkplain Timestamps2#isBetween(Timestamp, Timestamp, Timestamp) within} the passed
     * timestamp range.
     */
    class IsBetween extends TimePredicate {
        private static final long serialVersionUID = 0L;
        private final Timestamp start;
        private final Timestamp finish;

        private IsBetween(Timestamp start, Timestamp finish) {
            this.start = start;
            this.finish = finish;
        }

        @Override
        public Boolean apply(Event input) {
            final Timestamp ts = timeOf(input);
            final boolean result = Timestamps2.isBetween(ts, start, finish);
            return result;
        }
    }

    /**
     * Filters events matching a {@link EventStreamQuery}.
     */
    class Query implements EventPredicate {
        private static final long serialVersionUID = 0L;
        private final EventPredicate filter;

        /**
         * Creates a new predicate for the passed query.
         */
        public static Query of(EventStreamQuery query) {
            return new Query(query);
        }

        private Query(EventStreamQuery query) {
            //TODO:2017-06-07:alexander.yevsyukov: Change impl.
            this.filter = null; // new MatchesStreamQuery(query);
        }

        @Override
        public Boolean apply(Event input) {
            final boolean result = filter.apply(input);
            return result;
        }
    }
}
