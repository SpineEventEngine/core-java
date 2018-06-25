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
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.EventContext;
import io.spine.server.outbus.enrich.given.EnrichmentFunctionTestEnv.GivenEventMessage;
import io.spine.test.event.ProjectCreated;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EnrichmentFunctionShould {

    private Function<ProjectCreated, ProjectCreated.Enrichment> function;
    private FieldEnrichment<ProjectCreated, ProjectCreated.Enrichment, EventContext> fieldEnrichment;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        this.function = input -> {
            if (input == null) {
                return null;
            }
            ProjectCreated.Enrichment.Builder result = ProjectCreated.Enrichment
                    .newBuilder()
                    .setProjectName(input.getProjectId()
                                         .getId());
            return result.build();
        };
        this.fieldEnrichment = FieldEnrichment.of(ProjectCreated.class,
                                                  ProjectCreated.Enrichment.class,
                                                  function);
    }

    @Test
    @DisplayName("pass null tolerance check")
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(Message.class, Empty.getDefaultInstance())
                .setDefault(Class.class, Empty.class)
                .setDefault(Function.class, function)
                .testAllPublicStaticMethods(FieldEnrichment.class);
    }

    @Test
    @DisplayName("do not accept same source and target class")
    void doNotAcceptSameSourceAndTargetClass() {
        Function<StringValue, StringValue> func = new Function<StringValue, StringValue>() {
            @Override
            public @Nullable StringValue apply(@Nullable StringValue input) {
                return null;
            }
        };

        thrown.expect(IllegalArgumentException.class);
        FieldEnrichment.of(StringValue.class, StringValue.class, func);
    }

    @Test
    @DisplayName("return sourceClass")
    void returnSourceClass() {
        assertEquals(ProjectCreated.class, fieldEnrichment.getSourceClass());
    }

    @Test
    @DisplayName("return targetClass")
    void returnTargetClass() {
        assertEquals(ProjectCreated.Enrichment.class, fieldEnrichment.getEnrichmentClass());
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
    @DisplayName("have hashCode")
    void haveHashCode() {
        assertNotEquals(System.identityHashCode(fieldEnrichment), fieldEnrichment.hashCode());
    }

    @Test
    @DisplayName("have toString")
    void haveToString() {
        final String str = fieldEnrichment.toString();
        assertTrue(str.contains(ProjectCreated.class.getName()));
        assertTrue(str.contains(ProjectCreated.Enrichment.class.getName()));
    }

    @Test
    @DisplayName("have smart equals")
    void haveSmartEquals() {
        FieldEnrichment<ProjectCreated, ProjectCreated.Enrichment, EventContext> anotherEnricher =
                FieldEnrichment.of(ProjectCreated.class,
                                   ProjectCreated.Enrichment.class,
                                   function);
        new EqualsTester().addEqualityGroup(fieldEnrichment, anotherEnricher)
                          .testEquals();
    }
}
