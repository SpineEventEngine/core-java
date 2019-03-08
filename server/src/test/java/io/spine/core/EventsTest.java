/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.base.RejectionMessage;
import io.spine.base.ThrowableMessage;
import io.spine.base.Time;
import io.spine.server.entity.rejection.EntityAlreadyArchived;
import io.spine.server.entity.rejection.StandardRejections;
import io.spine.server.event.EventFactory;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.given.EventsTestEnv;
import io.spine.server.type.given.GivenEvent;
import io.spine.string.Stringifiers;
import io.spine.test.core.given.EtProjectCreated;
import io.spine.testing.Tests;
import io.spine.testing.UtilityClassTest;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.type.TypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.core.Events.checkValid;
import static io.spine.core.Events.getActor;
import static io.spine.core.Events.getMessage;
import static io.spine.core.Events.getProducer;
import static io.spine.core.Events.getTimestamp;
import static io.spine.core.Events.nothing;
import static io.spine.core.Events.sort;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.type.given.EventsTestEnv.tenantId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test functionality of {@link Events} utility class.
 *
 * <p>This test suite is placed under the {@code server} module to avoid dependency on the event
 * generation code which belongs to server-side.
 */
@DisplayName("Events utility should")
public class EventsTest extends UtilityClassTest<Events> {

    private static final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(EventsTest.class);

    private EventFactory eventFactory;

    private Event event;
    private EventContext context;

    EventsTest() {
        super(Events.class);
    }

    @BeforeEach
    void setUp() {
        CommandEnvelope cmd = generate();
        StringValue producerId = StringValue.of(getClass().getSimpleName());
        eventFactory = EventFactory.on(cmd, Identifier.pack(producerId));
        event = eventFactory.createEvent(GivenEvent.message(), null);
        context = event.getContext();
    }

    private static CommandEnvelope generate() {
        return CommandEnvelope.of(requestFactory.generateCommand());
    }

    @Override
    protected void configure(NullPointerTester tester) {
        super.configure(tester);
        EntityAlreadyArchived defaultThrowableMessage = EntityAlreadyArchived
                .newBuilder()
                .setEntityId(Any.getDefaultInstance())
                .build();
        tester.setDefault(StringValue.class, StringValue.getDefaultInstance())
              .setDefault(EventContext.class, GivenEvent.context())
              .setDefault(Version.class, Version.getDefaultInstance())
              .setDefault(Event.class, Event.getDefaultInstance())
              .setDefault(ThrowableMessage.class, defaultThrowableMessage);
    }

    @Nested
    @DisplayName("given event context, obtain")
    class GetFromEventContext {

        @Test
        @DisplayName("actor")
        void actor() {
            assertEquals(context.getCommandContext()
                                .getActorContext()
                                .getActor(), getActor(context));
        }

        @Test
        @DisplayName("producer")
        void producer() {
            StringValue msg = unpack(context.getProducerId(), StringValue.class);
            String id = (String) getProducer(context);
            assertEquals(msg.getValue(), id);
        }
    }

    @Nested
    @DisplayName("given event, obtain")
    class GetFromEvent {

        @Test
        @DisplayName("message")
        void message() {
            EventMessage message = GivenEvent.message();
            Event event = GivenEvent.withMessage(message);
            assertEquals(message, getMessage(event));
        }

        @Test
        @DisplayName("timestamp")
        void timestamp() {
            Event event = GivenEvent.occurredMinutesAgo(1);

            assertEquals(event.getContext()
                              .getTimestamp(), getTimestamp(event));
        }

        @Test
        @DisplayName("root command ID")
        void rootCommandId() {
            CommandEnvelope command = generate();
            Event event = newEvent(command);

            assertEquals(command.id(), Events.getRootCommandId(event));
        }

        @Test
        @DisplayName("type name")
        void typeName() {
            CommandEnvelope command = generate();
            Event event = newEvent(command);

            TypeName typeName = EventEnvelope.of(event)
                                             .messageTypeName();
            assertNotNull(typeName);
            assertEquals(EtProjectCreated.class.getSimpleName(), typeName.simpleName());
        }

        private Event newEvent(CommandEnvelope command) {
            StringValue producerId = StringValue.of(getClass().getSimpleName());
            EventFactory ef = EventFactory.on(command, Identifier.pack(producerId));
            return ef.createEvent(GivenEvent.message(), null);
        }
    }

    @Test
    @DisplayName("sort given events by timestamp")
    void sortEventsByTime() {
        Event event1 = GivenEvent.occurredMinutesAgo(30);
        Event event2 = GivenEvent.occurredMinutesAgo(20);
        Event event3 = GivenEvent.occurredMinutesAgo(10);
        List<Event> sortedEvents = newArrayList(event1, event2, event3);
        List<Event> eventsToSort = newArrayList(event2, event1, event3);

        sort(eventsToSort);

        assertEquals(sortedEvents, eventsToSort);
    }

