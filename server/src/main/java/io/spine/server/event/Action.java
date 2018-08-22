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

package io.spine.server.event;

import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.protobuf.AnyPacker;
import io.spine.type.TypeName;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Performs enrichment operation of an enrichable message.
 *
 * @author Alexander Yevsyukov
 */
final class Action {

    private final EventEnvelope envelope;
    private final Collection<EnrichmentFunction<?, ?, ?>> availableFunctions;

    private final Map<String, Any> enrichments = Maps.newHashMap();

    Action(Enricher parent, EventEnvelope envelope) {
        this.envelope = envelope;
        Class<? extends Message> sourceClass = envelope.getMessageClass()
                                                       .value();
        Collection<EnrichmentFunction<?, ?, ?>> functionsPerClass =
                parent.getFunctions(sourceClass);
        this.availableFunctions = functionsPerClass.stream()
                                                   .filter(EnrichmentFunction.activeOnly())
                                                   .collect(Collectors.toList());
    }

    EventEnvelope perform() {
        createEnrichments();
        EventEnvelope enriched = envelope.toEnriched(enrichments);
        return enriched;
    }

    private void createEnrichments() {
        Message sourceMessage = envelope.getMessage();
        for (EnrichmentFunction function : availableFunctions) {
            Message enriched = apply(function, sourceMessage, envelope.getMessageContext());
            checkResult(enriched, function);
            String typeName = TypeName.of(enriched)
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
     *      <li>put them into {@linkplain Enricher#functions} by their message class.
     * </ol>
     */
    @SuppressWarnings("unchecked")
    private static Message apply(EnrichmentFunction function,
                                 Message input,
                                 EventContext context) {
        Message result = (Message) function.apply(input, context);
        return result;
    }

    private void checkResult(Message enriched, EnrichmentFunction function) {
        checkNotNull(
            enriched,
            "EnrichmentFunction %s produced `null` for the source message %s",
            function, envelope.getMessage()
        );
    }
}
