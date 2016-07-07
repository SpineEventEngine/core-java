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

package org.spine3.server.event.enrich;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.Enrichments;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventStore;
import org.spine3.server.type.EventClass;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Events.*;
import static org.spine3.protobuf.Messages.toMessageClass;

/**
 * {@code Enricher} extends information of an event basing on its type and content.
 *
 * <p>The interface implements
 * <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/DataEnricher.html">ContentEnricher</a>
 * Enterprise Integration pattern.
 *
 * <p>There is one instance of an {@code Enricher} per {@code BoundedContext}. This instance is called by an
 * {@link EventBus} to enrich a new event before it is passed to further processing by dispatchers or handlers.
 *
 * <p>The event is passed to enrichment <em>after</em> it was passed to the {@link EventStore}.
 *
 * @author Alexander Yevsyukov
 */
public class EventEnricher {

    /** Available enrichment functions per event message class. */
    private final ImmutableMultimap<Class<?>, EnrichmentFunction<?, ?>> functions;

    /** Creates a new builder. */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates a new instance taking functions from the passed builder.
     *
     * <p>Also adds {@link EventMessageEnricher}s for all enrichments defined in Protobuf.
     */
    private EventEnricher(Builder builder) {
        final ImmutableMultimap.Builder<Class<?>, EnrichmentFunction<?, ?>> functionsMap = ImmutableMultimap.builder();
        for (EnrichmentFunction<?, ?> function : builder.getFunctions()) {
            functionsMap.put(function.getEventClass(), function);
        }
        putMsgEnrichers(functionsMap);
        this.functions = functionsMap.build();
    }

    @SuppressWarnings("MethodWithMultipleLoops") // is OK in this case
    private void putMsgEnrichers(ImmutableMultimap.Builder<Class<?>, EnrichmentFunction<?, ?>> functionsMap) {
        final ImmutableMultimap<TypeName, TypeName> enrichmentsMap = EventEnrichmentsMap.getInstance();
        for (TypeName enrichmentType : enrichmentsMap.keySet()) {
            final Class<Message> enrichmentClass = toMessageClass(enrichmentType);
            final ImmutableCollection<TypeName> eventTypes = enrichmentsMap.get(enrichmentType);
            for (TypeName eventType : eventTypes) {
                final Class<Message> eventClass = toMessageClass(eventType);
                final EventMessageEnricher msgEnricher = EventMessageEnricher.newInstance(this, eventClass, enrichmentClass);
                functionsMap.put(eventClass, msgEnricher);
            }
        }
    }

    /**
     * Verifies if the passed event class can be enriched.
     *
     * <p>An event can be enriched if the following conditions are met:
     *
     * <ol>
     *     <li>There is one or more enrichments defined in Protobuf using {@code enrichment_for} and/or {@code by} options.
     *     <li>There is one or more field enrichment functions registered for the class of the passed event.
     *     <li>The flag {@code do_not_enrich} is not set in the {@code EventContext} of the passed event.
     * </ol>
     *
     * @return {@code true} if the enrichment for the event is possible, {@code false} otherwise
     */
    public boolean canBeEnriched(Event event) {
        if (!enrichmentRegistered(event)) {
            return false;
        }
        final boolean enrichmentEnabled = isEnrichmentEnabled(event);
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
        checkArgument(isEnrichmentEnabled(event), "Enrichment is disabled for the event %s", event);

        final Message eventMessage = getMessage(event);
        final EventClass eventClass = EventClass.of(event);
        final Collection<EnrichmentFunction<?, ?>> availableFunctions = functions.get(eventClass.value());
        final Map<String, Any> enrichments = Maps.newHashMap();
        for (EnrichmentFunction function : availableFunctions) {
            function.setContext(event.getContext());
            final Message enriched = apply(function, eventMessage);
            checkNotNull(enriched, "EnrichmentFunction %s produced `null` from event message %s",
                                    function, eventMessage);
            final String typeName = TypeName.of(enriched)
                                            .toString();
            enrichments.put(typeName, Any.pack(enriched));
        }
        final EventContext enrichedContext = event.getContext()
                                                  .toBuilder()
                                                  .setEnrichments(Enrichments.newBuilder()
                                                                             .putAllMap(enrichments))
                                                  .build();
        final Event result = createEvent(eventMessage, enrichedContext);
        return result;
    }

    private static Message apply(EnrichmentFunction function, Message input) {
        @SuppressWarnings("unchecked") /** It is OK to suppress because we ensure types when we...
         (a) create enrichments,
         (b) put them into {@link #functions} by their event message class. **/
        final Message result = (Message) function.apply(input);
        return result;
    }

