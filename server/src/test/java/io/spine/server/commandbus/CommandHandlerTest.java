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

package io.spine.server.commandbus;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.core.Command;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.commandbus.given.CommandHandlerTestEnv.EventCatcher;
import io.spine.server.commandbus.given.CommandHandlerTestEnv.TestCommandHandler;
import io.spine.server.event.EventBus;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;

@DisplayName("CommandHandler should")
class CommandHandlerTest {

    private static final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(CommandHandlerTest.class);

    private CommandBus commandBus;
    private EventBus eventBus;
    private TestCommandHandler handler;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        BoundedContext context = BoundedContextBuilder
                .assumingTests(true)
                .build();
        commandBus = context.commandBus();
        eventBus = context.eventBus();
        handler = new TestCommandHandler();
        context.internalAccess()
               .registerCommandDispatcher(handler);
    }

    @AfterEach
    void tearDown() throws Exception {
        commandBus.close();
        eventBus.close();
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(CommandEnvelope.class, generate())
                .testAllPublicInstanceMethods(handler);
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("one command")
        void command() {
            assertHandles(Given.ACommand.createProject());
        }

        @Test
        @DisplayName("several commands")
        void severalCommands() {
            assertHandles(Given.ACommand.createProject());
            assertHandles(Given.ACommand.addTask());
            assertHandles(Given.ACommand.startProject());
        }

        private void assertHandles(Command cmd) {
            handler.handle(cmd);
            handler.assertHandled(cmd);
        }
    }

    @Test
    @DisplayName("post generated events to EventBus")
    void postGeneratedEventsToBus() {
        Command cmd = Given.ACommand.startProject();

        EventCatcher eventCatcher = new EventCatcher();
        eventBus.register(eventCatcher);

        handler.handle(cmd);

        ImmutableList<? extends Message> expectedMessages = handler.getEventsOnStartProjectCmd();
        List<EventEnvelope> actualEvents = eventCatcher.dispatched();
        for (int i = 0; i < expectedMessages.size(); i++) {
            Message expected = expectedMessages.get(i);
            Message actual = actualEvents.get(i)
                                         .outerObject()
                                         .enclosedMessage();
            assertThat(actual)
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("post Pair of events")
    class PostPair {

        @Test
        @DisplayName("with two non-null values")
        void withBothValues() {
            Command cmd = Given.ACommand.createTask(true);

            EventCatcher eventCatcher = new EventCatcher();
            eventBus.register(eventCatcher);

            handler.handle(cmd);

            List<EventEnvelope> dispatchedEvents = eventCatcher.dispatched();
            assertThat(dispatchedEvents)
                    .hasSize(2);
        }

        @Test
        @DisplayName("with null second value")
        void withNullSecondValue() {
            Command cmd = Given.ACommand.createTask(false);

            EventCatcher eventCatcher = new EventCatcher();
            eventBus.register(eventCatcher);

            handler.handle(cmd);

            List<EventEnvelope> dispatchedEvents = eventCatcher.dispatched();
            assertThat(dispatchedEvents)
                    .hasSize(1);
        }
    }

    @Test
    @DisplayName("handle equality")
    void equality() {
        new EqualsTester().addEqualityGroup(handler, new TestCommandHandler())
                          .testEquals();
    }

    private static CommandEnvelope generate() {
        return CommandEnvelope.of(requestFactory.generateCommand());
    }
}
