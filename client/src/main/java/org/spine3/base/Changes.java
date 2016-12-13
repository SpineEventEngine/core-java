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

package org.spine3.base;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with field changes.
 *
 * @author Alexander Yevsyukov
 */
public class Changes {

    private static final String PREVIOUS_VALUE = "previousValue";
    private static final String NEW_VALUE = "newValue";
    private static final String ERR_CANNOT_BE_EQUAL = "newValue cannot be equal to previousValue";

    private Changes() {}

    /**
     * Creates {@link StringChange} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static StringChange of(String previousValue, String newValue) {
        checkNotNull(previousValue, PREVIOUS_VALUE);
        checkNotNull(newValue, NEW_VALUE);
        checkArgument(!newValue.isEmpty(), "newValue cannot be empty");
        checkArgument(!newValue.equals(previousValue), ERR_CANNOT_BE_EQUAL);

        final StringChange result = StringChange.newBuilder()
                                                .setPreviousValue(previousValue)
                                                .setNewValue(newValue)
                                                .build();
        return result;
    }

    /**
     * Creates {@link StringChange} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static DoubleChange of(double previousValue, double newValue) {
        checkNotNull(previousValue, PREVIOUS_VALUE);
        checkNotNull(newValue, NEW_VALUE);
        checkArgument(Double.compare(newValue, previousValue) != 0, ERR_CANNOT_BE_EQUAL);

        final DoubleChange result = DoubleChange.newBuilder()
                                                .setPreviousValue(previousValue)
                                                .setNewValue(newValue)
                                                .build();
        return result;
    }
}
