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

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.EventContext;
import io.spine.server.event.given.EnrichmentFunctionTestEnv.GivenEventMessage;
import io.spine.test.event.ProjectCreated;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EnrichmentFunction should")
class EnrichmentFunctionTest {

    private BiFunction<ProjectCreated, EventContext, ProjectCreated.Enrichment> function;
    private FieldEnrichment<ProjectCreated, EventContext, ProjectCreated.Enrichment> fieldEnrichment;

    @BeforeEach
    void setUp() {
        this.function = (event, context) ->
                ProjectCreated.Enrichment
                        .newBuilder()
                        .setProjectName(event.getProjectId()
                                             .getId())
                        .build();
        this.fieldEnrichment = FieldEnrichment.of(ProjectCreated.class,
                                                  ProjectCreated.Enrichment.class,
                                                  function);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(Message.class, Empty.getDefaultInstance())
                .setDefault(Class.class, Empty.class)
                .setDefault(BiFunction.class, function)
                .testAllPublicStaticMethods(FieldEnrichment.class);
    }

    @Test
    @Disabled("Because there can be cases when source field and target field can be of the same class." +
            "For example, we have String ID, and create text representation basing on this ID.")
    @DisplayName("not accept same source and target class")
    void rejectSameSourceAndTarget() {
        BiFunction<StringValue, EventContext, StringValue> func =
                new BiFunction<StringValue, EventContext, StringValue>() {
                    @Override
                    public @Nullable StringValue apply(@Nullable StringValue input,
                                                       EventContext context) {
                        return null;
                    }
                };

        assertThrows(IllegalArgumentException.class,
                     () -> FieldEnrichment.of(StringValue.class, StringValue.class, func));
    }

    @Test
    @DisplayName("return source class")
    void returnSource() {
        assertEquals(ProjectCreated.class, fieldEnrichment.sourceClass());
    }

    @Test
    @DisplayName("return target class")
    void returnTarget() {
        assertEquals(ProjectCreated.Enrichment.class, fieldEnrichment.targetClass());
    }

    @Test
    @DisplayName("create custom instances")
    void createCustomInstances() {
        assertEquals(fieldEnrichment, FieldEnrichment.of(ProjectCreated.class,
                                                         ProjectCreated.Enrichment.class,
                                                         function));
    }

    @Test
    @DisplayName("apply enrichment")
    void applyEnrichment() {
        ProjectCreated event = GivenEventMessage.projectCreated();

        ProjectCreated.Enrichment enriched =
                fieldEnrichment.apply(event, EventContext.getDefaultInstance());

        assertNotNull(enriched);
        assertEquals(event.getProjectId()
                          .getId(), enriched.getProjectName());
    }

    @Test
    @DisplayName("have `hashCode`")
    void haveHashCode() {
        assertNotEquals(System.identityHashCode(fieldEnrichment), fieldEnrichment.hashCode());
    }

    @Test
    @DisplayName("have `toString`")
    void haveToString() {
        String str = fieldEnrichment.toString();
        assertTrue(str.contains(ProjectCreated.class.getName()));
        assertTrue(str.contains(ProjectCreated.Enrichment.class.getName()));
    }

    @Test
    @DisplayName("support equality")
    void haveSmartEquals() {
        FieldEnrichment<ProjectCreated, EventContext, ProjectCreated.Enrichment> anotherEnricher =
                FieldEnrichment.of(ProjectCreated.class,
                                   ProjectCreated.Enrichment.class,
                                   function);
        new EqualsTester().addEqualityGroup(fieldEnrichment, anotherEnricher)
                          .testEquals();
    }
}