    @Test
    @DisplayName("provide event comparator")
    void provideEventComparator() {
        Event event1 = GivenEvent.occurredMinutesAgo(120);
        Event event2 = GivenEvent.occurredMinutesAgo(2);

        Comparator<Event> comparator = Events.eventComparator();
        assertTrue(comparator.compare(event1, event2) < 0);
        assertTrue(comparator.compare(event2, event1) > 0);
        assertEquals(0, comparator.compare(event1, event1));
    }

    @Test
    @DisplayName("provide stringifier for event ID")
    void provideEventIdStringifier() {
        EventId id = event.getId();

        String str = Stringifiers.toString(id);
        EventId convertedBack = Stringifiers.fromString(str, EventId.class);

        assertEquals(id, convertedBack);
    }

    @Test
    @DisplayName("provide empty Iterable")
    void provideEmptyIterable() {
        for (Object ignored : nothing()) {
            fail("Something found in nothing().");
        }
    }

    @Test
    @DisplayName("reject empty event ID")
    void rejectEmptyEventId() {
        assertThrows(IllegalArgumentException.class,
                     () -> checkValid(EventId.getDefaultInstance()));
    }

    @Test
    @DisplayName("accept generated event ID")
    void acceptGeneratedEventId() {
        EventId eventId = event.getId();
        assertEquals(eventId, checkValid(eventId));
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Method called to throw exception.
    @Nested
    @DisplayName("throw IAE when reading tenant ID")
    class ThrowIaeOnRead {

        @Test
        @DisplayName("of the event without origin")
        void forEventWithoutOrigin() {
            EventContext context = contextWithoutOrigin().build();
            Event event = EventsTestEnv.event(context);
            assertThrows(IllegalArgumentException.class, () -> Events.getTenantId(event));
        }

        @Test
        @DisplayName("of the event whose event context has no origin")
        void forEventContextWithoutOrigin() {
            EventContext context = contextWithoutOrigin()
                    .setEventContext(contextWithoutOrigin())
                    .build();
            Event event = EventsTestEnv.event(context);
            assertThrows(IllegalArgumentException.class, () -> Events.getTenantId(event));
        }
    }

    @Nested
    @DisplayName("retrieve tenant ID")
    class GetTenantId {

        @Test
        @DisplayName("from event with command context")
        void fromCommandContext() {
            TenantId targetTenantId = tenantId();
            CommandContext commandContext = EventsTestEnv.commandContext(targetTenantId);
            EventContext context = contextWithoutOrigin().setCommandContext(commandContext)
                                                         .build();
            Event event = EventsTestEnv.event(context);

            TenantId tenantId = Events.getTenantId(event);

            assertEquals(targetTenantId, tenantId);

        }

        @Test
        @DisplayName("from event with event context originated from command context")
        void fromEventContextWithCommandContext() {
            TenantId targetTenantId = tenantId();
            CommandContext commandContext = EventsTestEnv.commandContext(targetTenantId);
            EventContext outerContext = contextWithoutOrigin().setCommandContext(
                    commandContext)
                                                              .build();
            EventContext context = contextWithoutOrigin().setEventContext(outerContext)
                                                         .build();
            Event event = EventsTestEnv.event(context);

            TenantId tenantId = Events.getTenantId(event);

            assertEquals(targetTenantId, tenantId);
        }
    }

    @Test
    @DisplayName("throw NullPointerException when getting tenant ID of null event")
    void notAcceptNullEvent() {
        assertThrows(NullPointerException.class, () -> Events.getTenantId(Tests.nullRef()));
    }

    @Test
    @DisplayName("tell if an Event is a rejection event")
    void tellWhenRejection() {
        RejectionEventContext rejectionContext = RejectionEventContext
                .newBuilder()
                .setStacktrace("at package.name.Class.method(Class.java:42)")
                .build();
        RejectionMessage message = StandardRejections.EntityAlreadyArchived
                .newBuilder()
                .setEntityId(pack(Time.currentTime()))
                .build();
        Event event =
                eventFactory.createRejectionEvent(message, null, rejectionContext);
        assertTrue(Events.isRejection(event));
    }

    @Test
    @DisplayName("tell if an Event is NOT a rejection event")
    void tellWhenNotRejection() {
        assertFalse(Events.isRejection(event));
    }

    private EventContext.Builder contextWithoutOrigin() {
        return context.toBuilder()
                      .clearOrigin();
    }
}
