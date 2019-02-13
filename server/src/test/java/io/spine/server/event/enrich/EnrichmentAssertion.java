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

import com.google.common.collect.ImmutableSet;
import com.google.common.truth.IterableSubject;
import com.google.protobuf.Message;
import io.spine.type.TypeName;

import java.util.stream.Stream;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.truth.Truth.assertThat;

/**
 * Asserts that the passed enrichment class enriches message classes.
 */
final class EnrichmentAssertion {

    private static final EnrichmentMap MAP = EnrichmentMap.load();

    private final TypeName enrichmentType;

    private EnrichmentAssertion(Class<? extends Message> cls) {
        enrichmentType = TypeName.of(cls);
    }

    static EnrichmentAssertion _assert(Class<? extends Message> cls) {
        return new EnrichmentAssertion(cls);
    }

    /**
     * Asserts that the passed enrichment class is used for enriching passed message classes.
     */
    @SafeVarargs
    final void enriches(Class<? extends Message>... expectedClasses) {
        assertEnriches(false, expectedClasses);
    }

    /**
     * Asserts that passed enrichment class enriches <em>only</em> passed source message classes.
     */
    @SafeVarargs
    final void enrichesOnly(Class<? extends Message>... expectedClasses) {
        assertEnriches(true, expectedClasses);
    }

    @SafeVarargs
    private final void assertEnriches(boolean strict, Class<? extends Message>... expectedClasses) {
        ImmutableSet<TypeName> sourceTypes = map().sourceTypes(enrichmentType);
        ImmutableSet<TypeName> expectedTypes =
                Stream.of(expectedClasses)
                      .map(TypeName::of)
                      .collect(toImmutableSet());

        IterableSubject assertThat = assertThat(sourceTypes);
        if (strict) {
            assertThat.containsExactlyElementsIn(expectedTypes);
        } else {
            assertThat.containsAllIn(expectedTypes);
        }
    }

    private static EnrichmentMap map() {
        return MAP;
    }
}
