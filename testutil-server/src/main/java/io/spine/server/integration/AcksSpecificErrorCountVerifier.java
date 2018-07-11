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

package io.spine.server.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that a command or an event was handled responding with an error matching a provided
 * {@link ErrorCriteria error criteria}.
 *
 * @author Mykhailo Drachuk
 */
class AcksSpecificErrorCountVerifier extends AcknowledgementsVerifier {

    private final int expectedCount;
    private final ErrorCriteria criteria;

    /**
     * @param expectedCount an amount of errors that are expected to match the criteria
     * @param criteria      an error criteria specifying which kind of error should be a part
     *                      of acknowledgement
     */
    AcksSpecificErrorCountVerifier(int expectedCount, ErrorCriteria criteria) {
        this.expectedCount = expectedCount;
        this.criteria = criteria;
    }

    @Override
    public void verify(Acknowledgements acks) {
        assertEquals(expectedCount, acks.countErrors(criteria),
                     "Bounded Context did not contain an expected count of errors. "
                             + criteria.description());
    }
}
