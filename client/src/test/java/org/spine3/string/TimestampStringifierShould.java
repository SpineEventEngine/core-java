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

package org.spine3.string;

import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.string.time.TimeStringifiers;

import static org.junit.Assert.assertEquals;
import static org.spine3.time.Timestamps2.getCurrentTime;

/**
 * @author Alexander Yevsyukov
 */
public class TimestampStringifierShould {

    private static final Stringifier<Timestamp> stringifier = TimeStringifiers.forTimestamp();

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_try_to_convert_inappropriate_string_to_timestamp() {
        // This uses TextFormat printing, for the output won't be parsable.
        final String time = getCurrentTime().toString();
        stringifier.fromString(time);
    }

    @Test
    public void convert_back_and_forth() {
        final Timestamp timestamp = getCurrentTime();

        final String str = stringifier.convert(timestamp);
        final Timestamp convertedBack = stringifier.reverse()
                                                   .convert(str);

        assertEquals(timestamp, convertedBack);
    }
}
