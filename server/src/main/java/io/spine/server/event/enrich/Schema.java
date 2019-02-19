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
import com.google.common.collect.LinkedListMultimap;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.code.proto.enrichment.EnrichmentType;
import io.spine.type.KnownTypes;

import java.util.Optional;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Multimaps.toMultimap;
import static io.spine.server.event.enrich.SupportsFieldConversion.supportsConversion;

/**
 * Provides enrichment functions for a {@linkplain #get(Class) source message Java class}.
 */
final class Schema {

    /** Available enrichment functions per Java class. */
    private ImmutableMultimap<Class<?>, EnrichmentFunction<?, ?, ?>> multimap;

    static Schema create(Enricher enricher, EnricherBuilder builder) {
        Builder schemaBuilder = new Builder(enricher, builder);
        return schemaBuilder.build();
    }

    private Schema(ImmutableMultimap<Class<?>, EnrichmentFunction<?, ?, ?>> multimap) {
        this.multimap = checkNotNull(multimap);
    }

    /**
     * Activates all enrichment functions, and compresses the schema to contain only
     * active functions.
     */
    void activate() {
        multimap.values()
                .forEach(EnrichmentFunction::activate);
        LinkedListMultimap<? extends Class<?>, ? extends EnrichmentFunction<?, ?, ?>> compacted =
                multimap.values()
                        .stream()
                        .filter(EnrichmentFunction::isActive)
                        .collect(toMultimap(EnrichmentFunction::sourceClass,
                                            Function.identity(),
                                            LinkedListMultimap::create));
        multimap = ImmutableMultimap.copyOf(compacted);
    }

    /**
     * Obtains a field enrichment function for the passed source/target couple of classes.
     */
    Optional<FieldEnrichment<?, ?, ?>> transition(Class<?> fieldClass,
                                                  Class<?> enrichmentFieldClass) {
        Optional<EnrichmentFunction<?, ?, ?>> found =
                multimap.values()
                        .stream()
                        .filter(supportsConversion(fieldClass, enrichmentFieldClass))
                        .findFirst();
        Optional<FieldEnrichment<?, ?, ?>> result =
                found.filter(f -> f instanceof FieldEnrichment)
                     .map(f -> (FieldEnrichment<?, ?, ?>) f);
        return result;
    }

    /**
     * Obtains active-only functions for enriching the passed message class.
     */
    ImmutableCollection<EnrichmentFunction<?, ?, ?>> get(Class<? extends Message> messageClass) {
        return multimap.get(messageClass);
    }

    /**
     * Verifies if there is at least one enrichment for instances the passed class.
     */
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
            loadEventEnrichments();
            Schema result = new Schema(multimap.build());
            return result;
        }

        private void addFieldEnrichments() {
            for (EnrichmentFunction<?, ?, ?> fn : builder.functions()) {
                multimap.put(fn.sourceClass(), fn);
            }
        }

        private void loadEventEnrichments() {
            ImmutableSet<EnrichmentType> enrichments =
                    KnownTypes.instance()
                              .enrichments();
            enrichments.forEach(
                    e -> e.sourceClasses()
                          .forEach(sourceClass -> addEventEnrichment(sourceClass, e.javaClass()))
            );
        }

        private void addEventEnrichment(Class<? extends Message> sourceClass,
                                        Class<? extends Message> enrichmentClass) {
            @SuppressWarnings("unchecked") /* It is relatively safe to cast since currently
            only event enrichment is available via proto definitions, and the source types loaded by
            EnrichmentMap are all event messages. Schema composition should be extended with
            checking the returned class for enrichments of message other than EnrichmentMessage. */
            Class<EventMessage> eventClass = (Class<EventMessage>) sourceClass;
            MessageEnrichment fn = EventEnrichment.create(enricher, eventClass, enrichmentClass);
            multimap.put(sourceClass, fn);
        }
    }
}
