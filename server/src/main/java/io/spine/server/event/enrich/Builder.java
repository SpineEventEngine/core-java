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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.spine.core.EventContext;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * The {@code Builder} allows to register {@link EnrichmentFunction}s handled by
 * the {@code Enricher} and set a custom translation function, if needed.
 */
public final class Builder {

    /** Translation functions which perform the enrichment. */
    private final Set<EnrichmentFunction<?, ?, ?>> functions = Sets.newHashSet();

    /**
     * Prevents direct instantiation.
     */
    Builder() {
    }

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
    public <S, T> Builder add(Class<S> sourceFieldClass,
                              Class<T> enrichmentFieldClass,
                              BiFunction<S, EventContext, T> func) {
        checkNotNull(sourceFieldClass);
        checkNotNull(enrichmentFieldClass);
        checkNotNull(func);

        EnrichmentFunction<S, T, ?> newEntry =
                FieldEnrichment.of(sourceFieldClass, enrichmentFieldClass, func);
        checkDuplicate(newEntry);
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
        Enricher result = new Enricher(this);
        validate(result);
        return result;
    }

    @VisibleForTesting
    Set<EnrichmentFunction<?, ?, ?>> getFunctions() {
        return ImmutableSet.copyOf(functions);
    }

    /**
     * Ensures that the passed enrichment function is not yet registered in this builder.
     *
     * @throws IllegalArgumentException
     *         if the builder already has a function, which has the same couple of
     *         source message and target enrichment classes
     */
    private void checkDuplicate(EnrichmentFunction<?, ?, ?> candidate) {
        Optional<EnrichmentFunction<?, ?, ?>> duplicate =
                EnrichmentFunction.firstThat(functions, SameTransition.asFor(candidate));
        if (duplicate.isPresent()) {
            throw newIllegalArgumentException("Enrichment from %s to %s already added as: %s",
                                              candidate.getSourceClass(),
                                              candidate.getEnrichmentClass(),
                                              duplicate.get());
        }
    }

    /** Performs validation of the {@code Enricher} by activating its functions. */
    private static void validate(Enricher enricher) {
        enricher.functions()
                .values()
                .forEach(EnrichmentFunction::activate);
    }
}
