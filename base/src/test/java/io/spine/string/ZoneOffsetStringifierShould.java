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

import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;
import io.spine.time.string.TimeStringifiers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
public class ZoneOffsetStringifierShould extends AbstractStringifierTest<ZoneOffset> {

    public ZoneOffsetStringifierShould() {
        super(TimeStringifiers.forZoneOffset());
    }

    @Override
    protected ZoneOffset createObject() {
        return ZoneOffsets.ofHoursMinutes(7, 40);
    }

    @Test
    public void convert_negative_zone_offset() {
        final Stringifier<ZoneOffset> stringifier = getStringifier();
        final ZoneOffset negative = ZoneOffsets.ofHoursMinutes(-3, -45);

        assertEquals(negative, stringifier.reverse()
                                          .convert(stringifier.convert(negative)));
    }
}
