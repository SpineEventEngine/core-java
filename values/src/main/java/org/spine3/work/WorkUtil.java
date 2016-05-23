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

package org.spine3.work;

/**
 * Common utility methods for {@link Work}.
 */
@SuppressWarnings("UtilityClass")
public class WorkUtil {

    private static final int MINUTES_PER_HOUR = 60;

    private WorkUtil() {
    }

    /**
     * Creates a new {@link Work} instance.
     *
     * @param minutes the amount of minutes in {@link Work} entry
     * @return the {@link Work} instance
     */
    public static Work ofMinutes(int minutes) {
        return ofHoursAndMinutes(0, minutes);
    }

    /**
     * Creates a new {@link Work} instance.
     *
     * @param hours the amount of hours in {@link Work} entry
     * @return the {@link Work} instance
     */
    public static Work ofHours(int hours) {
        return ofHoursAndMinutes(hours, 0);
    }

    /**
     * Creates a new {@link Work} instance.
     *
     * @param hours   the amount of hours in {@link Work} entry
     * @param minutes the amount of minutes in the last hour of {@link Work} entry
     * @return the {@link Work} instance
     */
    public static Work ofHoursAndMinutes(int hours, int minutes) {
        validateWorkAmount(minutes, "The amount of minutes in work can not be negative.");
        validateWorkAmount(hours, "The amount of hours in work can not be negative.");

        final int totalMinutes = hours * MINUTES_PER_HOUR + minutes;
        final Work result = Work.newBuilder()
                                .setMinutes(totalMinutes)
                                .build();
        return result;
    }

    /**
     * Makes sure, that work time amount is not negative.
     *
     * @param timeUnits    amount of work in hours or minutes
     * @param errorMessage the error message in case of invalid argument value
     */
    private static void validateWorkAmount(int timeUnits, String errorMessage) {
        if (timeUnits < 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
