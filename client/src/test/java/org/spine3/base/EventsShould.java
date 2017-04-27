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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.command.EventFactory;
import org.spine3.string.Stringifiers;
import org.spine3.test.EventTests;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.test.TestEventFactory;
import org.spine3.test.Tests;
import org.spine3.time.Time;

import java.util.Comparator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Events.checkValid;
import static org.spine3.base.Events.getActor;
import static org.spine3.base.Events.getMessage;
import static org.spine3.base.Events.getProducer;
import static org.spine3.base.Events.getTimestamp;
import static org.spine3.base.Events.sort;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.protobuf.Wrappers.newBoolValue;
import static org.spine3.protobuf.Wrappers.newDoubleValue;
import static org.spine3.protobuf.Wrappers.newStringValue;
import static org.spine3.protobuf.Wrappers.pack;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.test.Tests.newUuidValue;
import static org.spine3.test.TimeTests.Past.minutesAgo;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public class EventsShould {

    private static final TestEventFactory eventFactory =
            TestEventFactory.newInstance(pack(EventsShould.class.getSimpleName()),
                                         EventsShould.class);
    private EventContext context;

    private final StringValue stringValue = newStringValue(newUuid());
    private final BoolValue boolValue = newBoolValue(true);
    @SuppressWarnings("MagicNumber")
    private final DoubleValue doubleValue = newDoubleValue(10.1);

    static EventContext newEventContext() {
        final Event event = eventFactory.createEvent(Time.getCurrentTime(),
                                                     Tests.<Version>nullRef());
        return event.getContext();
    }

    static Event createEventOccurredMinutesAgo(int minutesAgo) {
        final Event result = eventFactory.createEvent(newUuidValue(),
                                                      null,
                                                      minutesAgo(minutesAgo));
        return result;
    }

    private Event createEventWithContext(Message eventMessage) {
        return EventTests.createEvent(eventMessage, context);
    }

    @Before
    public void setUp() {
        final TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(getClass());
        final Command cmd = requestFactory.command().create(Time.getCurrentTime());
        final StringValue producerId = newStringValue(getClass().getSimpleName());
        EventFactory eventFactory = EventFactory.newBuilder()
                                                .setProducerId(producerId)
                                                .setCommandContext(cmd.getContext())
                                                .build();
        context = eventFactory.createEvent(Time.getCurrentTime(), Tests.<Version>nullRef())
                              .getContext();
    }

    @Test
    public void have_private_ctor() {
        assertHasPrivateParameterlessCtor(Events.class);
    }

    @Test
    public void return_actor_from_EventContext() {
        assertEquals(context.getCommandContext()
                            .getActorContext()
                            .getActor(), getActor(context));
    }

    @Test
    public void sort_events_by_time() {
        final Event event1 = createEventOccurredMinutesAgo(30);
        final Event event2 = createEventOccurredMinutesAgo(20);
        final Event event3 = createEventOccurredMinutesAgo(10);
        final List<Event> sortedEvents = newArrayList(event1, event2, event3);
        final List<Event> eventsToSort = newArrayList(event2, event1, event3);

        sort(eventsToSort);

        assertEquals(sortedEvents, eventsToSort);
    }

    @Test
    public void have_event_comparator() {
        final Event event1 = createEventOccurredMinutesAgo(120);
        final Event event2 = createEventOccurredMinutesAgo(2);

        final Comparator<Event> comparator = Events.eventComparator();
        assertTrue(comparator.compare(event1, event2) < 0);
        assertTrue(comparator.compare(event2, event1) > 0);
        assertTrue(comparator.compare(event1, event1) == 0);
    }

   @Test
    public void get_message_from_event() {
        createEventAndAssertReturnedMessageFor(stringValue);
        createEventAndAssertReturnedMessageFor(boolValue);
        createEventAndAssertReturnedMessageFor(doubleValue);
    }

    private static void createEventAndAssertReturnedMessageFor(Message msg) {
        final Event event = EventTests.createContextlessEvent(msg);

        assertEquals(msg, getMessage(event));
    }

    @Test
    public void get_timestamp_from_event() {
        final Event event = createEventWithContext(stringValue);

        assertEquals(context.getTimestamp(), getTimestamp(event));
    }

    @Test
    public void get_producer_from_event_context() {
        final StringValue msg = unpack(context.getProducerId());

        final String id = getProducer(context);

        assertEquals(msg.getValue(), id);
    }


    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(StringValue.class, StringValue.getDefaultInstance())
                .setDefault(EventContext.class, newEventContext())
                .testAllPublicStaticMethods(Events.class);
    }

    @Test
    public void provide_EventId_stringifier() {
        final EventId id = context.getEventId();
        
        final String str = Stringifiers.toString(id);
        final EventId convertedBack = Stringifiers.fromString(str, EventId.class);

        assertEquals(id, convertedBack);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_empty_event_id() {
        checkValid(EventId.getDefaultInstance());
    }

    @Test
    public void accept_generated_event_id() {
        checkValid(context.getEventId());
    }
}
