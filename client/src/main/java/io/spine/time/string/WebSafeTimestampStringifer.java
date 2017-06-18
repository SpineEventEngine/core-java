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

package io.spine.time.string;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.string.Stringifier;

import java.io.Serializable;
import java.text.ParseException;
import java.util.regex.Pattern;

import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * The stringifier for web-safe representation of timestamps.
 *
 * <p>The stringifier replaces colons in the time part of a a RFC 3339 date string
 * with dashes when converting a timestamp to a string. It also restores the colons
 * back during the backward conversion.
 *
 * @author Alexander Yevsyukov
 */
final class WebSafeTimestampStringifer extends Stringifier<Timestamp> implements Serializable {

    private static final long serialVersionUID = 0L;
    private static final WebSafeTimestampStringifer INSTANCE = new WebSafeTimestampStringifer();

    private static final char COLON = ':';
    private static final Pattern PATTERN_COLON = Pattern.compile(String.valueOf(COLON));
    private static final String DASH = "-";

    /**
     * The index of a character separating hours and minutes.
     */
    private static final int HOUR_SEPARATOR_INDEX = 13;
    /**
     * The index of a character separating minutes and seconds.
     */
    private static final int MINUTE_SEPARATOR_INDEX = 16;

    static WebSafeTimestampStringifer getInstance() {
        return INSTANCE;
    }

    /**
     * Converts the passed timestamp string into a web-safe string, replacing colons to dashes.
     */
    private static String toWebSafe(String str) {
        final String result = PATTERN_COLON.matcher(str)
                                           .replaceAll(DASH);
        return result;
    }

    /**
     * Converts the passed web-safe timestamp representation to the RFC 3339 date string format.
     */
    private static String fromWebSafe(String webSafe) {
        char[] chars = webSafe.toCharArray();
        chars[HOUR_SEPARATOR_INDEX] = COLON;
        chars[MINUTE_SEPARATOR_INDEX] = COLON;
        return String.valueOf(chars);
    }

    @Override
    protected String toString(Timestamp timestamp) {
        String result = Timestamps.toString(timestamp);
        result = toWebSafe(result);
        return result;
    }

    @Override
    @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
    // It is OK because all necessary information from caught exception is passed.
    protected Timestamp fromString(String webSafe) {
        try {
            final String rfcStr = fromWebSafe(webSafe);
            return Timestamps.parse(rfcStr);
        } catch (ParseException e) {
            throw newIllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return "TimeStringifiers.forTimestampWebSafe()";
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
