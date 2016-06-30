/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.junit.Before;
import org.junit.Test;
import org.spine3.protobuf.Values;
import org.spine3.server.event.enrich.EventEnricher.Builder.SameTransition;
import org.spine3.test.Tests;
import org.spine3.test.event.ProjectId;
import org.spine3.users.UserId;

import javax.annotation.Nullable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"InstanceMethodNamingConvention", "ResultOfMethodCallIgnored"})
public class EventEnricherBuilderShould {

    private EventEnricher.Builder builder;
    private Function<Timestamp, StringValue> function;
    private FieldEnricher<Timestamp, StringValue> fieldEnricher;

    @Before
    public void setUp() {
        this.builder = EventEnricher.newBuilder();
        this.function = new Function<Timestamp, StringValue>() {
            @Nullable
            @Override
            public StringValue apply(@Nullable Timestamp input) {
                if (input == null) {
                    return null;
                }
                return Values.newStringValue(TimeUtil.toString(input));
            }
        };
        this.fieldEnricher = FieldEnricher.newInstance(Timestamp.class, StringValue.class, function);
    }

    @Test
    public void create_new_instance() {
        assertNotNull(EventEnricher.Builder.newInstance());
    }

    @Test
    public void build_enricher_if_all_functions_registered() {
        builder.addFieldEnrichment(ProjectId.class, String.class, new EventEnricherShould.GetProjectName());
        builder.addFieldEnrichment(ProjectId.class, UserId.class, new EventEnricherShould.GetProjectOwnerId());

        assertNotNull(builder.build());
    }

    @Test
    public void add_field_enrichment() {
        builder.addFieldEnrichment(Timestamp.class, StringValue.class, function);

        assertTrue(builder.getFunctions()
                          .contains(fieldEnricher));
    }

    @Test
    public void remove_enrichment_function() {
        builder.addFieldEnrichment(Timestamp.class, StringValue.class, function);

        builder.remove(fieldEnricher);

        assertTrue(builder.getFunctions()
                          .isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_source_class_for_field_enrichment() {
        builder.addFieldEnrichment(Tests.<Class<Timestamp>>nullRef(),
                                   StringValue.class,
                                   function);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_target_class_for_field_enrichment() {
        builder.addFieldEnrichment(Timestamp.class,
                                   Tests.<Class<StringValue>>nullRef(),
                                   function);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_function_for_field_enrichment() {
        builder.addFieldEnrichment(Timestamp.class,
                                   StringValue.class,
                                   Tests.<Function<Timestamp, StringValue>>nullRef());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_duplicates_for_field_enrichment() {
        builder.addFieldEnrichment(Timestamp.class, StringValue.class, function);
        // This should fail.
        builder.addFieldEnrichment(Timestamp.class, StringValue.class, function);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_no_enrichment_functions_registered() {
        EventEnricher.newBuilder()
                     .build();
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_no_GetProjectName_function_registered() {
        builder.addFieldEnrichment(ProjectId.class, UserId.class, new EventEnricherShould.GetProjectOwnerId());
        builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_no_GetProjectOwnerId_function_registered() {
        builder.addFieldEnrichment(ProjectId.class, String.class, new EventEnricherShould.GetProjectName());
        builder.build();
    }

    @Test
    public void assure_that_function_performs_same_transition() {
        assertTrue(SameTransition.asFor(fieldEnricher).apply(fieldEnricher));
    }

    @Test
    public void return_false_if_input_to_SameTransition_predicate_is_null() {
        assertFalse(SameTransition.asFor(fieldEnricher).apply(null));
    }
}
