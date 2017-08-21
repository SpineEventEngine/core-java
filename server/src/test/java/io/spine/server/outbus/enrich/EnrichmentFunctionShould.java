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

import com.google.common.base.Function;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.EventContext;
import io.spine.server.outbus.enrich.given.EnrichmentFunctionTestEnv.GivenEventMessage;
import io.spine.test.event.ProjectCreated;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EnrichmentFunctionShould {

    private Function<ProjectCreated, ProjectCreated.Enrichment> function;
    private FieldEnrichment<ProjectCreated, ProjectCreated.Enrichment, EventContext> fieldEnrichment;

    @Before
    public void setUp() {
        this.function = new Function<ProjectCreated, ProjectCreated.Enrichment>() {
            @Nullable
            @Override
            public ProjectCreated.Enrichment apply(@Nullable ProjectCreated input) {
                if (input == null) {
                    return null;
                }
                final ProjectCreated.Enrichment result = ProjectCreated.Enrichment.newBuilder()
                        .setProjectName(input.getProjectId().getId())
                        .build();
                return result;
            }
        };
        this.fieldEnrichment = FieldEnrichment.of(ProjectCreated.class,
                                                  ProjectCreated.Enrichment.class,
                                                  function);
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Message.class, Empty.getDefaultInstance())
                .setDefault(Class.class, Empty.class)
                .setDefault(Function.class, function)
                .testAllPublicStaticMethods(FieldEnrichment.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_same_source_and_target_class() {
        final Function<StringValue, StringValue> func = new Function<StringValue, StringValue>() {
            @Nullable
            @Override
            public StringValue apply(@Nullable StringValue input) {
                return null;
            }
        };
        FieldEnrichment.of(StringValue.class, StringValue.class, func);
    }

    @Test
    public void return_sourceClass() throws Exception {
        assertEquals(ProjectCreated.class, fieldEnrichment.getSourceClass());
    }

    @Test
    public void return_targetClass() throws Exception {
        assertEquals(ProjectCreated.Enrichment.class, fieldEnrichment.getEnrichmentClass());
    }

    @Test
    public void create_custom_instances() throws Exception {
        assertEquals(fieldEnrichment, FieldEnrichment.of(ProjectCreated.class,
                                                         ProjectCreated.Enrichment.class,
                                                         function));
    }

    @Test
    public void apply_enrichment() throws Exception {
        final ProjectCreated event = GivenEventMessage.projectCreated();

        final ProjectCreated.Enrichment enriched =
                fieldEnrichment.apply(event, EventContext.getDefaultInstance());

        assertNotNull(enriched);
        assertEquals(event.getProjectId()
                          .getId(), enriched.getProjectName());
    }

    @Test
    public void have_hashCode() throws Exception {
        assertNotEquals(System.identityHashCode(fieldEnrichment), fieldEnrichment.hashCode());
    }

    @Test
    public void have_toString() throws Exception {
        final String str = fieldEnrichment.toString();
        assertTrue(str.contains(ProjectCreated.class.getName()));
        assertTrue(str.contains(ProjectCreated.Enrichment.class.getName()));
    }

    @Test
    public void have_smart_equals() {
        final FieldEnrichment<ProjectCreated, ProjectCreated.Enrichment, EventContext> anotherEnricher =
                FieldEnrichment.of(
                    ProjectCreated.class,
                    ProjectCreated.Enrichment.class,
                    function);
        new EqualsTester().addEqualityGroup(fieldEnrichment, anotherEnricher)
                          .testEquals();
    }
}
