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

package org.spine3.server.event.enrich;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.Enrichment;
import org.spine3.base.Enrichment.Container;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeName;
import org.spine3.protobuf.TypeUrl;
import org.spine3.type.EventClass;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.LinkedListMultimap.create;
import static com.google.common.collect.Multimaps.synchronizedMultimap;
import static org.spine3.base.Events.createEvent;
import static org.spine3.base.Events.getMessage;
import static org.spine3.base.Events.isEnrichmentEnabled;
import static org.spine3.protobuf.Messages.toMessageClass;
import static org.spine3.util.Exceptions.newIllegalArgumentException;

/**
 * {@code EventEnricher} extends information of an event basing on its type and content.
 *
 * <p>The interface implements
 * <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/DataEnricher.html">ContentEnricher</a>
 * Enterprise Integration pattern.
 *
 * <p>There is one instance of an {@code EventEnricher} per {@code BoundedContext}.
 * This instance is called by an {@link org.spine3.server.event.EventBus EventBus} to enrich
 * a new event before it is passed to further processing by dispatchers or handlers.
 *
 * <p>The event is passed to enrichment <em>after</em> it was passed to the
 * {@link org.spine3.server.event.EventStore EventStore}.
 * Therefore events are stored without attached enrichment information.
 *
 * @author Alexander Yevsyukov
 */
public class EventEnricher {

    /** Available enrichment functions per event message class. */
    private final Multimap<Class<?>, EnrichmentFunction<?, ?>> functions;

    private final EnrichmentFunctionAppender appender;

    /** Creates a new builder. */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates a new instance taking functions from the passed builder.
     *
     * <p>Also adds {@link EventMessageEnricher}s for all enrichments defined in Protobuf.
     */
    EventEnricher(Builder builder) {
        final LinkedListMultimap<Class<?>, EnrichmentFunction<?, ?>> rawMap = create();
        final Multimap<Class<?>, EnrichmentFunction<?, ?>> functionMap = synchronizedMultimap(rawMap);
        for (EnrichmentFunction<?, ?> function : builder.getFunctions()) {
            functionMap.put(function.getEventClass(), function);
        }
        putMsgEnrichers(functionMap);

        this.functions = functionMap;
        this.appender = new EnrichmentFunctionAppender(functions);

    }

