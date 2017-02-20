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
package org.spine3.base;

import com.google.common.base.Optional;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeName;
import org.spine3.test.EventTests;
import org.spine3.test.NullToleranceTest;

import java.util.Comparator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Events.createEvent;
import static org.spine3.base.Events.generateId;
import static org.spine3.base.Events.getActor;
import static org.spine3.base.Events.getMessage;
import static org.spine3.base.Events.getProducer;
import static org.spine3.base.Events.getTimestamp;
import static org.spine3.base.Events.isEnrichmentEnabled;
import static org.spine3.base.Events.sort;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.protobuf.Timestamps2.minutesAgo;
import static org.spine3.protobuf.Timestamps2.secondsAgo;
import static org.spine3.protobuf.Values.newBoolValue;
import static org.spine3.protobuf.Values.newDoubleValue;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.hasPrivateParameterlessCtor;
import static org.spine3.validate.Validate.isNotDefault;

@SuppressWarnings("InstanceMethodNamingConvention")
public class EventsShould {

    private final EventContext context = EventTests.newEventContext();

    private final StringValue stringValue = newStringValue(newUuid());
    private final BoolValue boolValue = newBoolValue(true);
    @SuppressWarnings("MagicNumber")
    private final DoubleValue doubleValue = newDoubleValue(10.1);

    @Test
    public void have_private_ctor() {
        assertTrue(hasPrivateParameterlessCtor(Events.class));
    }

    @Test
    public void generate_event_id() {
        final EventId result = generateId();

        assertFalse(result.getUuid()
                          .isEmpty());
    }

    @Test
    public void return_actor_from_EventContext() {
        assertEquals(context.getCommandContext()
                            .getActor(), getActor(context));
    }

    @Test
    public void sort_events_by_time() {
        final Event event1 = createEvent(stringValue, EventTests.newEventContext(minutesAgo(30)));
        final Event event2 = createEvent(boolValue, EventTests.newEventContext(minutesAgo(20)));
        final Event event3 = createEvent(doubleValue, EventTests.newEventContext(secondsAgo(10)));
        final List<Event> sortedEvents = newArrayList(event1, event2, event3);
        final List<Event> eventsToSort = newArrayList(event2, event1, event3);

        sort(eventsToSort);

        assertEquals(sortedEvents, eventsToSort);
    }

    @Test
    public void have_event_comparator() {
        final Event event1 = createEvent(stringValue, EventTests.newEventContext(minutesAgo(120)));
        final Event event2 = createEvent(boolValue, EventTests.newEventContext(minutesAgo(2)));

        final Comparator<Event> comparator = Events.eventComparator();
        assertTrue(comparator.compare(event1, event2) < 0);
        assertTrue(comparator.compare(event2, event1) > 0);
        assertTrue(comparator.compare(event1, event1) == 0);
    }

    @Test
    public void create_event() {
        createEventTest(stringValue);
        createEventTest(boolValue);
        createEventTest(doubleValue);
    }

    private void createEventTest(Message msg) {
        final Event event = createEvent(msg, context);

        assertEquals(msg, unpack(event.getMessage()));
        assertEquals(context, event.getContext());
    }

    @Test
    public void create_event_with_Any() {
        final Any msg = AnyPacker.pack(stringValue);
        final Event event = createEvent(msg, context);

        assertEquals(msg, event.getMessage());
        assertEquals(context, event.getContext());
    }

    @Test
    public void create_import_event() {
        final Event event = Events.createImportEvent(stringValue, doubleValue);

        assertEquals(stringValue, unpack(event.getMessage()));
        assertEquals(doubleValue, unpack(event.getContext()
                                              .getProducerId()));
    }

    @Test
    public void create_import_event_context() {
        final EventContext context = Events.createImportEventContext(doubleValue);

        assertEquals(doubleValue, unpack(context.getProducerId()));
        assertTrue(isNotDefault(context.getEventId()));
        assertTrue(isNotDefault(context.getTimestamp()));
    }

    @Test
    public void get_message_from_event() {
        createEventAndAssertReturnedMessageFor(stringValue);
        createEventAndAssertReturnedMessageFor(boolValue);
        createEventAndAssertReturnedMessageFor(doubleValue);
    }

    private void createEventAndAssertReturnedMessageFor(Message msg) {
        final Event event = createEvent(msg, context);

        assertEquals(msg, getMessage(event));
    }

    @Test
    public void get_timestamp_from_event() {
        final Event event = createEvent(stringValue, context);

        assertEquals(context.getTimestamp(), getTimestamp(event));
    }

    @Test
    public void get_producer_from_event_context() {
        final StringValue msg = unpack(context.getProducerId());

        final String id = getProducer(context);

        assertEquals(msg.getValue(), id);
    }

    @Test
    public void return_true_if_event_enrichment_is_enabled() {
        final Event event = createEvent(stringValue, context);

        assertTrue(isEnrichmentEnabled(event));
    }

    @Test
    public void return_false_if_event_enrichment_is_disabled() {
        final EventContext withDisabledEnrichment = context.toBuilder()
                                                           .setDoNotEnrich(true)
                                                           .build();
        final Event event = createEvent(stringValue, withDisabledEnrichment);

        assertFalse(isEnrichmentEnabled(event));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We're sure the optional is populated in this method.
    @Test
    public void return_all_event_enrichments() {
        final EventContext context = newEventContextWithEnrichment(TypeName.of(stringValue), stringValue);

        final Optional<Enrichments> enrichments = Events.getEnrichments(context);

        assertTrue(enrichments.isPresent());
        assertEquals(context.getEnrichments(), enrichments.get());
    }

    @Test
    public void return_optional_absent_if_no_event_enrichments() {
        assertFalse(Events.getEnrichments(context)
                          .isPresent());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We're sure the optional is populated in this method.
    @Test
    public void return_specific_event_enrichment() {
        final EventContext context = newEventContextWithEnrichment(TypeName.of(stringValue), stringValue);

        final Optional<? extends StringValue> enrichment = Events.getEnrichment(stringValue.getClass(), context);

        assertTrue(enrichment.isPresent());
        assertEquals(stringValue, enrichment.get());
    }

    @Test
    public void return_optional_absent_if_no_event_enrichments_when_getting_one() {
        assertFalse(Events.getEnrichment(StringValue.class, context)
                          .isPresent());
    }

    @Test
    public void return_optional_absent_if_no_needed_event_enrichment_when_getting_one() {
        final EventContext context = newEventContextWithEnrichment(TypeName.of(boolValue), boolValue);
        assertFalse(Events.getEnrichment(StringValue.class, context)
                          .isPresent());
    }

    @Test
    public void pass_the_null_tolerance_check() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(Events.class)
                                                                     .addDefaultValue(stringValue.getClass())
                                                                     .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }

    private static EventContext newEventContextWithEnrichment(String enrichmentKey, Message enrichment) {
        final Enrichments.Builder enrichments = Enrichments.newBuilder()
                                                           .putMap(enrichmentKey, AnyPacker.pack(enrichment));
        final EventContext context = EventTests.newEventContext().toBuilder()
                                               .setEnrichments(enrichments.build())
                                               .build();
        return context;
    }

}
