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

package org.spine3.test;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.spine3.base.Command;
import org.spine3.protobuf.Durations2;
import org.spine3.protobuf.Timestamps2;

import static com.google.protobuf.util.Timestamps.add;
import static org.spine3.validate.Validate.checkPositive;

/**
 * Utlity class for working with time-related tests.
 *
 * @author Alexander Yevsykov
 */
public class TimeTests {

    private TimeTests() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Returns the current time in seconds via {@link Timestamps2#getCurrentTime()}.
     *
     * @return a seconds value
     */
    public static long currentTimeSeconds() {
        final long secs = Timestamps2.getCurrentTime().getSeconds();
        return secs;
    }

    /**
     * Adjusts a timestamp in the context of the passed command.
     *
     * @return new command instance with the modified timestamp
     */
    public static Command adjustTimestamp(Command command, Timestamp timestamp) {
        final Command.Builder commandBuilder =
                command.toBuilder()
                       .setContext(command.getContext()
                                          .toBuilder()
                                          .setTimestamp(timestamp));
        return commandBuilder.build();
    }

    /**
     * The provider of current time, which is always the same.
     *
     * <p>Use this {@code Timestamps.Provider} in time-related tests that are sensitive to
     * bounds of minutes, hours, days, etc.
     */
    public static class FrozenMadHatterParty implements Timestamps2.Provider {
        private final Timestamp frozenTime;

        public FrozenMadHatterParty(Timestamp frozenTime) {
            this.frozenTime = frozenTime;
        }

        /** Returns the value passed to the constructor. */
        @Override
        public Timestamp getCurrentTime() {
            return frozenTime;
        }
    }

    /**
     * The time provider that can rewind current time.
     *
     * <p>Created in the future, {@linkplain #THIRTY_YEARS_IN_HOURS 30 years} from
     * the {@link #getCurrentTime() current time}.
     */
    public static class BackToTheFuture implements Timestamps2.Provider {

        public static final long THIRTY_YEARS_IN_HOURS = 262800L;

        private Timestamp currentTime;

        public BackToTheFuture() {
            this.currentTime = Timestamps.add(
                    Timestamps2.getCurrentTime(),
                    Durations2.hours(THIRTY_YEARS_IN_HOURS));
        }

        @Override
        public synchronized Timestamp getCurrentTime() {
            return this.currentTime;
        }

        private synchronized void setCurrentTime(Timestamp currentTime) {
            this.currentTime = currentTime;
        }

        public synchronized Timestamp forward(long hoursDelta) {
            checkPositive(hoursDelta);
            final Timestamp newTime = Timestamps.add(this.currentTime,
                                                     Durations2.hours(hoursDelta));
            setCurrentTime(newTime);
            return newTime;
        }

        public synchronized Timestamp backward(long hoursDelta) {
            checkPositive(hoursDelta);
            final Timestamp newTime = Timestamps.subtract(this.currentTime,
                                                          Durations2.hours(hoursDelta));
            setCurrentTime(newTime);
            return newTime;
        }
    }

    /**
     * Utility class for working with timestamps of the the future.
     */
    @VisibleForTesting
    public static class Future {

        private Future() {
            // Do not allow creating instances of this utility class.
        }

        /**
         * Obtains timestamp in the future a number of seconds from current time.
         *
         * @param seconds a positive number of seconds
         * @return the moment `value` seconds from now
         */
        public static Timestamp secondsFromNow(int seconds) {
            checkPositive(seconds);
            final Timestamp currentTime = Timestamps2.getCurrentTime();
            final Timestamp result = add(currentTime, Durations2.ofSeconds(seconds));
            return result;
        }

        public static boolean isFuture(Timestamp timestamp) {
            // Do not use `getCurrentTime()` as we may use custom `TimestampProvider` already.
            // Get time from metal.
            final Timestamp currentSystemTime = Timestamps2.systemTime();

            // NOTE: we have the risk of having these two timestamps too close to each other
            // so that the passed timestamp becomes “the past” around the time of this call.
            // To avoid this, select some time in the “distant” future.
            final boolean result = Timestamps.comparator()
                                             .compare(currentSystemTime, timestamp) < 0;
            return result;
        }
    }
}