    @SuppressWarnings("MethodWithMultipleLoops") // is OK in this case
    private void putMsgEnrichers(Multimap<Class<?>, EnrichmentFunction<?, ?>> functionsMap) {
        final ImmutableMultimap<String, String> enrichmentsMap = EventEnrichmentsMap.getInstance();
        for (String enrichmentType : enrichmentsMap.keySet()) {
            final Class<Message> enrichmentClass = toMessageClass(TypeUrl.of(enrichmentType));
            final ImmutableCollection<String> eventTypes = enrichmentsMap.get(enrichmentType);
            for (String eventType : eventTypes) {
                final Class<Message> eventClass = toMessageClass(TypeUrl.of(eventType));
                final EventMessageEnricher msgEnricher =
                        EventMessageEnricher.newInstance(this,
                                                         eventClass,
                                                         enrichmentClass);
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
     *     <li>The flag {@code do_not_enrich} is not set in the
     *     {@code EventContext} of the passed event.
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

        final Message eventMessage = getMessage(event);
        final EventContext eventContext = event.getContext();

        final Action action = new Action(this, eventMessage, eventContext);
        final Event result = action.perform();
        return result;
    }

    /**
     * The method object class for enriching an event.
     */
    private static class Action {
        private final Message eventMessage;
        private final EventContext eventContext;
        private final Collection<EnrichmentFunction<?, ?>> availableFunctions;

        private final Map<String, Any> enrichments = Maps.newHashMap();

        private Action(EventEnricher parent, Message eventMessage, EventContext eventContext) {
            this.eventMessage = eventMessage;
            this.eventContext = eventContext;
            final Class<? extends Message> eventClass = EventClass.of(eventMessage)
                                                             .value();
            final Collection<EnrichmentFunction<?, ?>> functionsPerClass = parent.functions.get(eventClass);
            this.availableFunctions = filter(functionsPerClass, EnrichmentFunction.activeOnly());
        }

        private Event perform() {
            createEnrichments();
            final EventContext enrichedContext = enrichContext();
            final Event result = createEvent(eventMessage, enrichedContext);
            return result;
        }

        private void createEnrichments() {
            for (EnrichmentFunction function : availableFunctions) {
                function.setContext(eventContext);
                final Message enriched = apply(function, eventMessage);
                checkResult(enriched, function);
                final String typeName = TypeName.of(enriched);
                enrichments.put(typeName, AnyPacker.pack(enriched));
            }
        }

        /**
         * Applies the passed function to the message.
         *
         * <p>We suppress the {@code "unchecked"} because we ensure types when we...
         * <ol>
         *      <li>create enrichments,
         *      <li>put them into {@link #functions} by their event message class.
         * </ol>
         */
        @SuppressWarnings("unchecked")
        private static Message apply(EnrichmentFunction function, Message input) {
            final Message result = (Message) function.apply(input);
            return result;
        }

        private void checkResult(Message enriched, EnrichmentFunction function) {
            checkNotNull(
                enriched,
                "EnrichmentFunction %s produced `null` from event message %s",
                function, eventMessage
            );
        }

        private EventContext enrichContext() {
            final Enrichment.Builder enrichment =
                    Enrichment.newBuilder()
                              .setContainer(Container.newBuilder()
                                                     .putAllItems(enrichments));
            return eventContext.toBuilder()
                               .setEnrichment(enrichment)
                               .build();
        }
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
                from(functions.values())
                        .firstMatch(SupportsFieldConversion.of(eventFieldClass,
                                                               enrichmentFieldClass));
        return result;
    }

    public <S, T> void registerFieldEnrichment(Class<S> eventFieldClass,
                                               Class<T> enrichmentFieldClass,
                                               Function<S, T> function) {
        appender.addEntry(eventFieldClass, enrichmentFieldClass, function);
    }

    /**
     * The helper serving to append the enclosing instance of {@code EventEnricher} with
     * the new enrichment configuration rules at runtime.
     */
    private static class EnrichmentFunctionAppender {

        private final Multimap<Class<?>, EnrichmentFunction<?, ?>> destination;

        private EnrichmentFunctionAppender(Multimap<Class<?>,
                                           EnrichmentFunction<?, ?>> destination) {
            this.destination = destination;
        }

        private <S, T> void addEntry(Class<S> eventFieldClass,
                                     Class<T> enrichmentFieldClass,
                                     Function<S, T> function) {
            final EnrichmentFunction<S, T> newEntry = fieldEnrichmentOf(eventFieldClass,
                                                                        enrichmentFieldClass,
                                                                        function);
            checkDuplicate(newEntry, destination.values());
            destination.put(newEntry.getEventClass(), newEntry);

            validate(destination);
        }
    }

    /**
     * The predicate that helps finding a function that converts an event field (of the given class)
     * into an enrichment field (of another given class).
     *
     * @see #functionFor(Class, Class)
     */
    static class SupportsFieldConversion implements Predicate<EnrichmentFunction> {

        private final Class<?> eventFieldClass;
        private final Class<?> enrichmentFieldClass;

        static SupportsFieldConversion of(Class<?> eventFieldClass, Class<?> enrichmentFieldClass) {
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
            final boolean eventClassMatches =
                    eventFieldClass.equals(input.getEventClass());
            final boolean enrichmentClassMatches =
                    enrichmentFieldClass.equals(input.getEnrichmentClass());
            return eventClassMatches && enrichmentClassMatches;
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
    static class SameTransition implements Predicate<EnrichmentFunction> {

        private final EnrichmentFunction function;

        static SameTransition asFor(EnrichmentFunction function) {
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

    /**
     * The {@code Builder} allows to register {@link EnrichmentFunction}s handled by
     * the {@code Enricher} and set a custom translation function, if needed.
     */
    public static class Builder {

        /** Translation functions which perform the enrichment. */
        private final Set<EnrichmentFunction<?, ?>> functions = Sets.newHashSet();

        /** Creates a new instance. */
        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        /**
         * Add a new field enrichment translation function.
         *
         * @param eventFieldClass      a class of the field in the event message
         * @param enrichmentFieldClass a class of the field in the enrichment message
         * @param function             a function which converts fields
         * @return a builder instance
         */
        public <S, T> Builder addFieldEnrichment(Class<S> eventFieldClass,
                                                 Class<T> enrichmentFieldClass,
                                                 Function<S, T> function) {
            final EnrichmentFunction<S, T> newEntry =
                    fieldEnrichmentOf(eventFieldClass, enrichmentFieldClass, function);
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
        public EventEnricher build() {
            final EventEnricher result = new EventEnricher(this);
            validate(result.functions);
            return result;
        }

        @VisibleForTesting
        Set<EnrichmentFunction<?, ?>> getFunctions() {
            return ImmutableSet.copyOf(functions);
        }
    }

    /**
     * A utility method to create an instance of {@link EnrichmentFunction} based on
     * the {@link FieldEnricher}.
     */
    private static <S, T> EnrichmentFunction<S, T> fieldEnrichmentOf(Class<S> eventFieldClass,
                                                                     Class<T> enrichmentFieldClass,
                                                                     Function<S, T> function) {
        checkNotNull(eventFieldClass);
        checkNotNull(enrichmentFieldClass);
        checkNotNull(function);
        return FieldEnricher.newInstance(eventFieldClass,
                                         enrichmentFieldClass,
                                         function);
    }

    /**
     * @throws IllegalArgumentException if the builder already has a function, which has the same
     *                                  couple of source event and enrichment classes
     */
    private static void checkDuplicate(EnrichmentFunction<?, ?> function,
                                       Iterable<EnrichmentFunction<?, ?>> currentFns) {
        final Optional<EnrichmentFunction<?, ?>> duplicate = from(currentFns)
                .firstMatch(SameTransition.asFor(function));
        if (duplicate.isPresent()) {
            throw newIllegalArgumentException("Enrichment from %s to %s already added as: %s",
                                               function.getEventClass(),
                                               function.getEnrichmentClass(),
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
