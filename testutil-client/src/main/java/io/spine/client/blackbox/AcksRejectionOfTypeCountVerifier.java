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

package io.spine.client.blackbox;

import com.google.protobuf.Message;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that a command or an event was handled responding with a {@link Rejection rejection}
 * of a provided type specified amount of times.
 *
 * @author Mykhailo Drachuk
 */
class AcksRejectionOfTypeCountVerifier extends AcknowledgementsVerifier {

    private final RejectionClass type;
    private final Count expectedCount;

    /**
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @param type          rejection type in a form of {@link RejectionClass RejectionClass}
     */
    AcksRejectionOfTypeCountVerifier(RejectionClass type, Count expectedCount) {
        super();
        this.type = type;
        this.expectedCount = expectedCount;
    }

    @Override
    public void verify(Acknowledgements acks) {
        Class<? extends Message> rejectionClass = type.value();
        assertEquals(expectedCount.value(), acks.countRejections(type),
                     "Bounded Context did not contain " + rejectionClass.getSimpleName() +
                             "rejection expected amount of times.");
    }
}
