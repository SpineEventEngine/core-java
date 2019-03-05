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

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies that a command or an event was handled responding with an error matching a provided
 * {@link ErrorCriterion error criterion}.
 */
final class SpecificErrorPresenceVerify extends VerifyAcknowledgements {

    private final ErrorCriterion criterion;

    /**
     * Creates a new instance.
     *
     * @param criterion
     *         an error criterion specifying which kind of error should be a part
     *         of acknowledgement
     */
    SpecificErrorPresenceVerify(ErrorCriterion criterion) {
        super();
        this.criterion = criterion;
    }

    @Override
    public void verify(Acknowledgements acks) {
        if (!acks.containErrors(criterion)) {
            fail("Bounded Context did not contain an expected error. "
                         + criterion.description());
        }
    }
}
