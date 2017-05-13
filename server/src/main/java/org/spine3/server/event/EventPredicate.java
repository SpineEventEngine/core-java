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

package org.spine3.server.event;

import com.google.protobuf.Timestamp;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.spine3.annotation.SPI;
import org.spine3.base.Event;
import org.spine3.base.Events;
import org.spine3.time.Timestamps2;

import static com.google.protobuf.util.Timestamps.checkValid;
import static org.spine3.base.EventPredicates.IsBetween.checkArguments;

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

        public static EventPredicate isTrue() {
            return Value.ALWAYS_TRUE;
        }

        public static EventPredicate isFalse() {
            return Value.ALWAYS_FALSE;
        }
    }

    class Time {
        private Time() {
            // Prevent instantiation of this utility class.
        }

        public static IsAfter isAfter(Timestamp timestamp) {
            checkValid(timestamp);
            return new IsAfter(timestamp);
        }

        public static IsBefore isBefore(Timestamp timestamp) {
            checkValid(timestamp);
            return new IsBefore(timestamp);
        }

        public static IsBetween isBetween(Timestamp start, Timestamp finish) {
            checkArguments(start, finish);
            return new IsBetween(start, finish);
        }
    }

    abstract class TimePredicate implements EventPredicate {
        private static final long serialVersionUID = 0L;
        static Timestamp timeOf(Event input) {
            return Events.getTimestamp(input);
        }
    }

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
}
