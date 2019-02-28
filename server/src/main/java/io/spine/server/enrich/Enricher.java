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

package io.spine.server.enrich;

import com.google.protobuf.Message;
import io.spine.annotation.SPI;
import io.spine.core.EnrichableMessageContext;
import io.spine.core.Enrichment;
import io.spine.server.type.EnrichableMessageEnvelope;

import java.util.Optional;
import java.util.Set;

/**
 * Enriches messages <em>after</em> they are stored, and <em>before</em> they are dispatched.
 *
 * <p>Enrichment schema is constructed like this:
 * <pre>{@code
 *   Enricher enricher = Enricher
 *       .newBuilder()
 *       .add(MyEvent.class,
 *            new EventEnrichmentFn<MyEvent, EventContext, MyEnrichment> { ... } )
 *       ...
 *       .build();
 * }</pre>
 */
@SPI
public final class Enricher implements EnrichmentService {

    private final Schema schema;

    /**
     * Creates a new builder.
     */
    public static EnricherBuilder newBuilder() {
        return new EnricherBuilder();
    }

    /**
     * Creates a new instance taking functions from the passed builder.
     */
    Enricher(EnricherBuilder builder) {
        this.schema = new Schema(builder.functions());
    }

    Set<EnrichmentFn<?, ?, ?>> enrichmentOf(Class<? extends Message> cls) {
        return schema.enrichmentOf(cls);
    }

    /**
     * Enriches the passed message if it can be enriched. Otherwise, returns the passed instance.
     *
     * <p>An message can be enriched if the following conditions are met:
     *
     * <ol>
     *     <li>There is one or more enrichments defined in Protobuf using
     *     {@code enrichment_for} and/or {@code by} options.
     *     <li>There is one or more field enrichment schema registered for
     *     the class of the passed message.
     *     <li>The flag {@code do_not_enrich} is not set in the {@link io.spine.core.Enrichment
     *     Enrichment} instance of the context of the outer object of the message.
     * </ol>
     *
     * @param  source
     *         the envelope with the source message
     * @throws IllegalArgumentException
     *         if the passed message cannot be enriched
     */
    public <E extends EnrichableMessageEnvelope<?, ?, ?, E>> E enrich(E source) {
        E result = source.toEnriched(this);
        return  result;
    }

    @Override
    public
    Optional<Enrichment> createEnrichment(Message message, EnrichableMessageContext context) {
        Action action = new Action(this, message, context);
        Enrichment result = action.perform();
        return Optional.of(result);
    }
}
