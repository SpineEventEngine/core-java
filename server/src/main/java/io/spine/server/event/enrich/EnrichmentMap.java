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

package io.spine.server.event.enrich;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import io.spine.base.EventMessage;
import io.spine.type.TypeName;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.spine.server.event.enrich.EnrichmentMapBuilder.loadFromResources;

/**
 * A map from an event enrichment Protobuf type name to the corresponding
 * type name(s) of event(s) to enrich.
 *
 * <p>Example:
 * <p>{@code proto.type.MyEventEnrichment} - {@code proto.type.FirstEvent},
 * {@code proto.type.SecondEvent}
 */
final class EnrichmentMap {

    /** A multi-map from enrichment class name to enriched message class names. */
    private final ImmutableMultimap<String, String> enrichmentsMap;

    /** Creates new instance loading the map from resources. */
    private EnrichmentMap() {
        this.enrichmentsMap = loadFromResources();
    }

    /** Loads the map from resources. */
    static EnrichmentMap load() {
        return new EnrichmentMap();
    }

    ImmutableSet<TypeName> enrichmentTypes() {
        return enrichmentsMap.keySet()
                             .stream()
                             .map(TypeName::of)
                             .collect(toImmutableSet());
    }

    ImmutableSet<Class<EventMessage>> sourceEventClasses(TypeName enrichmentType) {
        @SuppressWarnings("unchecked") /* The cast is safe since values of the map contain type
            names of enriched event messages. */
        ImmutableSet<Class<EventMessage>> result =
                sourceEventTypes(enrichmentType)
                        .stream()
                        .map(TypeName::getMessageClass)
                        .map(c -> (Class<EventMessage>) (Class<?>) c)
                        .collect(toImmutableSet());
        return result;
    }

    ImmutableSet<TypeName> sourceEventTypes(TypeName enrichmentType) {
        return enrichmentsMap.get(enrichmentType.value())
                             .stream()
                             .map(TypeName::of)
                             .collect(toImmutableSet());
    }
}
