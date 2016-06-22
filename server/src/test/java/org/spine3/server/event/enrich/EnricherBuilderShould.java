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
import org.spine3.test.Tests;

import javax.annotation.Nullable;
import java.util.Set;

import static org.junit.Assert.*;

@SuppressWarnings({"InstanceMethodNamingConvention", "ResultOfMethodCallIgnored"})
public class EnricherBuilderShould {

    private EventEnricher.Builder builder;
    private Function<Timestamp, StringValue> function;
    private FieldEnricher<Timestamp, StringValue> fieldEnricherFunction;

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
        this.fieldEnricherFunction = FieldEnricher.newInstance(Timestamp.class, StringValue.class, function);
    }

    @Test
    public void create_new_instance() {
        assertNotNull(EventEnricher.Builder.newInstance());
    }

    @Test
    public void build_enricher() {
        assertNotNull(builder.build());
    }

    @Test
    public void add_event_enrichment() {
        builder.addEventEnrichment(Timestamp.class, StringValue.class);

        final Set<EnrichmentFunction<?, ?>> functions = builder.getFunctions();
        assertEquals(1, functions.size());
    }

    @Test
    public void add_field_enrichment() {
        builder.addFieldEnrichment(Timestamp.class, StringValue.class, fieldEnricherFunction.getFunction());

        assertTrue(builder.getFunctions().contains(fieldEnricherFunction));
    }

    @Test
    public void remove_enrichment_function() {
        builder.addFieldEnrichment(Timestamp.class, StringValue.class, fieldEnricherFunction.getFunction());

        builder.remove(fieldEnricherFunction);

        assertTrue(builder.getFunctions().isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_source_class_for_event_enrichment() {
        builder.addEventEnrichment(Tests.<Class<Timestamp>>nullRef(), StringValue.class);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_target_class_for_event_enrichment() {
        builder.addEventEnrichment(Timestamp.class, Tests.<Class<StringValue>>nullRef());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_duplicates_for_event_enrichment() {
        builder.addEventEnrichment(Timestamp.class, StringValue.class);
        // This should fail.
        builder.addEventEnrichment(Timestamp.class, StringValue.class);
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
}
