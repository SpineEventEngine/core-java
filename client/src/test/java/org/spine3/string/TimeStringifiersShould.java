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

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.string.time.TimeStringifiers;
import org.spine3.test.Tests;
import org.spine3.time.LocalDate;
import org.spine3.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.spine3.string.time.TimeStringifiers.forDuration;
import static org.spine3.string.time.TimeStringifiers.forLocalDate;
import static org.spine3.string.time.TimeStringifiers.forTimestamp;
import static org.spine3.string.time.TimeStringifiers.forZoneOffset;

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
    }
}
