/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import io.spine.annotation.SPI;
import io.spine.core.EnrichableMessageEnvelope;
import io.spine.type.TypeName;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.LinkedListMultimap.create;
import static io.spine.server.outbus.enrich.MessageEnrichment.create;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Extends information of a command output message basing on its type and content.
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
@SPI
public abstract class Enricher<M extends EnrichableMessageEnvelope<?, ?, C>, C extends Message> {

    /** Available enrichment functions per Java class. */
    private final ImmutableMultimap<Class<?>, EnrichmentFunction<?, ?, ?>> functions;

    /**
     * Creates a new instance taking functions from the passed builder.
     *
     * <p>Also adds {@link MessageEnrichment}s for all enrichments defined in Protobuf.
     */
    protected Enricher(AbstractBuilder<? extends Enricher, ?> builder) {
        LinkedListMultimap<Class<?>, EnrichmentFunction<?, ?, ?>> funcMap = create();
        for (EnrichmentFunction<?, ?, ?> function : builder.getFunctions()) {
            funcMap.put(function.getSourceClass(), function);
        }
        putMsgEnrichers(funcMap);

        this.functions = ImmutableMultimap.copyOf(funcMap);
    }

    @SuppressWarnings("MethodWithMultipleLoops") // is OK in this case
    private void putMsgEnrichers(Multimap<Class<?>, EnrichmentFunction<?, ?, ?>> functionsMap) {
        ImmutableMultimap<String, String> enrichmentsMap = EnrichmentsMap.getInstance();
        for (String enrichmentType : enrichmentsMap.keySet()) {
            Class<Message> enrichmentClass = TypeName.of(enrichmentType)
                                                           .getMessageClass();
            ImmutableCollection<String> srcMessageTypes = enrichmentsMap.get(enrichmentType);
            for (String srcType : srcMessageTypes) {
                Class<Message> messageClass = TypeName.of(srcType)
                                                            .getMessageClass();
                MessageEnrichment msgEnricher = create(this, messageClass, enrichmentClass);
                functionsMap.put(messageClass, msgEnricher);
            }
        }
    }

    /**
     * Verifies if the passed message can be enriched.
     *
     * <p>An message can be enriched if the following conditions are met:
     *
     * <ol>
     *     <li>There is one or more enrichments defined in Protobuf using
     *     {@code enrichment_for} and/or {@code by} options.
     *     <li>There is one or more field enrichment functions registered for
     *     the class of the passed message.
     *     <li>The flag {@code do_not_enrich} is not set in the {@link io.spine.core.Enrichment
     *     Enrichment} instance of the context of the outer object of the message.
     * </ol>
     *
     * @param message the message to inspect
     * @return {@code true} if the message can be enriched, {@code false} otherwise
     */
    public boolean canBeEnriched(M message) {
        if (!enrichmentRegistered(message)) {
            return false;
        }
        boolean enrichmentEnabled = message.isEnrichmentEnabled();
        return enrichmentEnabled;
    }

    private boolean enrichmentRegistered(M message) {
        boolean result = functions.containsKey(message.getMessageClass()
                                                            .value());
        return result;
    }

    /**
     * Enriches the passed message.
     *
     * @param  source
     *         the envelope with the source message
     * @throws IllegalArgumentException
     *         if the passed message cannot be enriched
     * @see    Enricher#canBeEnriched(EnrichableMessageEnvelope)
     */
    public M enrich(M source) {
        checkTypeRegistered(source);
        checkEnabled(source);

        Action<M, C> action = new Action<>(this, source);
        M result = action.perform();
        return result;
    }

    Collection<EnrichmentFunction<?, ?, ?>> getFunctions(Class<? extends Message> messageClass) {
        return functions.get(messageClass);
    }

    private void checkTypeRegistered(M message) {
        checkArgument(enrichmentRegistered(message),
                      "No registered enrichment for the message %s", message);
    }

