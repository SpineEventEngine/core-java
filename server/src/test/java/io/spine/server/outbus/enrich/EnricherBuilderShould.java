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

import com.google.common.base.Function;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.core.UserId;
import io.spine.server.event.EventEnricher;
import io.spine.server.outbus.enrich.given.EnricherBuilderTestEnv.Enrichment;
import io.spine.test.Tests;
import io.spine.test.event.ProjectId;
import org.junit.Before;
import org.junit.Test;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.DisplayName;

import static io.spine.protobuf.TypeConverter.toMessage;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class EnricherBuilderShould {

    private Enricher.AbstractBuilder builder;
    private Function<Timestamp, StringValue> function;
    private FieldEnrichment<Timestamp, StringValue, ?> fieldEnrichment;

    @Before
    public void setUp() {
        this.builder = EventEnricher.newBuilder();
        this.function = new Function<Timestamp, StringValue>() {
            @Override
            public @Nullable StringValue apply(@Nullable Timestamp input) {
                if (input == null) {
                    return null;
                }
                return toMessage(Timestamps.toString(input));
            }
        };
        this.fieldEnrichment = FieldEnrichment.of(Timestamp.class, StringValue.class, function);
    }

    @Test
    @DisplayName("build enricher if all functions registered")
    void buildEnricherIfAllFunctionsRegistered() {
        final Enricher enricher = Enrichment.newEnricher();

        assertNotNull(enricher);
    }

    @Test
    @DisplayName("add field enrichment")
    void addFieldEnrichment() {
        builder.add(Timestamp.class, StringValue.class, function);

        assertTrue(builder.getFunctions()
                          .contains(fieldEnrichment));
    }

    @Test
    @DisplayName("remove enrichment function")
    void removeEnrichmentFunction() {
        builder.add(Timestamp.class, StringValue.class, function);

        builder.remove(fieldEnrichment);

        assertTrue(builder.getFunctions()
                          .isEmpty());
    }

    @Test(expected = NullPointerException.class)
    @DisplayName("do not accept null source class for field enrichment")
    void doNotAcceptNullSourceClassForFieldEnrichment() {
        builder.add(Tests.<Class<Timestamp>>nullRef(),
                    StringValue.class,
                    function);
    }

    @Test(expected = NullPointerException.class)
    @DisplayName("do not accept null target class for field enrichment")
    void doNotAcceptNullTargetClassForFieldEnrichment() {
        builder.add(Timestamp.class,
                    Tests.<Class<StringValue>>nullRef(),
                    function);
    }

    @Test(expected = NullPointerException.class)
    @DisplayName("do not accept null function for field enrichment")
    void doNotAcceptNullFunctionForFieldEnrichment() {
        builder.add(Timestamp.class,
                    StringValue.class,
                    Tests.<Function<Timestamp, StringValue>>nullRef());
    }

    @Test(expected = IllegalArgumentException.class)
    @DisplayName("do not accept duplicates for field enrichment")
    void doNotAcceptDuplicatesForFieldEnrichment() {
        builder.add(Timestamp.class, StringValue.class, function);
        // This should fail.
        builder.add(Timestamp.class, StringValue.class, function);
    }

    @Test
    @DisplayName("allow registering no functions")
    void allowRegisteringNoFunctions() {
        final Enricher enricher = EventEnricher.newBuilder()
                                               .build();
        assertNotNull(enricher);
    }

    @Test
    @DisplayName("allow registering just some of expected functions")
    void allowRegisteringJustSomeOfExpectedFunctions() {
        builder.add(ProjectId.class, UserId.class,
                    new Enrichment.GetProjectOwnerId())
               .add(ProjectId.class, String.class,
                    new Enrichment.GetProjectName());
        final Enricher enricher = builder.build();
        assertNotNull(enricher);
    }

    @Test
    @DisplayName("assure that function performs same transition")
    void assureThatFunctionPerformsSameTransition() {
        assertTrue(SameTransition.asFor(fieldEnrichment).apply(fieldEnrichment));
    }

    @Test
    @DisplayName("return false if input to SameTransition predicate is null")
    void returnFalseIfInputToSameTransitionPredicateIsNull() {
        assertFalse(SameTransition.asFor(fieldEnrichment).apply(null));
    }
}
