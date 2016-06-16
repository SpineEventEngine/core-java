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

import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.Enrichments;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.server.type.EventClass;
import org.spine3.type.TypeName;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation for {@link Enricher}.
 *
 * @author Alexander Yevsyukov
 */
/* package */ final class EnricherImpl implements Enricher {

    /**
     * Available enrichments per event message class.
     */
    private final ImmutableMultimap<EventClass, EnrichmentFunction<?, ?>> enrichments;

    /* package */ EnricherImpl(Builder builder) {
        final Set<EnrichmentFunction<?, ?>> functions = builder.getFunctions();

        // Build the multi-map of all enrichments available per event class.
        final ImmutableMultimap.Builder<EventClass, EnrichmentFunction<?, ?>>
                enrichmentsBuilder = ImmutableMultimap.builder();
        for (EnrichmentFunction<?, ?> function : functions) {
            enrichmentsBuilder.put(EventClass.of(function.getSourceClass()), function);
        }
        this.enrichments = enrichmentsBuilder.build();
    }

    @Override
    public boolean canBeEnriched(Event event) {
        final boolean containsKey = enrichmentRegistered(event);
        if (!containsKey) {
            return false;
        }
        final boolean enrichmentEnabled = Events.isEnrichmentEnabled(event);
        return enrichmentEnabled;
    }

    private boolean enrichmentRegistered(Event event) {
        final Class<? extends Message> eventClass = EventClass.of(event)
                                                              .value();
        final boolean result = enrichments.containsKey(eventClass);
        return result;
    }

    @Override
    public Event enrich(Event event) {
        checkArgument(enrichmentRegistered(event), "No registered enrichment for the event %s", event);
        checkArgument(Events.isEnrichmentEnabled(event), "Enrichment is disabled for the event %s", event);

        final Message eventMessage = Events.getMessage(event);
        final EventClass eventClass = EventClass.of(event);

        // There can be more than one enrichment function per event message class.
        final Collection<EnrichmentFunction<?, ?>> availableFunctions = enrichments.get(eventClass);

        // Build enrichment using all the functions.
        final Enrichments.Builder enrichmentsBuilder = Enrichments.newBuilder();
        for (EnrichmentFunction function : availableFunctions) {
            @SuppressWarnings("unchecked") /** It is OK suppress because we ensure types when we...
                (a) create enrichments,
                (b) put them into {@link #enrichments} by their source message class. **/
            final Message enriched = function.apply(eventMessage);
            checkNotNull(enriched, "Enrichment %s produced null from event message %s", function, eventMessage);
            final String typeName = TypeName.of(enriched)
                                       .toString();
            enrichmentsBuilder.getMap().put(typeName, Any.pack(enriched));
        }

        final EventContext.Builder enrichedContext = event.getContext()
                                                          .toBuilder()
                                                          .setEnrichments(enrichmentsBuilder);
        final Event result = Events.createEvent(eventMessage, enrichedContext.build());
        return result;
    }
}
