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
import io.spine.time.Durations2;
import io.spine.time.string.TimeStringifiers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
public class DurationStringifierShould extends AbstractStringifierTest<Duration> {

    public DurationStringifierShould() {
        super(TimeStringifiers.forDuration());
    }

    @Override
    protected Duration createObject() {
        return Durations2.hoursAndMinutes(5, 37);
    }

    @Test
    public void convert_negative_duration() {
        final Stringifier<Duration> stringifier = getStringifier();
        final Duration negative = Durations2.hoursAndMinutes(-4, -31);
        assertEquals(negative, stringifier.reverse()
                                          .convert(stringifier.convert(negative)));
    }
}
