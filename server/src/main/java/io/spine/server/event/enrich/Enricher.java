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
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import io.spine.annotation.SPI;
import io.spine.core.EventEnvelope;
import io.spine.type.TypeName;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static io.spine.server.event.enrich.MessageEnrichment.create;

/**
 * Enriches events <em>after</em> they are stored, and <em>before</em> they are dispatched.
 *
 * <p>Enrichment schema is constructed like this:
 * <pre>
 *     {@code
 *     Enricher enricher = Enricher.newBuilder()
 *         .add(ProjectId.class, String.class, new BiFunction<ProjectId,  String> { ... } )
 *         .add(ProjectId.class, UserId.class, new BiFunction<ProjectId, UserId> { ... } )
 *         ...
 *         .build();
 *     }
 * </pre>
 */
@SPI
public final class Enricher {

    /** Available enrichment functions per Java class. */
    private final ImmutableMultimap<Class<?>, EnrichmentFunction<?, ?, ?>> functions;

    /**
     * Creates a new instance taking functions from the passed builder.
     *
     * <p>Also adds {@link MessageEnrichment}s for all enrichments defined in Protobuf.
     */
    Enricher(Builder builder) {
        LinkedListMultimap<Class<?>, EnrichmentFunction<?, ?, ?>> funcMap =
                LinkedListMultimap.create();
        for (EnrichmentFunction<?, ?, ?> fn : builder.getFunctions()) {
            funcMap.put(fn.getSourceClass(), fn);
        }
        putMsgEnrichers(funcMap);

        this.functions = ImmutableMultimap.copyOf(funcMap);
    }

    ImmutableMultimap<Class<?>, EnrichmentFunction<?, ?, ?>> functions() {
        return functions;
    }

    @SuppressWarnings("MethodWithMultipleLoops") // is OK in this case
    private void putMsgEnrichers(Multimap<Class<?>, EnrichmentFunction<?, ?, ?>> functions) {
        ImmutableMultimap<String, String> enrichmentsMap = EnrichmentsMap.instance();
        for (String enrichmentType : enrichmentsMap.keySet()) {
            Class<Message> enrichmentClass = TypeName.of(enrichmentType)
                                                     .getMessageClass();
            ImmutableCollection<String> srcMessageTypes = enrichmentsMap.get(enrichmentType);
            for (String srcType : srcMessageTypes) {
                Class<Message> messageClass = TypeName.of(srcType)
                                                      .getMessageClass();
                MessageEnrichment msgEnricher = create(this, messageClass, enrichmentClass);
                functions.put(messageClass, msgEnricher);
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
    public boolean canBeEnriched(EventEnvelope message) {
        if (!enrichmentRegistered(message)) {
            return false;
        }
        boolean enrichmentEnabled = message.isEnrichmentEnabled();
        return enrichmentEnabled;
    }

    private boolean enrichmentRegistered(EventEnvelope message) {
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
     */
    public EventEnvelope enrich(EventEnvelope source) {
        checkTypeRegistered(source);
        checkEnabled(source);

        Action action = new Action(this, source);
        EventEnvelope result = action.perform();
        return result;
    }

    Collection<EnrichmentFunction<?, ?, ?>> getFunctions(Class<? extends Message> messageClass) {
        return functions.get(messageClass);
    }

    private void checkTypeRegistered(EventEnvelope message) {
        checkArgument(enrichmentRegistered(message),
                      "No registered enrichment for the message %s", message);
    }

    private static void checkEnabled(EventEnvelope envelope) {
        checkArgument(envelope.isEnrichmentEnabled(),
                      "Enrichment is disabled for the message %s", envelope.getOuterObject());
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
                functions.values()
                         .stream()
                         .filter(SupportsFieldConversion.of(fieldClass, enrichmentFieldClass))
                         .findFirst();
        return result;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

}
