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
import io.spine.type.TypeName;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.spine.server.event.enrich.EnrichmentFunction.activeOnly;
import static io.spine.server.event.enrich.SupportsFieldConversion.supportsConversion;

/**
 * Enrichment multimap provides an enrichment function for a Java class.
 */
final class Schema {

    /** Available enrichment functions per Java class. */
    private final ImmutableMultimap<Class<?>, EnrichmentFunction<?, ?, ?>> multimap;

    static Schema create(Enricher enricher, EnricherBuilder builder) {
        Builder schemaBuilder = new Builder(enricher, builder);
        return schemaBuilder.build();
    }

    private Schema(ImmutableMultimap<Class<?>, EnrichmentFunction<?, ?, ?>> multimap) {
        this.multimap = checkNotNull(multimap);
    }

    void activate() {
        multimap.values()
                .forEach(EnrichmentFunction::activate);
    }

    Optional<EnrichmentFunction<?, ?, ?>> transition(Class<?> fieldClass,
                                                     Class<?> enrichmentFieldClass) {
        Optional<EnrichmentFunction<?, ?, ?>> result =
                multimap.values()
                        .stream()
                        .filter(supportsConversion(fieldClass, enrichmentFieldClass))
                        .findFirst();
        return result;
    }

    /**
     * Obtains active-only functions for enriching the passed message class.
     */
    ImmutableSet<EnrichmentFunction<?, ?, ?>> get(Class<? extends Message> messageClass) {
        ImmutableCollection<EnrichmentFunction<?, ?, ?>> unfiltered = multimap.get(messageClass);
        ImmutableSet<EnrichmentFunction<?, ?, ?>> result =
                unfiltered.stream()
                          .filter(activeOnly())
                          .collect(toImmutableSet());
        return result;
    }

    boolean supports(Class<?> sourceClass) {
        boolean result = multimap.containsKey(sourceClass);
        return result;
    }

    /**
     * Creates new {@code Schema}
     *
     * <p>{@link MessageEnrichment} requires reference to a parent {@link Enricher}.
     * This class uses the reference to the {@code Enricher} being constructed for this.
     */
    private static final class Builder {

        private final Enricher enricher;
        private final EnricherBuilder builder;
        private final ImmutableMultimap.Builder<Class<?>, EnrichmentFunction<?, ?, ?>> multimap;

        private Builder(Enricher enricher, EnricherBuilder builder) {
            this.enricher = enricher;
            this.builder = builder;
            this.multimap = ImmutableMultimap.builder();
        }

        private Schema build() {
            addFieldEnrichments();
            loadMessageEnrichments();
            Schema result = new Schema(multimap.build());
            return result;
        }

        private void addFieldEnrichments() {
            for (EnrichmentFunction<?, ?, ?> fn : builder.functions()) {
                multimap.put(fn.sourceClass(), fn);
            }
        }

        private void loadMessageEnrichments() {
            EnrichmentMap map = EnrichmentMap.load();
            for (TypeName enrichment : map.enrichmentTypes()) {
                map.sourceClasses(enrichment)
                   .forEach(sourceClass -> addMessageEnrichment(sourceClass, enrichment));
            }
        }

        private void addMessageEnrichment(Class<Message> sourceClass, TypeName enrichment) {
            Class<Message> enrichmentClass = enrichment.getMessageClass();
            MessageEnrichment fn = MessageEnrichment.create(enricher, sourceClass, enrichmentClass);
            multimap.put(sourceClass, fn);
        }
    }
}
