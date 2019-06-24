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

package io.spine.server.delivery;

import io.spine.server.type.ActorMessageEnvelope;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * The {@linkplain LazyEndpoint endpoints} configured as destinations for
 * a certain {@linkplain io.spine.server.delivery.InboxLabel label}.
 */
class Endpoints<I, M extends ActorMessageEnvelope<?, ?, ?>> {

    private final Map<InboxLabel, LazyEndpoint<I, M>> endpoints =
            new EnumMap<>(InboxLabel.class);

    /**
     * Adds a lazy-initializable endpoint with a respective label.
     */
    void add(InboxLabel label, LazyEndpoint<I, M> lazyEndpoint) {
        endpoints.put(label, lazyEndpoint);
    }

    /**
     * Obtains the message endpoint for the given label and the envelope.
     *
     * Returns {@code Optional.empty()} if there is no such label configured.
     */
    Optional<MessageEndpoint<I, M>> get(InboxLabel label, M envelope) {
        if (!endpoints.containsKey(label)) {
            return Optional.empty();
        }
        return Optional.of(endpoints.get(label)
                                    .apply(envelope));
    }

    /**
     * Tells if there are any endpoints configured.
     */
    boolean isEmpty() {
        return endpoints.isEmpty();
    }
}
