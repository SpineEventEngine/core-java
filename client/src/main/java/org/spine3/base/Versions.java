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

package org.spine3.base;

import com.google.protobuf.Timestamp;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;

/**
 * Utilities for working with {@link Version}.
 *
 * @author Alexander Yevsyukov
 */
public class Versions {

    private Versions() {
        // Prevent instantiation of this utility class.
    }

    private static Version create(int number, Timestamp timestamp) {
        return Version.newBuilder()
                      .setNumber(number)
                      .setTimestamp(timestamp)
                      .build();
    }

    /**
     * Creates a new instance with the zero number and current system time.
     */
    public static Version create() {
        return create(0, getCurrentTime());
    }

    /**
     * Creates a new instance with the passed number and the timestamp.
     */
    public static Version newVersion(int number, Timestamp timestamp) {
        checkNotNull(timestamp);
        return create(number, timestamp);
    }

    /**
     * Creates a new {@code Version} with the number increased by one
     * and the timestamp of the current system time.
     */
    public static Version increment(Version version) {
        final Version result = create(version.getNumber() + 1, getCurrentTime());
        return result;
    }

    /**
     * Ensures that an entity transits into a new version with a greater number.
     *
     * @param currentVersion the current version of an entity
     * @param newVersion the candidate for the new version of the entity
     * @throws IllegalArgumentException if {@code newVersion} has less or equal
     *                                  number with the {@code currentVersion}
     */
    public static void checkIsIncrement(Version currentVersion, Version newVersion) {
        final int currentNumber = currentVersion.getNumber();
        final int newNumber = newVersion.getNumber();
        if (newNumber <= currentNumber) {
            final String errMsg = format(
                    "New version number (%d) cannot be less or equal to the current (%d).",
                    newNumber, currentNumber);
            throw new IllegalArgumentException(errMsg);
        }
    }
}
