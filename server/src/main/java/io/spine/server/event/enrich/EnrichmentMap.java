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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.code.proto.MessageType;
import io.spine.code.proto.Type;
import io.spine.code.proto.enrichment.EnrichmentType;
import io.spine.type.KnownTypes;
import io.spine.type.TypeName;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * A map from an enrichment type name to the corresponding type name(s) of enriched types.
 */
final class EnrichmentMap {

    private final ImmutableSet<TypeName> enrichments;

    /** Creates new instance loading the map from resources. */
    private EnrichmentMap() {
        this.enrichments = allKnownEnrichments();
    }

    private static ImmutableSet<TypeName> allKnownEnrichments() {
        return KnownTypes.instance()
                         .enrichments()
                         .stream()
                         .map(Type::name)
                         .collect(toImmutableSet());
    }

    /** Loads the map from resources. */
    static EnrichmentMap load() {
        return new EnrichmentMap();
    }

    ImmutableSet<TypeName> enrichmentTypes() {
        return enrichments;
    }

    ImmutableSet<Class<Message>> sourceClasses(TypeName enrichmentType) {
        ImmutableSet<Class<Message>> result =
                sourceTypes(enrichmentType)
                        .stream()
                        .map(TypeName::toMessageClass)
                        .collect(toImmutableSet());
        return result;
    }

    /**
     * Obtains source types enriched by the passed enrichment type.
     */
    ImmutableSet<TypeName> sourceTypes(TypeName enrichmentType) {
        MessageType mt = MessageType.of(enrichmentType.messageDescriptor());
        EnrichmentType enrichment = EnrichmentType.from(mt);
        ImmutableSet<TypeName> result =
                enrichment.knownSources()
                          .stream()
                          .map(Type::name)
                          .collect(toImmutableSet());
        return result;
    }
}
