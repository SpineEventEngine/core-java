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
package org.spine3.protobuf;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.TimestampOrBuilder;
import com.google.protobuf.util.TimeUtil;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

/**
 * Utilities class for working with timestamps in addition to those available from {@link TimeUtil}.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 * @see TimeUtil
 */
@SuppressWarnings("UtilityClass")
public class Timestamps {

    private static final long NANOS_PER_MILLISECOND = 1_000_000;
    private static final long MILLISECONDS_PER_SECOND = 1000;
    private static final long MICROS_PER_SECOND = 1000000;
    private static final int SECONDS_PER_MINUTE = 60;

    /**
     * @return timestamp of the moment a minute ago from now.
     */
    public static Timestamp minuteAgo() {
        final Duration minute = TimeUtil.createDurationFromMillis(MICROS_PER_SECOND * SECONDS_PER_MINUTE * -1);
        final Timestamp result = TimeUtil.add(TimeUtil.getCurrentTime(), minute);
        return result;
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public static int compare(@Nullable Timestamp t1, @Nullable Timestamp t2) {
        if (t1 == null) {
            return t2 == null ? 0 : 1;
        }
        if (t2 == null) {
            return -1;
        }

        int result = Long.compare(t1.getSeconds(), t2.getSeconds());
        result = result == 0
                ? Integer.compare(t1.getNanos(), t2.getNanos())
                : result;
        return result;
    }

    public static boolean isBetween(Timestamp timestamp, Timestamp start, Timestamp finish) {
        final boolean isAfterStart = compare(start, timestamp) < 0;
        final boolean isBeforeFinish = compare(timestamp, finish) < 0;
        return isAfterStart && isBeforeFinish;
    }

    public static boolean isAfter(Timestamp timestamp, Timestamp from) {
        return (compare(from, timestamp) < 0);
    }

    public static Comparator<? super Timestamp> comparator() {
        return new TimestampComparator();
    }

    /*
     * Converts Timestamp to Date to the nearest millisecond.
     */
    public static Date convertToDate(TimestampOrBuilder timestamp) {

        final long millisecsFromNanos = timestamp.getNanos() / NANOS_PER_MILLISECOND;
        final long millisecsFromSeconds = timestamp.getSeconds() * MILLISECONDS_PER_SECOND;
        final Date date = new Date(millisecsFromSeconds + millisecsFromNanos);
        return date;
    }

    private static class TimestampComparator implements Comparator<Timestamp>, Serializable {
        @Override
        public int compare(Timestamp t1, Timestamp t2) {
            return Timestamps.compare(t1, t2);
        }

        private static final long serialVersionUID = 0;
    }

    private Timestamps() {
    }

}
