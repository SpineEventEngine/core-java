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

package io.spine.time;

import com.google.common.base.Converter;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static io.spine.time.Time.MILLIS_PER_SECOND;
import static io.spine.time.Time.getCurrentTime;

/**
 * Converts from {@link TimeZone} to {@link ZoneOffset} using zone IDs and offsets.
 *
 * @author Alexander Yevsyukov
 */
final class ZoneConverter extends Converter<TimeZone, ZoneOffset> implements Serializable {

    private static final ZoneConverter INSTANCE = new ZoneConverter();

    private static final long serialVersionUID = 0L;
    private static final Set<String> supportedIds = ImmutableSet.copyOf(TimeZone.getAvailableIDs());

    static ZoneConverter getInstance() {
        return INSTANCE;
    }

    @Override
    protected ZoneOffset doForward(TimeZone timeZone) {
        final ZoneOffset result = toZoneOffset(timeZone);
        return result;
    }

    @Override
    protected TimeZone doBackward(ZoneOffset zoneOffset) {
        final TimeZone result = toTimeZone(zoneOffset);
        return result;
    }

    /**
     * Converts the passed {@code ZoneOffset} into {@code TimeZone}.
     *
     * <p>If the passed instance contains a supported zone ID, it will be used for obtaining the
     * time zone.
     *
     * <p>Otherwise {@code SimpleTimeZone} will be created using the offset.
     */
    private static TimeZone toTimeZone(ZoneOffset zoneOffset) {
        final Optional<String> optional = getId(zoneOffset);
        if (optional.isPresent()) {
            final String id = optional.get();
            if (supportedIds.contains(id)) {
                final TimeZone result = TimeZone.getTimeZone(id);
                return result;
            }
            return toSimpleTimeZone(zoneOffset);
        } else {
            return toSimpleTimeZone(zoneOffset);
        }
    }

    private static Optional<String> getId(ZoneOffset offset) {
        final String id = offset.getId()
                                .getValue();
        if (id.length() > 0) {
            return Optional.of(id);
        }
        return Optional.absent();
    }

    private static SimpleTimeZone toSimpleTimeZone(ZoneOffset zoneOffset) {
        @SuppressWarnings("NumericCastThatLosesPrecision")
        // OK as a valid zoneOffset isn't that big.
        final int offsetMillis = (int) TimeUnit.SECONDS.toMillis(zoneOffset.getAmountSeconds());
        final Optional<String> optional = getId(zoneOffset);
        final String id = optional.isPresent()
                ? optional.get()
                : "temp";
        return new SimpleTimeZone(offsetMillis, id);
    }

    /**
     * Obtains the {@code ZoneOffset} instance using {@code TimeZone}.
     *
     * @param timeZone target time zone
     * @return zone offset instance of specified timezone
     */
    private static ZoneOffset toZoneOffset(TimeZone timeZone) {
        final Timestamp now = getCurrentTime();
        final long date = Timestamps.toMillis(now);
        final int offsetInSeconds = getOffsetInSeconds(timeZone, date);
        @Nullable
        final String zoneId = timeZone.getID();
        return ZoneOffsets.create(offsetInSeconds, zoneId);
    }

    /**
     * Obtains offset of the passed {@code TimeZone} in seconds.
     */
    private static int getOffsetInSeconds(TimeZone timeZone, long date) {
        final int seconds = timeZone.getOffset(date) / MILLIS_PER_SECOND;
        return seconds;
    }

    @Override
    public String toString() {
        return "ZoneConverter.instance()";
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
