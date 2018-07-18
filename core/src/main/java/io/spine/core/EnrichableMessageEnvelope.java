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

package io.spine.core;

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Enrichments.createEnrichment;

/**
 * Common base for envelopes of messages that can be enriched.
 *
 * @param <I> the type of message IDs
 * @param <T> the type of outer objects
 * @param <C> the type of context of the messages
 * @author Alexander Yevsyukov
 */
public abstract class EnrichableMessageEnvelope<I extends Message, T, C extends Message>
        extends AbstractMessageEnvelope<I, T, C>
        implements ActorMessageEnvelope<I, T, C> {

    EnrichableMessageEnvelope(T object) {
        super(object);
    }

    /**
     * Verifies if the enrichment of the message is enabled.
     *
     * @see Enrichment.Builder#setDoNotEnrich(boolean)
     */
    public final boolean isEnrichmentEnabled() {
        boolean result = getEnrichment().getModeCase() != Enrichment.ModeCase.DO_NOT_ENRICH;
        return result;
    }

    /**
     * Obtains the enrichment of the message.
     */
    protected abstract Enrichment getEnrichment();

    /**
     * Factory method for creating enriched version of the message.
     */
    protected abstract EnrichableMessageEnvelope<I, T, C> enrich(Enrichment enrichment);

    /**
     * Creates a new version of the message with the enrichments applied.
     *
     * @param enrichments the enrichments to apply
     * @return new enriched envelope
     */
    public EnrichableMessageEnvelope<I, T, C> toEnriched(Map<String, Any> enrichments) {
        checkNotNull(enrichments);
        Enrichment enrichment = createEnrichment(enrichments);
        EnrichableMessageEnvelope<I, T, C> result = enrich(enrichment);
        return result;
    }
}
