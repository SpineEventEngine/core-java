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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.code.proto.MessageType;
import io.spine.code.proto.TypeSet;
import io.spine.code.proto.ref.TypeRef;
import io.spine.type.KnownTypes;
import io.spine.type.TypeName;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * A map from an enrichment type name to the corresponding type name(s) of enriched types.
 */
final class EnrichmentMap {

    /** A multi-map from enrichment class name to enriched message class names. */
    private final ImmutableMultimap<String, String> enrichmentsMap;

    /** Creates new instance loading the map from resources. */
    private EnrichmentMap() {
        this.enrichmentsMap = EnrichmentFileSet.loadFromResources();
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

    ImmutableSet<Class<Message>> sourceClasses(TypeName enrichmentType) {
        ImmutableSet<Class<Message>> result =
                sourceTypes(enrichmentType)
                        .stream()
                        .map(TypeName::getMessageClass)
                        .collect(toImmutableSet());
        return result;
    }

    /**
     * Obtains source types enriched by the passed enrichment type.
     */
    ImmutableSet<TypeName> sourceTypes(TypeName enrichmentType) {
        KnownTypes knownTypes = KnownTypes.instance();
        ImmutableCollection<String> sourceRefs = enrichmentsMap.get(enrichmentType.value());
        TypeSet.Builder builder = TypeSet.newBuilder();
        for (String ref : sourceRefs) {
            TypeRef typeRef = TypeRef.parse(ref);
            ImmutableSet<MessageType> matchingRef = knownTypes.allMatching(typeRef);
            builder.addAll(matchingRef);
        }
        TypeSet sourceTypes = builder.build();
        ImmutableSet<TypeName> result =
                sourceTypes.messageTypes()
                           .stream()
                           .map(MessageType::name)
                           .collect(toImmutableSet());
        return result;
    }
}
