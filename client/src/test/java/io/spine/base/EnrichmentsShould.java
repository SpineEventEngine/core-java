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

package io.spine.base;

import com.google.common.base.Optional;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.protobuf.Wrapper;
import io.spine.test.EventTests;
import io.spine.test.TestEventFactory;
import io.spine.time.Time;
import io.spine.type.TypeName;
import org.junit.Before;
import org.junit.Test;

import static io.spine.base.Enrichments.isEnrichmentEnabled;
import static io.spine.base.EventsShould.newEventContext;
import static io.spine.base.Identifiers.newUuid;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.Wrapper.forBoolean;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class EnrichmentsShould {

    private static final StringValue producerId = Wrapper.forString(
            EnrichmentsShould.class.getSimpleName());

    private TestEventFactory eventFactory;
    private EventContext context;

    private final StringValue stringValue = Wrapper.forString(newUuid());
    private final BoolValue boolValue = forBoolean(true);

    private static EventContext newEventContextWithEnrichment(String enrichmentKey,
            Message enrichment) {
        final Enrichment.Builder enrichments =
                Enrichment.newBuilder()
                          .setContainer(Enrichment.Container.newBuilder()
                                                            .putItems(enrichmentKey,
                                                                      pack(enrichment)));
        final EventContext context = newEventContext()
                .toBuilder()
                .setEnrichment(enrichments.build())
                .build();
        return context;
    }

    @Before
    public void setUp() {
        eventFactory = TestEventFactory.newInstance(pack(producerId), getClass());
        context = eventFactory.createEvent(Time.getCurrentTime())
                              .getContext();
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(StringValue.class, StringValue.getDefaultInstance())
                .setDefault(EventContext.class, newEventContext())
                .testAllPublicStaticMethods(Enrichments.class);
    }

    @Test
    public void have_utility_ctor() {
        assertHasPrivateParameterlessCtor(Enrichments.class);
    }

    @Test
    public void return_true_if_event_enrichment_is_enabled() {
        final Event event = eventFactory.createEvent(stringValue);

        assertTrue(isEnrichmentEnabled(event));
    }

    @Test
    public void return_false_if_event_enrichment_is_disabled() {
        final EventContext withDisabledEnrichment =
                context.toBuilder()
                       .setEnrichment(Enrichment.newBuilder()
                                                .setDoNotEnrich(true))
                       .build();
        final Event event = EventTests.createEvent(stringValue, withDisabledEnrichment);

        assertFalse(isEnrichmentEnabled(event));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    // We're sure the optional is populated in this method.
    @Test
    public void return_all_event_enrichments() {
        final EventContext context =
                newEventContextWithEnrichment(TypeName.of(stringValue)
                                                      .value(), stringValue);

        final Optional<Enrichment.Container> enrichments = Enrichments.getEnrichments(context);

        assertTrue(enrichments.isPresent());
        assertEquals(context.getEnrichment()
                            .getContainer(), enrichments.get());
    }

    @Test
    public void return_optional_absent_if_no_event_enrichments() {
        assertFalse(Enrichments.getEnrichments(context)
                               .isPresent());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    // We're sure the optional is populated in this method.
    @Test
    public void return_specific_event_enrichment() {
        final EventContext context = newEventContextWithEnrichment(
                TypeName.of(stringValue).value(),
                stringValue);

        final Optional<? extends StringValue> enrichment =
                Enrichments.getEnrichment(stringValue.getClass(), context);

        assertTrue(enrichment.isPresent());
        assertEquals(stringValue, enrichment.get());
    }

    @Test
    public void return_optional_absent_if_no_event_enrichments_when_getting_one() {
        assertFalse(Enrichments.getEnrichment(StringValue.class, context)
                               .isPresent());
    }

    @Test
    public void return_optional_absent_if_no_needed_event_enrichment_when_getting_one() {
        final EventContext context = newEventContextWithEnrichment(
                TypeName.of(boolValue).value(),
                boolValue);
        assertFalse(Enrichments.getEnrichment(StringValue.class, context)
                               .isPresent());
    }
}
