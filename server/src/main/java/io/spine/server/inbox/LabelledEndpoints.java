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

package io.spine.server.inbox;

import io.spine.server.delivery.MessageEndpoint;
import io.spine.server.type.ActorMessageEnvelope;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * The {@linkplain io.spine.server.inbox.LazyEndpoint endpoints} configured as destinations for
 * a certain {@linkplain io.spine.server.inbox.InboxLabel label}.
 */
class LabelledEndpoints<I, M extends ActorMessageEnvelope<?, ?, ?>> {

    private final Map<InboxLabel, LazyEndpoint<I, M>> endpoints =
            new EnumMap<>(InboxLabel.class);

    void add(InboxLabel label, LazyEndpoint<I, M> lazyEndpoint) {
        endpoints.put(label, lazyEndpoint);
    }

    Optional<MessageEndpoint<I, M>> get(InboxLabel label, M envelope) {
        if (!endpoints.containsKey(label)) {
            return Optional.empty();
        }
        return Optional.of(endpoints.get(label)
                                    .apply(envelope));
    }
}