    private static <M extends EnrichableMessageEnvelope> void checkEnabled(M envelope) {
        checkArgument(envelope.isEnrichmentEnabled(),
                      "Enrichment is disabled for the message %s", envelope.getOuterObject());
    }

    protected ImmutableMultimap<Class<?>, EnrichmentFunction<?, ?, ?>> functions() {
        return functions;
    }

    /**
     * Finds a function that converts an source message field into an enrichment field.
     *
     * @param fieldClass
     *        the class of the source field
     * @param enrichmentFieldClass
     *        the class of the enrichment field
     */
    Optional<EnrichmentFunction<?, ?, ?>> functionFor(Class<?> fieldClass,
                                                   Class<?> enrichmentFieldClass) {
        Optional<EnrichmentFunction<?, ?, ?>> result =
                FluentIterable.from(functions.values())
                              .firstMatch(SupportsFieldConversion.of(fieldClass,
                                                                     enrichmentFieldClass));
        return result;
    }

    /**
     * The {@code Builder} allows to register {@link EnrichmentFunction}s handled by
     * the {@code Enricher} and set a custom translation function, if needed.
     */
    protected abstract static class AbstractBuilder<E extends Enricher<?, ?>,
                                                    B extends AbstractBuilder<E, B>> {

        /** Translation functions which perform the enrichment. */
        private final Set<EnrichmentFunction<?, ?, ?>> functions = Sets.newHashSet();

        /** Prevents instantiation from outside. */
        protected AbstractBuilder() {}

        protected abstract E createEnricher();

        /**
         * Adds a new field enrichment function.
         *
         * @param  sourceFieldClass
         *         a class of the field in the source message
         * @param  enrichmentFieldClass
         *         a class of the field in the enrichment message
         * @param  func
         *         a function which converts fields
         * @return the builder instance
         */
        public <S, T> B add(Class<S> sourceFieldClass,
                            Class<T> enrichmentFieldClass,
                            Function<S, T> func) {
            checkNotNull(sourceFieldClass);
            checkNotNull(enrichmentFieldClass);
            checkNotNull(func);

            EnrichmentFunction<S, T, ?> newEntry =
                    FieldEnrichment.of(sourceFieldClass, enrichmentFieldClass, func);
            checkDuplicate(newEntry, functions);
            functions.add(newEntry);
            return castThis();
        }

        /** Removes a translation for the passed type. */
        public B remove(EnrichmentFunction entry) {
            functions.remove(entry);
            return castThis();
        }

        /** Creates a new {@code Enricher}. */
        public E build() {
            E result = createEnricher();
            validate(result.functions());
            return result;
        }

        @VisibleForTesting
        Set<EnrichmentFunction<?, ?, ?>> getFunctions() {
            return ImmutableSet.copyOf(functions);
        }

        /** Casts this to generic type to provide type covariance in the derived classes. */
        @SuppressWarnings("unchecked") // See Javadoc
        private B castThis() {
            return (B) this;
        }
    }

    /**
     * Ensures that the passed enrichment function is not
     *
     * @throws IllegalArgumentException
     *         if the builder already has a function, which has the same couple of
     *         source message and target enrichment classes
     */
    private static void checkDuplicate(EnrichmentFunction<?, ?, ?> candidate,
                                       Iterable<EnrichmentFunction<?, ?, ?>> currentFns) {
        Optional<EnrichmentFunction<?, ?, ?>> duplicate =
                EnrichmentFunction.firstThat(currentFns, SameTransition.asFor(candidate));
        if (duplicate.isPresent()) {
            throw newIllegalArgumentException("Enrichment from %s to %s already added as: %s",
                                               candidate.getSourceClass(),
                                               candidate.getEnrichmentClass(),
                                               duplicate.get());
        }
    }

    /** Performs validation by validating its functions. */
    private static void validate(Multimap<Class<?>, EnrichmentFunction<?, ?, ?>> fnRegistry) {
        for (EnrichmentFunction<?, ?, ?> func : fnRegistry.values()) {
            func.activate();
        }
    }
}