    /* package */ Optional<EnrichmentFunction<?, ?>> functionFor(Class<?> eventFieldClass,
                                                                 Class<?> enrichmentFieldClass) {
        final Optional<EnrichmentFunction<?, ?>> result =
                FluentIterable.from(functions.values())
                              .firstMatch(SupportsFieldConversion.of(eventFieldClass, enrichmentFieldClass));
        return result;
    }

    /* package */ static class SupportsFieldConversion implements Predicate<EnrichmentFunction> {

        private final Class<?> eventFieldClass;
        private final Class<?> enrichmentFieldClass;

        /* package */ static SupportsFieldConversion of(Class<?> eventFieldClass, Class<?> enrichmentFieldClass) {
            return new SupportsFieldConversion(eventFieldClass, enrichmentFieldClass);
        }

        private SupportsFieldConversion(Class<?> eventFieldClass, Class<?> enrichmentFieldClass) {
            this.eventFieldClass = eventFieldClass;
            this.enrichmentFieldClass = enrichmentFieldClass;
        }

        @Override
        public boolean apply(@Nullable EnrichmentFunction input) {
            if (input == null) {
                return false;
            }
            final boolean eventClassMatches = eventFieldClass.equals(input.getEventClass());
            final boolean enrichmentClassMatches = enrichmentFieldClass.equals(input.getEnrichmentClass());
            return eventClassMatches && enrichmentClassMatches;
        }
    }

    /**
     * The {@code Builder} allows to register {@link EnrichmentFunction}s handled by the {@code Enricher}
     * and set a custom translation function, if needed.
     */
    public static class Builder {

        /** Translation functions which perform the enrichment. */
        private final Set<EnrichmentFunction<?, ?>> functions = Sets.newHashSet();

        /** Creates a new instance. */
        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {}

        /**
         * Add a new field enrichment translation function.
         *
         * @param eventFieldClass a class of the field in the event message
         * @param enrichmentFieldClass a class of the field in the enrichment message
         * @param function a function which converts fields
         * @return a builder instance
         */
        public <S, T> Builder addFieldEnrichment(Class<S> eventFieldClass,
                                                 Class<T> enrichmentFieldClass,
                                                 Function<S, T> function) {
            checkNotNull(eventFieldClass);
            checkNotNull(enrichmentFieldClass);
            final EnrichmentFunction<S, T> newEntry = FieldEnricher.newInstance(eventFieldClass,
                                                                                enrichmentFieldClass,
                                                                                function);
            checkDuplicate(newEntry);
            functions.add(newEntry);
            return this;
        }

        /**
         * @throws IllegalArgumentException if the builder already has a function, which has the same couple of
         *                                  source event and enrichment classes
         */
        private void checkDuplicate(EnrichmentFunction<?, ?> function) {
            final Optional<EnrichmentFunction<?, ?>> duplicate = FluentIterable.from(functions)
                    .firstMatch(SameTransition.asFor(function));
            if (duplicate.isPresent()) {
                final String msg = String.format("Enrichment from %s to %s already added as: %s",
                        function.getEventClass(),
                        function.getEnrichmentClass(),
                        duplicate.get());
                throw new IllegalArgumentException(msg);
            }
        }

        /**
         * A helper predicate that allows to find functions with the same transition from
         * source event to enrichment class.
         *
         * <p>Such functions are not necessarily equal because they may have different translators.
         *
         * @see EnrichmentFunction
         */
        @VisibleForTesting
        /* package */  static class SameTransition implements Predicate<EnrichmentFunction> {

            private final EnrichmentFunction function;

            /* package */ static SameTransition asFor(EnrichmentFunction function) {
                return new SameTransition(function);
            }

            private SameTransition(EnrichmentFunction function) {
                this.function = checkNotNull(function);
            }

            @Override
            public boolean apply(@Nullable EnrichmentFunction input) {
                if (input == null) {
                    return false;
                }
                final boolean sameSourceClass = function.getEventClass()
                                                        .equals(input.getEventClass());
                final boolean sameEnrichmentClass = function.getEnrichmentClass()
                                                        .equals(input.getEnrichmentClass());
                return sameSourceClass && sameEnrichmentClass;
            }
        }

        /** Removes a translation for the passed type. */
        public Builder remove(EnrichmentFunction entry) {
            functions.remove(entry);
            return this;
        }

        /** Creates a new {@code Enricher}. */
        public EventEnricher build() {
            final EventEnricher result = new EventEnricher(this);
            result.validate();
            return result;
        }

        @VisibleForTesting
        /* package */ Set<EnrichmentFunction<?, ?>> getFunctions() {
            return ImmutableSet.copyOf(functions);
        }
    }

    /** Performs validation by validating its functions. */
    private void validate() {
        for (EnrichmentFunction<?, ?> func : functions.values()) {
            func.validate();
        }
    }
}
