/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import org.spine3.base.Event;
import org.spine3.base.Events;
import org.spine3.server.type.EventClass;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code Enricher} extends information of an event basing on its type and content.
 *
 * <p>The class implements
 * <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/DataEnricher.html">ContentEnricher</a>
 * Enterprise Integration pattern.
 *
 * <p>There is one instance of this class per {@code BoundedContext}. This instance is called by an {@link EventBus}
 * of to enrich a new event before it is passed to further processing by dispatchers or handlers.
 *
 * <p>The event is passed to enrichment <em>after</em> it was passed to the {@link EventStore}.
 *
 * //TODO:2016-06-14:alexander.yevsyukov: Finish documentation
 *
 * @author Alexander Yevsyukov
 */
public final class Enricher {

    /**
     * Translation functions for supported enrichment types.
     */
    private final ImmutableMap<EnrichmentType<? extends Class<?>, ? extends Class<?>>,
                               Function<?, ?>> translators;

    /**
     * Available enrichments per event message class.
     */
    private final ImmutableMultimap<EventClass, EnrichmentType<?, ?>> enrichments;

    private Enricher(Builder builder) {
        final Map<EnrichmentType<?, ?>, Function<?, ?>> translators = builder.getTranslators();
        this.translators = ImmutableMap.copyOf(translators);

        // Build the multi-map of all enrichments available per event class.
        final ImmutableMultimap.Builder<EventClass, EnrichmentType<?, ?>>
                enrichmentsBuilder = ImmutableMultimap.builder();
        final Set<EnrichmentType<?, ?>> enrichmentTypes = translators.keySet();
        for (EnrichmentType<?, ?> enrichmentType : enrichmentTypes) {
            enrichmentsBuilder.put(EventClass.of(enrichmentType.getSource()), enrichmentType);
        }
        this.enrichments = enrichmentsBuilder.build();
    }

    /**
     * Verifies if the passed event class can be enriched.
     *
     * <p>An event can be enriched if the following conditions are met:
     *
     * <ol>
     *     <li>There is one or more functions registered for an {@link EnrichmentType} where
     *     the passed class is the {@code source}.
     *     <li>The flag {@code do_not_enrich} is not set in the {@code EventContext} of the passed event.
     * </ol>
     *
     * @return {@code true} if the enrichment for the event is possible, {@code false} otherwise
     */
    public boolean canBeEnriched(Event event) {
        final Class<? extends Message> eventClass = EventClass.of(event)
                                                         .value();
        final boolean containsKey = enrichments.containsKey(eventClass);
        final boolean enrichmentEnabled = Events.isEnrichmentEnabled(event);
        return containsKey && enrichmentEnabled;
    }

    /**
     * The default mechanism for enriching messages based on {@code FieldOptions} of
     * Protobuf message definitions.
     *
     * @param <M> a type of the message of the event to enrich
     * @param <E> a type of the enrichment message
     */
    @VisibleForTesting
    /* package */ static class DefaultTranslator<M extends Message, E extends Message> implements Function<M, E> {
        @Nullable
        @Override
        public E apply(@Nullable M input) {
            if (input == null) {
                return null;
            }

            //TODO:2016-06-14:alexander.yevsyukov: Implement
            return null;
        }
    }

    /**
     * The {@code Builder} allows to register {@link EnrichmentType}s handled by the {@code Enricher}
     * and set a custom translation function, if needed.
     */
    public static class Builder {
        /**
         * A map from an enrichment type to a translation function which performs the enrichment.
         */
        private final Map<EnrichmentType<? extends Class<?>, ? extends Class<?>>,
                          Function<?, ?>> translators = Maps.newHashMap();

        /**
         * Adds an {@code EnrichmentType} with the default translation.
         */
        public <M extends Message, E extends Message> Builder add(
                EnrichmentType<Class<? extends M>, Class<? extends E>> entry) {
            translators.put(entry, new DefaultTranslator<M, E>());
            return this;
        }

        /**
         * Adds an {@code EnrichmentType} with a custom translation function.
         */
        public <M extends Message, E extends Message> Builder add(
                EnrichmentType<Class<? extends M>, Class<? extends E>> type,
                Function<M, E> translator) {
            checkNotNull(type);
            checkNotNull(translator);
            if (translators.containsKey(type)) {
                throw new IllegalArgumentException("Enrichment type already added: " + type);
            }

            translators.put(type, translator);
            return this;
        }

        /**
         * Removes a translation for the passed type.
         */
        public Builder remove(EnrichmentType entry) {
            translators.remove(entry);
            return this;
        }

        /**
         * Creates new {@code Enricher}.
         */
        public Enricher build() {
            final Enricher result = new Enricher(this);
            return result;
        }

        /* package */ Map<EnrichmentType<?, ?>, Function<?, ?>> getTranslators() {
            return Collections.unmodifiableMap(translators);
        }
    }
}
