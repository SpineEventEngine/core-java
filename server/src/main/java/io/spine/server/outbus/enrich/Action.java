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

package io.spine.server.outbus.enrich;

import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.Enrichment;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.protobuf.AnyPacker;
import io.spine.type.TypeName;

import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.filter;

/**
 * Performs enrichment operation of an event.
 *
 * @author Alexander Yevsyukov
 */
final class Action {

    private final EventEnvelope envelope;
    private final Collection<EnrichmentFunction<?, ?>> availableFunctions;

    private final Map<String, Any> enrichments = Maps.newHashMap();

    Action(Enricher parent, EventEnvelope envelope) {
        this.envelope = envelope;
        final Class<? extends Message> eventClass = envelope.getMessageClass()
                                                            .value();
        final Collection<EnrichmentFunction<?, ?>> functionsPerClass =
                parent.getFunctions(eventClass);
        this.availableFunctions = filter(functionsPerClass, EnrichmentFunction.activeOnly());
    }

    Event perform() {
        createEnrichments();
        final EventContext enrichedContext = enrichContext();
        final Event result = envelope.getOuterObject()
                                     .toBuilder()
                                     .setContext(enrichedContext)
                                     .build();
        return result;
    }

    private void createEnrichments() {
        final EventContext eventContext = envelope.getEventContext();
        final Message eventMessage = envelope.getMessage();
        for (EnrichmentFunction function : availableFunctions) {
            final Message enriched = apply(function, eventMessage, eventContext);
            checkResult(enriched, function);
            final String typeName = TypeName.of(enriched)
                                            .value();
            enrichments.put(typeName, AnyPacker.pack(enriched));
        }
    }

    /**
     * Applies the passed function to the message.
     *
     * <p>We suppress the {@code "unchecked"} because we ensure types when we...
     * <ol>
     *      <li>create enrichments,
     *      <li>put them into {@linkplain Enricher#functions} by their event message class.
     * </ol>
     */
    @SuppressWarnings("unchecked")
    private static Message apply(EnrichmentFunction function, Message input, EventContext context) {
        final Message result = (Message) function.apply(input, context);
        return result;
    }

    private void checkResult(Message enriched, EnrichmentFunction function) {
        checkNotNull(
            enriched,
            "EnrichmentFunction %s produced `null` from event message %s",
            function, envelope.getMessage()
        );
    }

    private EventContext enrichContext() {
        final Enrichment.Builder enrichment =
                Enrichment.newBuilder()
                          .setContainer(Enrichment.Container.newBuilder()
                                                            .putAllItems(enrichments));
        return envelope.getEventContext()
                       .toBuilder()
                       .setEnrichment(enrichment)
                       .build();
    }
}
