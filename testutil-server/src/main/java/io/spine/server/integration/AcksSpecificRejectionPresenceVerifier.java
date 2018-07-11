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

import com.google.protobuf.Message;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies that a command or an event was handled responding with rejection matching the
 * provided predicate.
 *
 * @param <T> a domain rejection type
 * @author Mykhailo Drachuk
 */
class AcksSpecificRejectionPresenceVerifier<T extends Message> extends AcknowledgementsVerifier {

    private final Class<T> type;
    private final RejectionPredicate<T> predicate;

    /**
     * @param type      a type of a domain rejection specified by a message class
     * @param predicate a predicate filtering the domain rejections
     */
    AcksSpecificRejectionPresenceVerifier(Class<T> type,
                                          RejectionPredicate<T> predicate) {
        this.type = type;
        this.predicate = predicate;
    }

    @Override
    public void verify(Acknowledgements acks) {
        if (!acks.containRejection(type, predicate)) {
            fail("Bounded Context did not reject a message:"
                         + predicate.message());
        }
    }
}
