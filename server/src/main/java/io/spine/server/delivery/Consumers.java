/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import io.spine.server.commandbus.DelegatingCommandDispatcher;
import io.spine.server.event.DelegatingEventDispatcher;
import io.spine.server.rejection.DelegatingRejectionDispatcher;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A utility class for working with delivery consumers.
 *
 * @author Alex Tymchenko
 */
public final class Consumers {

    /** Prevents instantiation of this utility class. */
    private Consumers() {}

    /**
     * Creates an identity for the {@linkplain Delivery} consumer.
     *
     * @param consumer the consumer
     * @return the consumer identity
     */
    public static ConsumerId idOf(Object consumer) {
        checkNotNull(consumer);

        // Delegates are masking the real class.
        // Using their string identity instead of FQN.
        if (consumer instanceof DelegatingCommandDispatcher ||
                consumer instanceof DelegatingEventDispatcher ||
                consumer instanceof DelegatingRejectionDispatcher) {

            return idOfString(consumer.toString());
        }
        return idOfString(consumer.getClass()
                                  .getName());
    }

    private static ConsumerId idOfString(String identityString) {
        return ConsumerId.newBuilder()
                         .setConsumerIdentity(identityString)
                         .build();
    }
}
