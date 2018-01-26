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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.Identifier;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.given.GivenEvent;
import io.spine.server.event.EventFactory;
import io.spine.string.Stringifiers;
import io.spine.test.Tests;
import io.spine.time.Time;
import io.spine.type.TypeName;
import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.Identifier.newUuid;
import static io.spine.core.Events.checkValid;
import static io.spine.core.Events.getActor;
import static io.spine.core.Events.getMessage;
import static io.spine.core.Events.getProducer;
import static io.spine.core.Events.getTimestamp;
import static io.spine.core.Events.nothing;
import static io.spine.core.Events.sort;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test functionality of {@link Events} utility class.
 *
 * <p>This test suite is placed under the {@code server} module to avoid dependency on the event
 * generation code which belongs to server-side.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public class EventsShould {

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(EventsShould.class);

    private Event event;
    private EventContext context;

    private final StringValue stringValue = toMessage(newUuid());
    private final BoolValue boolValue = toMessage(true);
    @SuppressWarnings("MagicNumber")
    private final DoubleValue doubleValue = toMessage(10.1);

    @Before
    public void setUp() {
        final TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(getClass());
        final CommandEnvelope cmd = requestFactory.generateEnvelope();
        final StringValue producerId = toMessage(getClass().getSimpleName());
        EventFactory eventFactory = EventFactory.on(cmd, Identifier.pack(producerId));
        event = eventFactory.createEvent(Time.getCurrentTime(),
                                                     Tests.<Version>nullRef());
        context = event.getContext();
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
        final Event event1 = GivenEvent.occurredMinutesAgo(30);
        final Event event2 = GivenEvent.occurredMinutesAgo(20);
        final Event event3 = GivenEvent.occurredMinutesAgo(10);
        final List<Event> sortedEvents = newArrayList(event1, event2, event3);
        final List<Event> eventsToSort = newArrayList(event2, event1, event3);

        sort(eventsToSort);

        assertEquals(sortedEvents, eventsToSort);
    }

    @Test
    public void have_event_comparator() {
        final Event event1 = GivenEvent.occurredMinutesAgo(120);
        final Event event2 = GivenEvent.occurredMinutesAgo(2);

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
        final Event event = GivenEvent.withMessage(msg);

        assertEquals(msg, getMessage(event));
    }

    @Test
    public void get_timestamp_from_event() {
        final Event event = GivenEvent.occurredMinutesAgo(1);

        assertEquals(event.getContext().getTimestamp(), getTimestamp(event));
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
                .setDefault(EventContext.class, GivenEvent.context())
                .testAllPublicStaticMethods(Events.class);
    }

    @Test
    public void provide_EventId_stringifier() {
        final EventId id = event.getId();
        
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
        final EventId eventId = event.getId();
        assertEquals(eventId, checkValid(eventId));
    }

    @Test
    public void obtain_type_name_of_event() {
        final CommandEnvelope command = requestFactory.generateEnvelope();
        final StringValue producerId = toMessage(getClass().getSimpleName());
        final EventFactory ef = EventFactory.on(command, Identifier.pack(producerId));
        final Event event = ef.createEvent(Time.getCurrentTime(), Tests.<Version>nullRef());

        final TypeName typeName = EventEnvelope.of(event)
                                               .getTypeName();
        assertNotNull(typeName);
        assertEquals(Timestamp.class.getSimpleName(), typeName.getSimpleName());
    }

    @Test
    public void obtain_root_command_id() {
        final CommandEnvelope command = requestFactory.generateEnvelope();
        final StringValue producerId = toMessage(getClass().getSimpleName());
        final EventFactory ef = EventFactory.on(command, Identifier.pack(producerId));
        final Event event = ef.createEvent(Time.getCurrentTime(), Tests.<Version>nullRef());
        
        assertEquals(command.getId(), Events.getRootCommandId(event));
    }

    @Test
    public void provide_empty_Iterable() {
        for (Object ignored : nothing()) {
            fail("Something found in nothing().");
        }
    }
}
