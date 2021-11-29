/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.delivery;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;
import io.spine.base.Time;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.util.Timestamps.subtract;
import static com.google.protobuf.util.Timestamps.toMillis;

@DisplayName("`Turbulence` should")
class TurbulenceTest {

    @Test
    @DisplayName("not accept `null` values of `Duration` at creation")
    void notAcceptNulls() {
        new NullPointerTester().setDefault(Duration.class, Duration.getDefaultInstance())
                               .testStaticMethods(Turbulence.class, PACKAGE);
    }

    @Test
    @DisplayName("define the turbulence start time " +
            "by counting back its duration from the current time")
    void defineStartTime() {
        var duration = Durations.fromSeconds(10);
        var turbulence = Turbulence.of(duration);
        var actual = turbulence.whenStarts();

        var currentTime = Time.currentTime();
        var approximateExpected = subtract(currentTime, duration);

        var difference = Math.abs(toMillis(actual) - toMillis(approximateExpected));
        assertThat(difference).isLessThan(50 /* ms */);
    }
}
