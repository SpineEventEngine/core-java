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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.Enrichments;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.server.type.EventClass;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code Enricher} extends information of an event basing on its type and content.
 *
 * <p>The interface implements
 * <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/DataEnricher.html">ContentEnricher</a>
 * Enterprise Integration pattern.
 *
 * <p>There is one instance of an {@code Enricher} per {@code BoundedContext}. This instance is called by an
 * {@link EventBus} of to enrich a new event before it is passed to further processing by dispatchers or handlers.
 *
 * <p>The event is passed to enrichment <em>after</em> it was passed to the {@link EventStore}.
 *
 * @author Alexander Yevsyukov
 */
public class EventEnricher {

    /**
     * Available enrichments per event message class.
     */
    private final ImmutableMultimap<Class<? extends Message>, EnrichmentFunction<?, ?>> functions;

    /**
     * Creates a new builder.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates a new instance taking functions from the passed builder.
     *
     * <p>Transforms unbound instances of {@code EventEnricher}s into bound ones, passing the reference
     * to this instance.
     */
    private EventEnricher(Builder builder) {
        final Set<EnrichmentFunction<?, ?>> functions = builder.getFunctions();

        // Build the multi-map of all enrichments available per event class.
        final ImmutableMultimap.Builder<Class<? extends Message>, EnrichmentFunction<?, ?>>
                enrichmentsBuilder = ImmutableMultimap.builder();

        for (EnrichmentFunction<?, ?> function : functions) {
            if (function instanceof EventMessageEnricher.Unbound) {
                final EventMessageEnricher.Unbound unbound = (EventMessageEnricher.Unbound) function;
                //noinspection ThisEscapedInObjectConstruction
                function = unbound.toBound(this);
            }
            enrichmentsBuilder.put(function.getSourceClass(), function);
        }
        this.functions = enrichmentsBuilder.build();
    }

    /**
     * Verifies if the passed event class can be enriched.
     *
     * <p>An event can be enriched if the following conditions are met:
     *
     * <ol>
     *     <li>There is one or more functions registered for an {@link EnrichmentFunction} where
     *     the passed class is the {@code source}.
     *     <li>The flag {@code do_not_enrich} is not set in the {@code EventContext} of the passed event.
     * </ol>
     *
     * @return {@code true} if the enrichment for the event is possible, {@code false} otherwise
     */
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
        final boolean result = functions.containsKey(eventClass);
        return result;
    }

    /**
     * Enriches the passed event.
     *
     * @throws IllegalArgumentException if the passed event cannot be enriched
     * @see #canBeEnriched(Event)
     */
    public Event enrich(Event event) {
        checkArgument(enrichmentRegistered(event), "No registered enrichment for the event %s", event);
        checkArgument(Events.isEnrichmentEnabled(event), "Enrichment is disabled for the event %s", event);

        final Message eventMessage = Events.getMessage(event);
        final EventClass eventClass = EventClass.of(event);

        // There can be more than one enrichment function per event message class.
        final Collection<EnrichmentFunction<?, ?>> availableFunctions = functions.get(eventClass.value());

        // Build enrichment using all the functions.
        final Enrichments.Builder enrichmentsBuilder = Enrichments.newBuilder();
        for (EnrichmentFunction function : availableFunctions) {
            @SuppressWarnings("unchecked") /** It is OK suppress because we ensure types when we...
             (a) create enrichments,
             (b) put them into {@link #functions} by their source message class. **/
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

    /**
     * The {@code Builder} allows to register {@link EnrichmentFunction}s handled by the {@code Enricher}
     * and set a custom translation function, if needed.
     */
    public static class Builder {
        /**
         * A map from an enrichment type to a translation function which performs the enrichment.
         */
        private final Set<EnrichmentFunction<? extends Message, ? extends Message>> functions = Sets.newHashSet();

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {}

        public <M extends Message, E extends Message> Builder addEventEnrichment(Class<M> eventMessageClass,
                Class<E> enrichmentClass) {
            checkNotNull(eventMessageClass);
            checkNotNull(enrichmentClass);
            final EnrichmentFunction<M, E> newEntry = EventMessageEnricher.unboundInstance(eventMessageClass,
                                                                                           enrichmentClass);
            checkDuplicate(newEntry);
            functions.add(newEntry);
            return this;
        }

        public <M extends Message, E extends Message> Builder addFieldEnrichment(Class<M> eventMessageClass,
        Class<E> enrichmentClass, Function<M, E> function) {
            checkNotNull(eventMessageClass);
            checkNotNull(enrichmentClass);
            final EnrichmentFunction<M, E> newEntry = FieldEnricher.newInstance(eventMessageClass,
                                                                                enrichmentClass,
                                                                                function);
            checkDuplicate(newEntry);
            functions.add(newEntry);
            return this;
        }

        /**
         * @throws IllegalArgumentException if the builder already has a function, which has the same couple of
         * source and target classes
         */
        private <M extends Message, E extends Message> void checkDuplicate(EnrichmentFunction<M, E> function) {
            final Optional<EnrichmentFunction<? extends Message, ? extends Message>> duplicate =
                    FluentIterable.from(functions)
                                  .firstMatch(new SameTransition(function));
            if (duplicate.isPresent()) {
                final String msg = String.format("Enrichment from %s to %s already added with function: %s ",
                        function.getSourceClass(),
                        function.getTargetClass(),
                        duplicate.get().getFunction());
                throw new IllegalArgumentException(msg);
            }
        }

        /**
         * A helper predicate that allows to find functions with the same transition from
         * source to target class
         *
         * <p>Such functions are not necessarily equal because they may have different translators.
         * @see EnrichmentFunction
         */
        private static class SameTransition implements Predicate<EnrichmentFunction> {

            private final EnrichmentFunction function;

            private SameTransition(EnrichmentFunction function) {
                this.function = checkNotNull(function);
            }

            @Override
            public boolean apply(@Nullable EnrichmentFunction input) {
                if (input == null) {
                    return false;
                }
                final boolean sameSourceClass = function.getSourceClass()
                                                        .equals(input.getSourceClass());
                final boolean sameTargetClass = function.getTargetClass()
                                                        .equals(input.getTargetClass());
                return sameSourceClass && sameTargetClass;
            }
        }

        /**
         * Removes a translation for the passed type.
         */
        public Builder remove(EnrichmentFunction entry) {
            functions.remove(entry);
            return this;
        }

        /**
         * Creates new {@code Enricher}.
         */
        public EventEnricher build() {
            final EventEnricher result = new EventEnricher(this);
            result.validate();
            return result;
        }

        private void validateCompleteness(EventEnricher result) {
            //TODO:2016-06-17:alexander.yevsyukov: Validate completeness of the translation schema by traversing
            // DefaultTranslator instances and checking if the field definitions are also covered by functions we have.
        }

        /* package */ Set<EnrichmentFunction<?, ?>> getFunctions() {
            return Collections.unmodifiableSet(functions);
        }
    }

    /**
     * @throws IllegalStateException if there is a missing function for field enrichments entailed
     * from annotations defined in the added event enrichments
     */
    private void validate() {

    }
}
