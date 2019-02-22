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

import com.google.common.collect.ImmutableCollection;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.EnrichableMessageContext;
import io.spine.core.Enrichment;
import io.spine.protobuf.AnyPacker;
import io.spine.type.TypeName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;

/**
 * Performs enrichment operation for an event.
 */
final class Action {

    private final Message message;
    private final EnrichableMessageContext context;

    /**
     * Active functions applicable to the enriched event.
     */
    private final ImmutableCollection<EnrichmentFunction<?, ?, ?>> functions;

    /**
     * A map from the type name of an enrichment to its packed instance, in the form
     * it is used in the enriched event context.
     */
    private final Map<String, Any> enrichments = newHashMap();

    Action(Enricher parent, Message message, EnrichableMessageContext context) {
        this.message = message;
        this.context = context;
        Class<? extends Message> sourceClass = message.getClass();
        this.functions = parent.schema()
                               .get(sourceClass);
    }

    /**
     * Creates new envelope with the enriched version of the event.
     */
    Enrichment perform() {
        createEnrichments();
        Enrichment result = createEnrichment();
        return result;
    }

    private void createEnrichments() {
        for (EnrichmentFunction function : functions) {
            Message enrichment = apply(function, message, context);
            checkResult(enrichment, function);
            put(enrichment);
        }
    }

    private void put(Message enrichment) {
        String typeName = TypeName.of(enrichment)
                                  .value();
        enrichments.put(typeName, AnyPacker.pack(enrichment));
    }

    /**
     * Applies the passed function to the message.
     *
     * <p>We suppress the {@code "unchecked"} because we ensure types when we...
     * <ol>
     *      <li>create enrichments,
     *      <li>put them into {@link Enricher} by their message class.
     * </ol>
     */
    @SuppressWarnings("unchecked")
    private static Message apply(EnrichmentFunction fn,
                                 Message message,
                                 EnrichableMessageContext context) {
        Message result = (Message) fn.apply(message, context);
        return result;
    }

    private void checkResult(@Nullable Message enriched, EnrichmentFunction function) {
        checkNotNull(
            enriched,
            "EnrichmentFunction `%s` produced `null` for the source message `%s`.",
            function, message
        );
    }

    /**
     * Creates a new {@link Enrichment} instance from the passed map.
     */
    private Enrichment createEnrichment() {
        Enrichment.Builder enrichment =
                Enrichment.newBuilder()
                          .setContainer(Enrichment.Container.newBuilder()
                                                            .putAllItems(enrichments));
        return enrichment.build();
    }
}
