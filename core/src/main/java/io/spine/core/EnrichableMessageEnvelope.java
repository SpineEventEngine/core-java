/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

public abstract class EnrichableMessageEnvelope<I extends Message, T, C extends Message>
        extends AbstractMessageEnvelope<I, T, C>
        implements ActorMessageEnvelope<I, T, C> {

    EnrichableMessageEnvelope(T object) {
        super(object);
    }

    public final boolean isEnrichmentEnabled() {
        final boolean result = getEnrichment().getModeCase() != Enrichment.ModeCase.DO_NOT_ENRICH;
        return result;
    }

    abstract Enrichment getEnrichment();

    private static Enrichment createEnrichment(Map<String, Any> enrichments) {
        final Enrichment.Builder enrichment =
                Enrichment.newBuilder()
                          .setContainer(Enrichment.Container.newBuilder()
                                                            .putAllItems(enrichments));
        return enrichment.build();
    }

    public EnrichableMessageEnvelope<I, T, C> toEnriched(Map<String, Any> enrichments) {

        final Enrichment enrichment = createEnrichment(enrichments);
        final EnrichableMessageEnvelope<I, T, C> result = enrich(enrichment);
        return result;

    }

    protected abstract EnrichableMessageEnvelope<I, T, C> enrich(Enrichment enrichment);
}
