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

import java.util.Calendar;

/**
 * Routines for working with {@link OffsetDate}.
 *
 * @author Alexander Aleksandrov
 */
public class OffsetDates {

    private OffsetDates() {
    }

    /**
     * Obtains current OffsetDate instance using {@link ZoneOffset}.
     */
    public static OffsetDate now(ZoneOffset zoneOffset) {
        final int minutes = zoneOffset.getAmountSeconds() / 60;
        final LocalDate localDate = LocalDates.plusMinutes(minutes);

        final OffsetDate result = OffsetDate.newBuilder()
                                            .setDate(localDate)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

    /**
     * Obtains OffsetDate instance using {@link LocalDate} and {@link ZoneOffset}.
     */
    public static OffsetDate of(LocalDate localDate, ZoneOffset zoneOffset) {

        final OffsetDate result = OffsetDate.newBuilder()
                                            .setDate(localDate)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }
}
