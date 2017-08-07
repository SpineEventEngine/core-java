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

import com.google.common.base.Predicate;

import javax.annotation.Nullable;

/**
 * The predicate that helps finding a function that converts an event field (of the given class)
 * into an enrichment field (of another given class).
 *
 * @see Enricher#functionFor(Class, Class)
 * @author Alexander Yevsyukov
 */
final class SupportsFieldConversion implements Predicate<EnrichmentFunction<?, ?>> {

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
    public boolean apply(@Nullable EnrichmentFunction<?, ?> input) {
        if (input == null) {
            return false;
        }
        final boolean eventClassMatches =
                eventFieldClass.equals(input.getSourceClass());
        final boolean enrichmentClassMatches =
                enrichmentFieldClass.equals(input.getEnrichmentClass());
        return eventClassMatches && enrichmentClassMatches;
    }
}
