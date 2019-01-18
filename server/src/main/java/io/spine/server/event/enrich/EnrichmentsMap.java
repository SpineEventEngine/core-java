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
import com.google.protobuf.Message;
import io.spine.Resources;
import io.spine.type.TypeName;

import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import static io.spine.io.PropertyFiles.loadAllProperties;

/**
 * A map from an event enrichment Protobuf type name to the corresponding
 * type name(s) of event(s) to enrich.
 *
 * <p>Example:
 * <p>{@code proto.type.MyEventEnrichment} - {@code proto.type.FirstEvent},
 * {@code proto.type.SecondEvent}
 */
final class EnrichmentsMap {

    /** A map from enrichment class name to enriched message class name. */
    private static final ImmutableMultimap<String, String> enrichmentsMap = buildEnrichmentsMap();

    /** Prevents instantiation of this utility class. */
    private EnrichmentsMap() {
    }

    /** Obtains immutable map from enrichment class name to enriched message class name. */
    static ImmutableMultimap<String, String> instance() {
        return enrichmentsMap;
    }

    static Collection<String> getEventTypes(Class<? extends Message> enrichmentClass) {
        String enrichmentType = TypeName.of(enrichmentClass)
                                        .value();
        ImmutableCollection<String> result = instance().get(enrichmentType);
        return result;
    }

    private static ImmutableMultimap<String, String> buildEnrichmentsMap() {
        Set<Properties> propertiesSet = loadAllProperties(Resources.ENRICHMENTS);
        EnrichmentMapBuilder builder = new EnrichmentMapBuilder(propertiesSet);
        ImmutableMultimap<String, String> result = builder.build();
        return result;
    }

}
