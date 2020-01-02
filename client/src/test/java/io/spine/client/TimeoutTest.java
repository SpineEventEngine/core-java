/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.client;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.truth.StringSubject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("`Timeout` should")
class TimeoutTest {

    @Test
    @DisplayName("reject nulls")
    void nullCheck() {
        new NullPointerTester().testAllPublicStaticMethods(Timeout.class);
    }

    @Test
    @DisplayName("obtain value and unit")
    void valueAndUnit() {
        int value = 356;
        TimeUnit unit = TimeUnit.DAYS;
        
        Timeout timeout = Timeout.of(value, unit);
        assertThat(timeout.value())
                .isEqualTo(value);
        assertThat(timeout.unit())
                .isEqualTo(unit);
    }

    @Test
    @DisplayName("have `hashCode()` and `equals()`")
    void hashCodeAndEquals() {
        new EqualsTester()
                .addEqualityGroup(Timeout.of(5, TimeUnit.MINUTES))
                .addEqualityGroup(Timeout.of(100, TimeUnit.MILLISECONDS),
                                  Timeout.of(100, TimeUnit.MILLISECONDS))
                .addEqualityGroup(Timeout.of(0, TimeUnit.NANOSECONDS))
                .testEquals();
    }

    @Test
    @DisplayName("have string diagnostics output")
    void diags() {
        int timout = 100;
        TimeUnit unit = TimeUnit.HOURS;
        String diags = Timeout.of(timout, unit)
                              .toString();

        StringSubject assertOutput = assertThat(diags);
        assertOutput.contains(String.valueOf(timout));
        assertOutput.contains(String.valueOf(unit));
    }
}
