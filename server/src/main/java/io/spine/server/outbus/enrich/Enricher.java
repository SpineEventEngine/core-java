/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.outbus.enrich;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.type.TypeName;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.LinkedListMultimap.create;
import static com.google.common.collect.Multimaps.synchronizedMultimap;
import static io.spine.core.Enrichments.isEnrichmentEnabled;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Extends information of an command output message basing on its type and content.
 *
 * <p>The class implements the
 * <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/DataEnricher.html">ContentEnricher</a>
 * Enterprise Integration pattern.
 *
 * <p>There is one instance of an {@code Enricher} per
 * {@link io.spine.server.outbus.CommandOutputBus CommandOutputBus}.
 * This instance is called by the bus to enrich a new command output message before it is passed to
 * further processing by dispatchers or handlers.
 *
 * <p>The message is passed to enrichment <em>after</em> it was passed to the corresponding storage.
 * Therefore command output messages are stored without attached enrichment information.
 *
 * @author Alexander Yevsyukov
 */
public class Enricher {

    /** Available enrichment functions per message class. */
    private final Multimap<Class<?>, EnrichmentFunction<?, ?>> functions;

    /** Creates a new builder. */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates a new instance taking functions from the passed builder.
     *
     * <p>Also adds {@link MessageEnrichment}s for all enrichments defined in Protobuf.
     */
    Enricher(Builder builder) {
        final LinkedListMultimap<Class<?>, EnrichmentFunction<?, ?>> rawMap = create();
        final Multimap<Class<?>, EnrichmentFunction<?, ?>> functionMap =
                synchronizedMultimap(rawMap);
        for (EnrichmentFunction<?, ?> function : builder.getFunctions()) {
            functionMap.put(function.getEventClass(), function);
        }
        putMsgEnrichers(functionMap);

        this.functions = functionMap;
    }

