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

import com.google.protobuf.Message;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies that a command or an event was handled responding with a {@link Rejection rejection}
 * of the provided type.
 *
 * @author Mykhailo Drachuk
 */
class AcksRejectionOfTypePresenceVerifier extends AcknowledgementsVerifier {

    private final RejectionClass type;

    /** @param type rejection type in a form of {@link RejectionClass RejectionClass} */
    AcksRejectionOfTypePresenceVerifier(RejectionClass type) {
        super();
        this.type = type;
    }

    @Override
    public void verify(Acknowledgements acks) {
        if (!acks.containRejections(type)) {
            Class<? extends Message> domainRejection = type.value();
            fail("Bounded Context did not reject a message of type:" +
                         domainRejection.getSimpleName());
        }
    }
}
