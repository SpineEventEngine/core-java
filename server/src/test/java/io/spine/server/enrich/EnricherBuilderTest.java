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

package io.spine.server.enrich;

import com.google.protobuf.BoolValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.server.enrich.given.event.EbtOrderCreated;
import io.spine.server.enrich.given.event.EbtOrderEvent;
import io.spine.server.enrich.given.event.EbtOrderLineAdded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Enricher Builder should")
class EnricherBuilderTest {

    private EnricherBuilder builder;

    @BeforeEach
    void setUp() {
        builder = Enricher.newBuilder();
    }

    @Nested
    @DisplayName("build Enricher")
    class BuildEnricher {

        @Test
        @DisplayName("if no functions have been registered")
        void noFunctions() {
            assertBuilt(builder.build());
        }

        @Test
        @DisplayName("if functions added")
        void functionsAdded() {
            builder.add(EbtOrderCreated.class, StringValue.class,
                        (e, c) -> StringValue.getDefaultInstance())
                   .add(EbtOrderLineAdded.class, BoolValue.class,
                        (e, c) -> BoolValue.of(true));
            assertBuilt(builder.build());
        }

        void assertBuilt(Enricher enricher) {
            assertThat(enricher).isNotNull();
        }
    }

    @Nested
    @DisplayName("not allow duplicating entries")
    class DupEntries {

        @Test
        @DisplayName("of source and enrichment class pair")
        void classPair() {
            builder.add(EbtOrderLineAdded.class, BoolValue.class,
                        (e, c) -> BoolValue.of(true));

            assertRejects(() ->
                    builder.add(EbtOrderLineAdded.class, BoolValue.class,
                                (e, c) -> BoolValue.of(false))
            );
        }

        @Test
        @DisplayName("when a function is defined for implemened interface")
        void interfaceEnrichment() {
            builder.add(EbtOrderEvent.class, FloatValue.class,
                        (e, c) -> FloatValue.of(3.14f));

            assertRejects(() ->
                    builder.add(EbtOrderCreated.class, FloatValue.class,
                                (e, c) -> FloatValue.of(2.68f))
            );
        }

        private void assertRejects(Executable runnable) {
            assertThrows(IllegalArgumentException.class, runnable);
        }
    }

    @Test
    @DisplayName("do not allow passing an interface as enrichment class")
    void prohibitInterface() {
        assertThrows(IllegalArgumentException.class, () ->
                builder.add(EbtOrderEvent.class, Message.class,
                            (e, c) -> StringValue.getDefaultInstance())
        );
    }
}