    @SuppressWarnings("MethodWithMultipleLoops") // is OK in this case
    private void putMsgEnrichers(Multimap<Class<?>, EnrichmentFunction<?, ?>> functionsMap) {
        final ImmutableMultimap<String, String> enrichmentsMap = EnrichmentsMap.getInstance();
        for (String enrichmentType : enrichmentsMap.keySet()) {
            final Class<Message> enrichmentClass = TypeName.of(enrichmentType)
                                                           .getJavaClass();
            final ImmutableCollection<String> eventTypes = enrichmentsMap.get(enrichmentType);
            for (String eventType : eventTypes) {
                final Class<Message> eventClass = TypeName.of(eventType)
                                                          .getJavaClass();
                final MessageEnrichment msgEnricher =
                        MessageEnrichment.newInstance(this, eventClass, enrichmentClass);
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
     *     <li>There is one or more enrichments defined in Protobuf using
     *     {@code enrichment_for} and/or {@code by} options.
     *     <li>There is one or more field enrichment functions registered for
     *     the class of the passed event.
     *     <li>The flag {@code do_not_enrich} is not set in the {@link io.spine.core.Enrichment
     *     Enrichment} instance of the context of the passed event.
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
        checkTypeRegistered(event);
        checkEnabled(event);

        final EventEnvelope envelope = EventEnvelope.of(event);

        final Action action = new Action(this, envelope);
        final Event result = action.perform();
        return result;
    }

    Collection<EnrichmentFunction<?, ?>> getFunctions(Class<? extends Message> messageClass) {
        return functions.get(messageClass);
    }

    private void checkTypeRegistered(Event event) {
        checkArgument(enrichmentRegistered(event),
                      "No registered enrichment for the event %s", event);
    }

    private static void checkEnabled(Event event) {
        checkArgument(isEnrichmentEnabled(event),
                      "Enrichment is disabled for the event %s", event);
    }

    /**
     * Finds a function that converts an event field into an enrichment field.
     *
     * @param eventFieldClass      the class of event fields
     * @param enrichmentFieldClass the class of enrichment fields
     */
    Optional<EnrichmentFunction<?, ?>> functionFor(Class<?> eventFieldClass,
                                                   Class<?> enrichmentFieldClass) {
        final Optional<EnrichmentFunction<?, ?>> result =
                FluentIterable.from(functions.values())
                              .firstMatch(SupportsFieldConversion.of(eventFieldClass,
                                                                     enrichmentFieldClass));
        return result;
    }

    /**
     * Appends enrichment function at runtime.
     *
     * @param eventFieldClass
     *        the class of the field to enrich
     * @param enrichmentFieldClass
     *        the class of the resulting enrichment field
     * @param func
     *        enrichment function
     * @param <S>
     *        the type of the enriched event message
     * @param <T>
     *        the type of the enrichment field
     */
    public <S, T> void registerFieldEnrichment(Class<S> eventFieldClass,
                                               Class<T> enrichmentFieldClass,
                                               Function<S, T> func) {
        checkNotNull(eventFieldClass);
        checkNotNull(enrichmentFieldClass);
        checkNotNull(func);

        final EnrichmentFunction<S, T> newEntry =
                FieldEnrichment.newInstance(eventFieldClass, enrichmentFieldClass, func);

        checkDuplicate(newEntry, functions.values());
        functions.put(newEntry.getEventClass(), newEntry);
        validate(functions);
    }

    /**
     * The {@code Builder} allows to register {@link EnrichmentFunction}s handled by
     * the {@code Enricher} and set a custom translation function, if needed.
     */
    public static class Builder {

        /** Translation functions which perform the enrichment. */
        private final Set<EnrichmentFunction<?, ?>> functions = Sets.newHashSet();

        /** Prevents instantiation from outside. */
        private Builder() {}

        /**
         * Adds a new field enrichment function.
         *
         * @param  eventFieldClass
         *         a class of the field in the event message
         * @param  enrichmentFieldClass
         *         a class of the field in the enrichment message
         * @param  func
         *         a function which converts fields
         * @return the builder instance
         */
        public <S, T> Builder addFieldEnrichment(Class<S> eventFieldClass,
                                                 Class<T> enrichmentFieldClass,
                                                 Function<S, T> func) {
            checkNotNull(eventFieldClass);
            checkNotNull(enrichmentFieldClass);
            checkNotNull(func);

            final EnrichmentFunction<S, T> newEntry =
                    FieldEnrichment.newInstance(eventFieldClass, enrichmentFieldClass, func);
            checkDuplicate(newEntry, functions);
            functions.add(newEntry);
            return this;
        }

        /** Removes a translation for the passed type. */
        public Builder remove(EnrichmentFunction entry) {
            functions.remove(entry);
            return this;
        }

        /** Creates a new {@code Enricher}. */
        public Enricher build() {
            final Enricher result = new Enricher(this);
            validate(result.functions);
            return result;
        }

        @VisibleForTesting
        Set<EnrichmentFunction<?, ?>> getFunctions() {
            return ImmutableSet.copyOf(functions);
        }
    }

    /**
     * Ensures that the passed enrichment function is not
     *
     * @throws IllegalArgumentException
     *         if the builder already has a function, which has the same couple of source event and
     *         enrichment classes
     */
    private static void checkDuplicate(EnrichmentFunction<?, ?> candidate,
                                       Iterable<EnrichmentFunction<?, ?>> currentFns) {
        final Optional<EnrichmentFunction<?, ?>> duplicate =
                FluentIterable.from(currentFns)
                              .firstMatch(SameTransition.asFor(candidate));
        if (duplicate.isPresent()) {
            throw newIllegalArgumentException("Enrichment from %s to %s already added as: %s",
                                               candidate.getEventClass(),
                                               candidate.getEnrichmentClass(),
                                               duplicate.get());
        }
    }

    /** Performs validation by validating its functions. */
    private static void validate(Multimap<Class<?>, EnrichmentFunction<?, ?>> fnRegistry) {
        for (EnrichmentFunction<?, ?> func : fnRegistry.values()) {
            func.activate();
        }
    }
}
