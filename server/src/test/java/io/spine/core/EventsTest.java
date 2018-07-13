/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.given.EventsTestEnv;
import io.spine.core.given.GivenEvent;
import io.spine.server.event.EventFactory;
import io.spine.string.Stringifiers;
import io.spine.test.Tests;
import io.spine.type.TypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Identifier.newUuid;
import static io.spine.core.Events.checkValid;
import static io.spine.core.Events.getActor;
import static io.spine.core.Events.getMessage;
import static io.spine.core.Events.getProducer;
import static io.spine.core.Events.getTimestamp;
import static io.spine.core.Events.nothing;
import static io.spine.core.Events.sort;
import static io.spine.core.given.EventsTestEnv.tenantId;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.test.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test functionality of {@link Events} utility class.
 *
 * <p>This test suite is placed under the {@code server} module to avoid dependency on the event
 * generation code which belongs to server-side.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 * @author Mykhailo Drachuk
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
// Simple test names duplicate random literals.
@DisplayName("Events utility should")
public class EventsTest {

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(EventsTest.class);

    private Event event;
    private EventContext context;

    private final StringValue stringValue = toMessage(newUuid());
    private final BoolValue boolValue = toMessage(true);
    @SuppressWarnings("MagicNumber")
    private final DoubleValue doubleValue = toMessage(10.1);

    @BeforeEach
    void setUp() {
        TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(getClass());
        CommandEnvelope cmd = requestFactory.generateEnvelope();
        StringValue producerId = toMessage(getClass().getSimpleName());
        EventFactory eventFactory = EventFactory.on(cmd, Identifier.pack(producerId));
        event = eventFactory.createEvent(Time.getCurrentTime(),
                                         Tests.nullRef());
        context = event.getContext();
    }

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(Events.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(StringValue.class, StringValue.getDefaultInstance())
                .setDefault(EventContext.class, GivenEvent.context())
                .testAllPublicStaticMethods(Events.class);
    }

    @Nested
    @DisplayName("given event context, obtain")
    class GetFromContext {

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
            StringValue msg = unpack(context.getProducerId());

            String id = getProducer(context);

            assertEquals(msg.getValue(), id);
        }
    }

    @Nested
    @DisplayName("given event, obtain")
    class GetFromEvent {

        @Test
        @DisplayName("message")
        void message() {
            createEventAndAssertReturnedMessageFor(stringValue);
            createEventAndAssertReturnedMessageFor(boolValue);
            createEventAndAssertReturnedMessageFor(doubleValue);
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
            CommandEnvelope command = requestFactory.generateEnvelope();
            StringValue producerId = toMessage(getClass().getSimpleName());
            EventFactory ef = EventFactory.on(command, Identifier.pack(producerId));
            Event event = ef.createEvent(Time.getCurrentTime(), Tests.nullRef());

            assertEquals(command.getId(), Events.getRootCommandId(event));
        }

        @Test
        @DisplayName("type name")
        void typeName() {
            CommandEnvelope command = requestFactory.generateEnvelope();
            StringValue producerId = toMessage(getClass().getSimpleName());
            EventFactory ef = EventFactory.on(command, Identifier.pack(producerId));
            Event event = ef.createEvent(Time.getCurrentTime(), Tests.nullRef());

            TypeName typeName = EventEnvelope.of(event)
                                                   .getTypeName();
            assertNotNull(typeName);
            assertEquals(Timestamp.class.getSimpleName(), typeName.getSimpleName());
        }

        private void createEventAndAssertReturnedMessageFor(Message msg) {
            Event event = GivenEvent.withMessage(msg);

            assertEquals(msg, getMessage(event));
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

    @Nested
    @DisplayName("return default tenant ID")
    class ReturnDefaultTenantId {

        @Test
        @DisplayName("for event without origin")
        void forNoOrigin() {
            EventContext context = contextWithoutOrigin().build();
            Event event = EventsTestEnv.event(context);

            TenantId tenantId = Events.getTenantId(event);

            TenantId defaultTenantId = TenantId.getDefaultInstance();
            assertEquals(defaultTenantId, tenantId);
        }

        @Test
        @DisplayName("for event with rejection context without command")
        void forRejectionContextWithoutCommand() {
            RejectionContext rejectionContext = EventsTestEnv.rejectionContext();
            EventContext context = contextWithoutOrigin().setRejectionContext(
                    rejectionContext)
                                                               .build();
            Event event = EventsTestEnv.event(context);

            TenantId tenantId = Events.getTenantId(event);

            TenantId defaultTenantId = TenantId.getDefaultInstance();
            assertEquals(defaultTenantId, tenantId);
        }

        @Test
        @DisplayName("for event with event context without origin")
        void forEventContextWithoutOrigin() {
            EventContext context = contextWithoutOrigin().setEventContext(
                    contextWithoutOrigin())
                                                               .build();
            Event event = EventsTestEnv.event(context);

            TenantId tenantId = Events.getTenantId(event);

            TenantId defaultTenantId = TenantId.getDefaultInstance();
            assertEquals(defaultTenantId, tenantId);
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
        @DisplayName("from event with rejection context")
        void fromRejectionContext() {
            TenantId targetTenantId = tenantId();
            RejectionContext rejectionContext = EventsTestEnv.rejectionContext(
                    targetTenantId);
            EventContext context = contextWithoutOrigin().setRejectionContext(
                    rejectionContext)
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

        @Test
        @DisplayName("from event with event context originated from rejection context")
        void fromEventContextWithRejectionContext() {
            TenantId targetTenantId = tenantId();
            RejectionContext rejectionContext = EventsTestEnv.rejectionContext(
                    targetTenantId);
            EventContext outerContext =
                    contextWithoutOrigin().setRejectionContext(rejectionContext)
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

    private EventContext.Builder contextWithoutOrigin() {
        return context.toBuilder()
                      .clearOrigin();
    }
}
