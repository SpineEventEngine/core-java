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
 * Routines for working with {@link LocalDate}.
 */
public class LocalDates {

    private LocalDates() {
    }

    /**
     * Obtains current LocalDate instance.
     */
    public static LocalDate now() {
        final Calendar calendar = Calendar.getInstance();
        final int year = calendar.get(Calendar.YEAR);
        // The Calendar class assumes JANUARY is zero. Therefore add 1 to get the value of MonthOfYear.
        final MonthOfYear month = MonthOfYear.forNumber(calendar.get(Calendar.MONTH) + 1);
        final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        final LocalDate result = LocalDate.newBuilder()
                                          .setYear(year)
                                          .setMonth(month)
                                          .setDay(dayOfMonth)
                                          .build();
        return result;
    }

    /**
     * Obtains current LocalDate instance with the specified number of days added.
     */
    public static LocalDate plusDays(int daysToAdd) {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, daysToAdd);
        final int year = calendar.get(Calendar.YEAR);
        // The Calendar class assumes JANUARY is zero. Therefore add 1 to get the value of MonthOfYear.
        final MonthOfYear month = MonthOfYear.forNumber(calendar.get(Calendar.MONTH) + 1);
        final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        final LocalDate result = LocalDate.newBuilder()
                                          .setYear(year)
                                          .setMonth(month)
                                          .setDay(dayOfMonth)
                                          .build();
        return result;
    }
}
