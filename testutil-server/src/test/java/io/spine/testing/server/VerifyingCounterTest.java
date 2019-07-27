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

package io.spine.testing.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("VerifyingCounter should")
class VerifyingCounterTest {

    private VerifyingCounter counter;

    @BeforeEach
    void setUpCounter() {
        counter = new VerifyingCounter();
    }

    @Test
    @DisplayName("be initialized with `0` by default")
    void beInitializedWithZero() {
        assertThat(counter.value()).isEqualTo(0);
    }

    @Test
    @DisplayName("be initialized with some predefined number")
    void beInitializedWithNumber() {
        int value = 15;
        VerifyingCounter counter = new VerifyingCounter(value);
        assertThat(counter.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("increment the stored value by `1`")
    void incrementByOne() {
        counter.increment();
        assertThat(counter.value()).isEqualTo(1);
    }

    @Test
    @DisplayName("increment the stored value by some number")
    void incrementByNumber() {
        int number = 15;
        counter.increment(number);
        assertThat(counter.value()).isEqualTo(number);
    }

    @Test
    @DisplayName("decrement the stored value by `1`")
    void decrementByOne() {
        counter.decrement();
        assertThat(counter.value()).isEqualTo(-1);
    }

    @Test
    @DisplayName("decrement the stored value by some number")
    void decrementByNumber() {
        int number = 15;
        counter.decrement(number);
        assertThat(counter.value()).isEqualTo(-number);
    }

    @Test
    @DisplayName("verify against the given expected value")
    void verifyAgainstExpected() {
        counter.verifyEquals(0);

        assertThrows(AssertionError.class, () -> counter.verifyEquals(15));
    }
}
