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

package org.spine3.time;

import org.junit.Before;

import static java.lang.Math.abs;
import static org.spine3.test.Tests.random;
import static org.spine3.time.ZoneOffsets.MAX_HOURS_OFFSET;
import static org.spine3.time.ZoneOffsets.MAX_MINUTES_OFFSET;
import static org.spine3.time.ZoneOffsets.MIN_HOURS_OFFSET;
import static org.spine3.time.ZoneOffsets.MIN_MINUTES_OFFSET;

/**
 * Abstract base for test of time with offset.
 *
 * @author Alexander Yevsyukov
 */
public class AbstractZonedTimeTest {

    @SuppressWarnings("ProtectedField") // OK for brevity of test code.
    protected ZoneOffset zoneOffset;

    private static ZoneOffset generateOffset() {
        // Reduce the hour range by one assuming minutes are also generated.
        final int hours = random(MIN_HOURS_OFFSET + 1, MAX_HOURS_OFFSET - 1);
        int minutes = random(MIN_MINUTES_OFFSET, MAX_MINUTES_OFFSET);
        // Make minutes of the same sign with hours.
        minutes = hours >= 0 ? abs(minutes) : -abs(minutes);
        return ZoneOffsets.ofHoursMinutes(hours, minutes);
    }

    @Before
    public void setUp() {
        zoneOffset = generateOffset();
    }
}
