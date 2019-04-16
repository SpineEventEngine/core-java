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
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.server.entity.rejection.EntityAlreadyArchived;
import io.spine.server.event.EventFactory;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.given.GivenEvent;
import io.spine.string.Stringifiers;
import io.spine.testing.UtilityClassTest;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.core.Events.checkValid;
import static io.spine.core.Events.getActor;
import static io.spine.core.Events.getProducer;
import static io.spine.core.Events.nothing;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.type.given.EventsTestEnv.event;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private Event event;
    private EventContext context;

    EventsTest() {
        super(Events.class);
    }

    @BeforeEach
    void setUp() {
        CommandEnvelope cmd = generate();
        StringValue producerId = StringValue.of(getClass().getSimpleName());
        EventFactory eventFactory = EventFactory.on(cmd, Identifier.pack(producerId));
        event = eventFactory.createEvent(GivenEvent.message(), null);
        context = event.context();
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

    //TODO:2019-04-16:alexander.yevsyukov: Move to EventContextTest.
    // Throw IllegalStateException instead of IllegalArgumentException since it should be from an instance method.
    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Method called to throw exception.
    @Nested
    @DisplayName("throw IAE when reading tenant ID")
    class ThrowIaeOnRead {

        @Test
        @DisplayName("of the event without origin")
        void forEventWithoutOrigin() {
            EventContext context = contextWithoutOrigin().build();
            Event event = event(context);
            assertThrows(IllegalArgumentException.class, event::tenant);
        }

        @Test
        @DisplayName("of the event whose event context has no origin")
        void forEventContextWithoutOrigin() {
            EventContext context = contextWithoutOrigin()
                    .setEventContext(contextWithoutOrigin())
                    .build();
            Event event = event(context);
            assertThrows(IllegalArgumentException.class, event::tenant);
        }

        private EventContext.Builder contextWithoutOrigin() {
            return context.toBuilder()
                          .clearOrigin();
        }
    }
}
