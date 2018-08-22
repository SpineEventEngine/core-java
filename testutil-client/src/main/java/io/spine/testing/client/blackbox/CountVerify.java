/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.testing.client.blackbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that a command or an event was handled responding with specified number of
 * {@link io.spine.core.Ack acks}.
 *
 * @author Mykhailo Drachuk
 */
final class CountVerify extends VerifyAcknowledgements {

    private final Count expectedCount;

    /** @param expectedCount an amount acks that are expected be observed */
    CountVerify(Count expectedCount) {
        super();
        this.expectedCount = expectedCount;
    }

    @Override
    public void verify(Acknowledgements acks) {
        int actualCount = acks.count();
        int expectedCount = this.expectedCount.value();
        String moreOrLess = compare(actualCount, expectedCount);
        assertEquals(
                expectedCount, actualCount,
                "Bounded Context acknowledged " + moreOrLess + " commands than expected"
        );
    }

    /**
     * Compares two integers returning a string stating if the first value is less, more or
     * same number as the second.
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static String compare(int firstValue, int secondValue) {
        if (firstValue > secondValue) {
            return "more";
        }
        if (firstValue < secondValue) {
            return "less";
        }
        return "same number";
    }
}
