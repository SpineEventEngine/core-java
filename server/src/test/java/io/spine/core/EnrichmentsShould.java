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

package io.spine.core;

import com.google.common.base.Optional;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.given.GivenEvent;
import io.spine.server.command.TestEventFactory;
import io.spine.time.Time;
import io.spine.type.TypeName;
import org.junit.Before;
import org.junit.Test;

import static io.spine.Identifier.newUuid;
import static io.spine.core.Enrichments.getEnrichment;
import static io.spine.core.Enrichments.getEnrichments;
import static io.spine.core.given.GivenEvent.context;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class EnrichmentsShould {

    private static final StringValue producerId =
            toMessage(EnrichmentsShould.class.getSimpleName());
    private final StringValue stringValue = toMessage(newUuid());
    private final BoolValue boolValue = toMessage(true);
    private TestEventFactory eventFactory;
    private EventContext context;

    /**
     * Creates a new {@link EventContext} enriched with the passed message.
     *
     * <p>The key in the map is a fully-qualified {@code TypeName} of the message.
     * See {@link Enrichment.Container#getItemsMap()} or {@code Enrichment} proto type definition
     * for details.
     */
    private static EventContext givenContextEnrichedWith(Message enrichment) {
        final String enrichmentKey = TypeName.of(enrichment)
                                             .value();
        final Enrichment.Builder enrichments =
                Enrichment.newBuilder()
                          .setContainer(Enrichment.Container.newBuilder()
                                                            .putItems(enrichmentKey,
                                                                      pack(enrichment)));
        final EventContext context = context().toBuilder()
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
                .setDefault(EventContext.class, context())
                .setDefault(RejectionContext.class, RejectionContext.getDefaultInstance())
                .testAllPublicStaticMethods(Enrichments.class);
    }

    @Test
    public void have_utility_ctor() {
        assertHasPrivateParameterlessCtor(Enrichments.class);
    }

    @Test
    public void return_true_if_event_enrichment_is_enabled() {
        final EventEnvelope event = EventEnvelope.of(eventFactory.createEvent(stringValue));

        assertTrue(event.isEnrichmentEnabled());
    }

    @Test
    public void return_false_if_event_enrichment_is_disabled() {
        final EventEnvelope event = EventEnvelope.of(
                GivenEvent.withDisabledEnrichmentOf(stringValue)
        );

        assertFalse(event.isEnrichmentEnabled());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    // We're sure the optional is populated in this method.
    @Test
    public void return_all_event_enrichments() {
        final EventContext context = givenContextEnrichedWith(stringValue);

        final Optional<Enrichment.Container> enrichments = getEnrichments(context);

        assertTrue(enrichments.isPresent());
        assertEquals(context.getEnrichment()
                            .getContainer(), enrichments.get());
    }

    @Test
    public void return_optional_absent_if_no_event_enrichments() {
        assertFalse(getEnrichments(context).isPresent());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    // We're sure the optional is populated in this method.
    @Test
    public void return_specific_event_enrichment() {
        final EventContext context = givenContextEnrichedWith(stringValue);

        final Optional<? extends StringValue> enrichment =
                getEnrichment(stringValue.getClass(), context);

        assertTrue(enrichment.isPresent());
        assertEquals(stringValue, enrichment.get());
    }

    @Test
    public void return_optional_absent_if_no_event_enrichments_when_getting_one() {
        assertFalse(getEnrichment(StringValue.class, context).isPresent());
    }

    @Test
    public void return_optional_absent_if_no_needed_event_enrichment_when_getting_one() {
        final EventContext context = givenContextEnrichedWith(boolValue);
        assertFalse(getEnrichment(StringValue.class, context).isPresent());
    }
}
