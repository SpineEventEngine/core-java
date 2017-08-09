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

import javax.annotation.Nullable;

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
            @Nullable
            @Override
            public StringValue apply(@Nullable Timestamp input) {
                if (input == null) {
                    return null;
                }
                return toMessage(Timestamps.toString(input));
            }
        };
        this.fieldEnrichment = FieldEnrichment.of(Timestamp.class, StringValue.class, function);
    }

    @Test
    public void build_enricher_if_all_functions_registered() {
        final Enricher enricher = Enrichment.newEnricher();

        assertNotNull(enricher);
    }

    @Test
    public void add_field_enrichment() {
        builder.add(Timestamp.class, StringValue.class, function);

        assertTrue(builder.getFunctions()
                          .contains(fieldEnrichment));
    }

    @Test
    public void remove_enrichment_function() {
        builder.add(Timestamp.class, StringValue.class, function);

        builder.remove(fieldEnrichment);

        assertTrue(builder.getFunctions()
                          .isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_source_class_for_field_enrichment() {
        builder.add(Tests.<Class<Timestamp>>nullRef(),
                    StringValue.class,
                    function);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_target_class_for_field_enrichment() {
        builder.add(Timestamp.class,
                    Tests.<Class<StringValue>>nullRef(),
                    function);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_function_for_field_enrichment() {
        builder.add(Timestamp.class,
                    StringValue.class,
                    Tests.<Function<Timestamp, StringValue>>nullRef());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_duplicates_for_field_enrichment() {
        builder.add(Timestamp.class, StringValue.class, function);
        // This should fail.
        builder.add(Timestamp.class, StringValue.class, function);
    }

    @Test
    public void allow_registering_no_functions() {
        final Enricher enricher = EventEnricher.newBuilder()
                                               .build();
        assertNotNull(enricher);
    }

    @Test
    public void allow_registering_just_some_of_expected_functions() {
        builder.add(ProjectId.class, UserId.class,
                    new Enrichment.GetProjectOwnerId())
               .add(ProjectId.class, String.class,
                    new Enrichment.GetProjectName());
        final Enricher enricher = builder.build();
        assertNotNull(enricher);
    }

    @Test
    public void assure_that_function_performs_same_transition() {
        assertTrue(SameTransition.asFor(fieldEnrichment).apply(fieldEnrichment));
    }

    @Test
    public void return_false_if_input_to_SameTransition_predicate_is_null() {
        assertFalse(SameTransition.asFor(fieldEnrichment).apply(null));
    }
}
