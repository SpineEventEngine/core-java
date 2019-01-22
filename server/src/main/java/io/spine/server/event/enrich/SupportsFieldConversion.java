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

import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The predicate that helps finding a function that converts a message field (of the given class)
 * into an enrichment field (of another given class).
 *
 * @see Enricher#functionFor(Class, Class)
 */
@VisibleForTesting
final class SupportsFieldConversion implements Predicate<EnrichmentFunction<?, ?, ?>> {

    private final Class<?> messageField;
    private final Class<?> enrichmentField;

    static SupportsFieldConversion conversion(Class<?> messageField, Class<?> enrichmentField) {
        return new SupportsFieldConversion(messageField, enrichmentField);
    }

    private SupportsFieldConversion(Class<?> messageField, Class<?> enrichmentField) {
        this.messageField = messageField;
        this.enrichmentField = enrichmentField;
    }

    @Override
    public boolean test(EnrichmentFunction<?, ?, ?> input) {
        checkNotNull(input);
        boolean eventClassMatches = messageField.equals(input.sourceClass());
        boolean enrichmentClassMatches = enrichmentField.equals(input.targetClass());
        return eventClassMatches && enrichmentClassMatches;
    }

    IllegalStateException unsupported() {
        return newIllegalStateException(
                "Unable to get enrichment for the conversion from message field " +
                        "of type `%s` to enrichment field of type `%s`.",
                messageField.getName(),
                enrichmentField.getName()
        );
    }
}
