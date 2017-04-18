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
import org.spine3.test.Tests;
import org.spine3.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.spine3.string.TimeStringifiers.forTimestampWebSafe;
import static org.spine3.time.Timestamps2.getCurrentTime;

/**
 * @author Alexander Yevsyukov
 */
public class TimeStringifiersShould {

    @Test
    public void have_utility_ctor() {
        Tests.assertHasPrivateParameterlessCtor(TimeStringifiers.class);
    }

    @Test
    public void register_all_stringifiers() {

    }

    @Test
    public void register_stringifier() {
        assertFalse(StringifierRegistry.getInstance()
                                       .get(LocalDate.class)
                                       .isPresent());

        TimeStringifiers.registerAll();

        assertEquals(TimeStringifiers.forLocalDate(), StringifierRegistry.getInstance()
                                                                         .get(LocalDate.class)
                                                                         .get());
    }

    @Test
    public void provide_webSafe_timestamp_stringifier() {
        final Timestamp timestamp = getCurrentTime();
        final Stringifier<Timestamp> stringifier = forTimestampWebSafe();

        final String str = stringifier.convert(timestamp);
        assertEquals(timestamp, stringifier.reverse()
                                           .convert(str));
    }

    @Test
    public void provide_stringifier_for_Timestamp() {
        final Timestamp timestamp = getCurrentTime();

        final String str = Stringifiers.toString(timestamp);
        final Timestamp convertedBack = Stringifiers.fromString(str, Timestamp.class);

        assertEquals(timestamp, convertedBack);
    }

}
