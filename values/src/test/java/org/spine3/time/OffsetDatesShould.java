/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings("InstanceMethodNamingConvention")
public class OffsetDatesShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(OffsetDates.class));
    }

    @Test
    public void obtain_current_OffsetDate_using_ZoneOffset() {
        final ZoneOffset inKiev = ZoneOffsets.ofHours(3);
        final OffsetDate today = OffsetDates.now(inKiev);
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 3);

        assertEquals(calendar.get(Calendar.YEAR), today.getDate().getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, today.getDate().getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), today.getDate().getDay());
    }

    @Test
    public void obtain_current_OffsetDate_using_LocalDate_and_ZoneOffset() {
        final ZoneOffset inDelhi = ZoneOffsets.ofHoursMinutes(5, 30);
        final LocalDate tomorrow = LocalDates.plusDays(1);

        final OffsetDate tomorrowInDelhi = OffsetDates.of(tomorrow, inDelhi);
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.HOUR, 5);
        calendar.add(Calendar.MINUTE, 30);

        assertEquals(calendar.get(Calendar.YEAR), tomorrowInDelhi.getDate().getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, tomorrowInDelhi.getDate().getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), tomorrowInDelhi.getDate().getDay());
    }

}
