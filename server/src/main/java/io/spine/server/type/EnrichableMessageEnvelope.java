/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.type;

import com.google.protobuf.Message;
import io.spine.core.EnrichableMessageContext;
import io.spine.server.enrich.EnrichmentService;

/**
 * A common interface for enrichable message envelopes.
 *
 * @param <I>
 *         the type of the envelope identifiers
 * @param <T>
 *         the type of the objects wrapping the original message
 * @param <M>
 *         the type of the messages
 * @param <C>
 *         the type of the message contexts
 * @param <E>
 *         the type of the enrichable envelopes
 */
public interface EnrichableMessageEnvelope<I extends Message,
                                           T,
                                           M extends Message,
                                           C extends EnrichableMessageContext,
                                           E extends EnrichableMessageEnvelope<I, T, M, C, E>>
    extends MessageEnvelope<I, T, C> {

    /**
     * Creates a copy with the envelope adding enrichments to the context of the message.
     */
    E toEnriched(EnrichmentService<M, C> service);
}
