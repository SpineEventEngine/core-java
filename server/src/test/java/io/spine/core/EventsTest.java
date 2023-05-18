/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.jupiter.api.Test;

import static io.spine.core.Events.nothing;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    EventsTest() {
        super(Events.class);
    }

    @BeforeEach
    void setUp() {
        CommandEnvelope cmd = generate();
        StringValue producerId = StringValue.of(getClass().getSimpleName());
        EventFactory eventFactory = EventFactory.on(cmd, Identifier.pack(producerId));
        event = eventFactory.createEvent(GivenEvent.message(), null);
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
}
