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

package io.spine.string;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.test.Tests;
import io.spine.time.LocalDate;
import io.spine.time.LocalTime;
import io.spine.time.OffsetDateTime;
import io.spine.time.OffsetTime;
import io.spine.time.ZoneOffset;
import io.spine.time.string.TimeStringifiers;
import org.junit.Test;

import static io.spine.time.string.TimeStringifiers.forDuration;
import static io.spine.time.string.TimeStringifiers.forLocalDate;
import static io.spine.time.string.TimeStringifiers.forLocalTime;
import static io.spine.time.string.TimeStringifiers.forOffsetDateTime;
import static io.spine.time.string.TimeStringifiers.forOffsetTime;
import static io.spine.time.string.TimeStringifiers.forTimestamp;
import static io.spine.time.string.TimeStringifiers.forZoneOffset;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
public class TimeStringifiersShould {

    @Test
    public void have_utility_ctor() {
        Tests.assertHasPrivateParameterlessCtor(TimeStringifiers.class);
    }

    private static Stringifier<Object> getStringifier(Class<?> cls) {
        return StringifierRegistry.getInstance().get(cls).get();
    }

    @Test
    public void register_stringifiers() {
        assertEquals(forZoneOffset(), getStringifier(ZoneOffset.class));
        assertEquals(forDuration(), getStringifier(Duration.class));
        assertEquals(forTimestamp(), getStringifier(Timestamp.class));
        assertEquals(forLocalDate(), getStringifier(LocalDate.class));
        assertEquals(forLocalTime(), getStringifier(LocalTime.class));
        assertEquals(forOffsetDateTime(), getStringifier(OffsetDateTime.class));
        assertEquals(forOffsetTime(), getStringifier(OffsetTime.class));
    }
}
