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
package org.spine3.time.change;

import com.google.protobuf.Timestamp;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with field changes.
 *
 * @author Alexander Aleksandrov
 */
@SuppressWarnings("OverlyCoupledClass") /* ... because we want one utility class for all the TimeChanges classes. */
public class Changes {

    private static final String PREVIOUS_VALUE = "previousValue";
    private static final String NEW_VALUE = "newValue";
    private static final String ERR_CANNOT_BE_EQUAL = "newValue cannot be equal to previousValue";
    private static final String ERR_NEW_VALUE_CANNOT_BE_EMPTY = "newValue cannot be empty";

    private Changes() {
    }

    /**
     * Creates {@link IntervalChange} object for the passed previous and new field values of time interval.
     *
     * <p>Passed values cannot be equal.
     */
    public static IntervalChange ofInterval(Timestamp previousStartValue, Timestamp newStartValue,
                                    Timestamp previousEndValue, Timestamp newEndValue) {
        checkNotNull(previousStartValue, PREVIOUS_VALUE);
        checkNotNull(newStartValue, NEW_VALUE);
        checkNotNull(previousEndValue, PREVIOUS_VALUE);
        checkNotNull(newEndValue, NEW_VALUE);
        checkArgument(!newStartValue.equals(previousStartValue) && !newEndValue.equals(previousEndValue), ERR_CANNOT_BE_EQUAL);

        final IntervalChange result = IntervalChange.newBuilder()
                                                    .setPreviouseStartValue(previousStartValue)
                                                    .setPreviouseEndValue(previousEndValue)
                                                    .setNewStartValue(newStartValue)
                                                    .setNewEndValue(newEndValue)
                                                    .build();
        return result;
    }
}
