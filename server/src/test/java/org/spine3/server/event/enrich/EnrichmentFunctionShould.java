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

package org.spine3.server.event.enrich;

import com.google.common.base.Function;
import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.event.Given;
import org.spine3.test.Tests;
import org.spine3.test.event.ProjectCreated;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EnrichmentFunctionShould {

    private Function<ProjectCreated, ProjectCreated.Enrichment> function;
    private FieldEnricher<ProjectCreated, ProjectCreated.Enrichment> fieldEnricher;

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
        this.fieldEnricher = FieldEnricher.newInstance(ProjectCreated.class,
                                                       ProjectCreated.Enrichment.class,
                                                       function);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_source_class() {
        FieldEnricher.newInstance(Tests.<Class<ProjectCreated>>nullRef(), ProjectCreated.Enrichment.class, function);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_target_class() {
        FieldEnricher.newInstance(ProjectCreated.class, Tests.<Class<ProjectCreated.Enrichment>>nullRef(), function);
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
        FieldEnricher.newInstance(StringValue.class, StringValue.class, func);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_translator_function() {
        FieldEnricher.newInstance(BoolValue.class,
                                  ProjectCreated.Enrichment.class,
                                  Tests.<Function<BoolValue, ProjectCreated.Enrichment>>nullRef());
    }

    @Test
    public void return_sourceClass() throws Exception {
        assertEquals(ProjectCreated.class, fieldEnricher.getEventClass());
    }

    @Test
    public void return_targetClass() throws Exception {
        assertEquals(ProjectCreated.Enrichment.class, fieldEnricher.getEnrichmentClass());
    }

    @Test
    public void create_custom_instances() throws Exception {
        assertEquals(fieldEnricher, FieldEnricher.newInstance(ProjectCreated.class,
                                                              ProjectCreated.Enrichment.class,
                                                              function));
    }

    @Test
    public void return_function() {
        assertNotNull(fieldEnricher.getFunction());
    }

    @Test
    public void apply_enrichment() throws Exception {
        final ProjectCreated event = Given.EventMessage.projectCreated();

        final ProjectCreated.Enrichment enriched = fieldEnricher.apply(event);

        assertNotNull(enriched);
        assertEquals(event.getProjectId()
                          .getId(), enriched.getProjectName());
    }

    @Test
    public void have_hashCode() throws Exception {
        assertNotEquals(System.identityHashCode(fieldEnricher), fieldEnricher.hashCode());
    }

    @Test
    public void have_toString() throws Exception {
        final String str = fieldEnricher.toString();
        assertTrue(str.contains(ProjectCreated.class.getName()));
        assertTrue(str.contains(ProjectCreated.Enrichment.class.getName()));
    }

    @Test
    public void return_null_on_applying_null() {
        assertNull(fieldEnricher.apply(Tests.<ProjectCreated>nullRef()));
    }

    @SuppressWarnings("EqualsWithItself")
    @Test
    public void have_smart_equals() {
        assertTrue(fieldEnricher.equals(fieldEnricher));
        assertFalse(fieldEnricher.equals(Tests.<EnrichmentFunction<ProjectCreated, ProjectCreated.Enrichment>>nullRef()));
    }
}
