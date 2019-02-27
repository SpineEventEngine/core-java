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

package io.spine.testing.client.blackbox;

import io.spine.type.RejectionType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that a command was handled with a {@link io.spine.base.ThrowableMessage rejection} of
 * a provided type specified amount of times.
 */
final class RejectionOfTypeCountVerify extends VerifyAcknowledgements {

    private final RejectionType type;
    private final Count expectedCount;

    RejectionOfTypeCountVerify(RejectionType type, Count expectedCount) {
        super();
        this.type = type;
        this.expectedCount = expectedCount;
    }

    @Override
    public void verify(Acknowledgements acks) {
        int expectedValue = expectedCount.value();
        int actual = acks.countRejections(type);
        assertEquals(
                expectedValue,
                actual,
                "The rejection of type `" + type.descriptor().getFullName() +
                "` was not created expected number of times (" + expectedValue + ")." +
                " The actual count is: " + actual + '.'
        );
    }
}
