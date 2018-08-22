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

package io.spine.server.event;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.core.UserId;
import io.spine.server.event.given.EnricherBuilderTestEnv.Enrichment;
import io.spine.test.event.ProjectId;
import io.spine.testing.Tests;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static io.spine.protobuf.TypeConverter.toMessage;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@DisplayName("Enricher Builder should")
class EnricherBuilderTest {

    private Enricher.Builder builder;
    private Function<Timestamp, StringValue> function;
    private FieldEnrichment<Timestamp, StringValue, ?> fieldEnrichment;

    @BeforeEach
    void setUp() {
        this.builder = Enricher.newBuilder();
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

    @Nested
    @DisplayName("build Enricher")
    class BuildEnricher {

        @Test
        @DisplayName("if all functions have been registered")
        void forAllFunctionsRegistered() {
            Enricher enricher = Enrichment.newEnricher();

            assertNotNull(enricher);
        }

        @Test
        @DisplayName("if no functions have been registered")
        void forNoFunctionsRegistered() {
            Enricher enricher = Enricher.newBuilder()
                                        .build();
            assertNotNull(enricher);
        }

        @Test
        @DisplayName("if only some of expected functions have been registered")
        void forSomeFunctionsRegistered() {
            builder.add(ProjectId.class, UserId.class,
                        new Enrichment.GetProjectOwnerId())
                   .add(ProjectId.class, String.class,
                        new Enrichment.GetProjectName());
            Enricher enricher = builder.build();
            assertNotNull(enricher);
        }
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

    @Nested
    @DisplayName("not accept")
    class NotAccept {

        @Test
        @DisplayName("null source class")
        void nullSourceClass() {
            assertThrows(NullPointerException.class,
                         () -> builder.add(Tests.<Class<Timestamp>>nullRef(),
                                           StringValue.class,
                                           function));
        }

        @Test
        @DisplayName("null target class")
        void nullTargetClass() {
            assertThrows(NullPointerException.class,
                         () -> builder.add(Timestamp.class,
                                           Tests.<Class<StringValue>>nullRef(),
                                           function));
        }

        @Test
        @DisplayName("null function")
        void nullFunction() {
            assertThrows(NullPointerException.class,
                         () -> builder.add(Timestamp.class,
                                           StringValue.class,
                                           Tests.<Function<Timestamp, StringValue>>nullRef()));
        }

        @Test
        @DisplayName("duplicate field enrichment function")
        void duplicates() {
            builder.add(Timestamp.class, StringValue.class, function);
            assertThrows(IllegalArgumentException.class,
                         () -> builder.add(Timestamp.class, StringValue.class, function));
        }
    }

    @Test
    @DisplayName("assure that function performs same transition")
    void assureSameTransition() {
        assertTrue(SameTransition.asFor(fieldEnrichment).test(fieldEnrichment));
    }

    @Test
    @DisplayName("return false if input to SameTransition predicate is null")
    void assureNotSameForNull() {
        assertFalse(SameTransition.asFor(fieldEnrichment).test(null));
    }
}
