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

package io.spine.server.commandbus;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.server.BoundedContext;
import io.spine.server.event.EventBus;
import io.spine.server.event.given.CommandHandlerTestEnv.EventCatcher;
import io.spine.server.event.given.CommandHandlerTestEnv.TestCommandHandler;
import io.spine.server.model.ModelTests;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.event.EventRecodingLogger;
import org.slf4j.event.Level;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.SubstituteLogger;

import java.util.List;
import java.util.Queue;

import static io.spine.base.Identifier.newUuid;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.test.Tests.nullRef;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("CommandHandler should")
class CommandHandlerTest {

    private CommandBus commandBus;
    private EventBus eventBus;
    private TestCommandHandler handler;

    @BeforeEach
    void setUp() {
        ModelTests.clearModel();
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .setMultitenant(true)
                                                            .build();
        commandBus = boundedContext.getCommandBus();
        eventBus = boundedContext.getEventBus();
        handler = new TestCommandHandler(eventBus);

        commandBus.register(handler);
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
                .setDefault(CommandEnvelope.class, givenCommandEnvelope())
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
        final Command cmd = Given.ACommand.startProject();

        final EventCatcher eventCatcher = new EventCatcher();
        eventBus.register(eventCatcher);

        handler.handle(cmd);

        final ImmutableList<Message> expectedMessages = handler.getEventsOnStartProjectCmd();
        final List<EventEnvelope> actualEvents = eventCatcher.getDispatched();
        for (int i = 0; i < expectedMessages.size(); i++) {
            final Message expected = expectedMessages.get(i);
            final Message actual = Events.getMessage(actualEvents.get(i).getOuterObject());
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("provide `hashCode` method such that")
    class ProvideHashCode {

        @Test
        @DisplayName("for non-empty handler ID non-zero hashcode is generated")
        void nonZeroForNonEmptyId() {
            final int hashCode = handler.hashCode();

            assertTrue(hashCode != 0);
        }

        @Test
        @DisplayName("for same handler instances same hashcode is generated")
        void sameForSameInstances() {
            assertEquals(handler.hashCode(), handler.hashCode());
        }
    }

    @Nested
    @DisplayName("provide `equals` method such that")
    class ProvideEqualsSuchThat {

        @Test
        @DisplayName("same handlers are equal")
        void equalsToSame() {
            final TestCommandHandler same = new TestCommandHandler(eventBus);

            assertTrue(handler.equals(same));
        }

        @SuppressWarnings("EqualsWithItself") // is the goal of the test
        @Test
        @DisplayName("handler is equal to itself")
        void equalsToSelf() {
            assertTrue(handler.equals(handler));
        }

        @Test
        @DisplayName("handler is not equal to null")
        void notEqualsToNull() {
            assertFalse(handler.equals(nullRef()));
        }

        @SuppressWarnings("EqualsBetweenInconvertibleTypes") // is the goal of the test
        @Test
        @DisplayName("handler is not equal to object of another class")
        void notEqualsToOtherClass() {
            assertFalse(handler.equals(newUuid()));
        }
    }

    @Test
    @DisplayName("have class-specific logger")
    void haveClassSpecificLogger() {
        final Logger logger = handler.log();
        assertNotNull(logger);
        assertEquals(logger.getName(), handler.getClass()
                                              .getName());
    }

    @Test
    @DisplayName("log errors")
    void logErrors() {
        final CommandEnvelope commandEnvelope = givenCommandEnvelope();

        // Since we're in the tests mode `Environment` returns `SubstituteLogger` instance.
        final SubstituteLogger log = (SubstituteLogger) handler.log();

        // Restrict the queue size only to the number of calls we want to make.
        final Queue<SubstituteLoggingEvent> queue = Queues.newArrayBlockingQueue(1);
        log.setDelegate(new EventRecodingLogger(log, queue));

        SubstituteLoggingEvent loggingEvent;

        final RuntimeException exception = new RuntimeException("log_errors");
        handler.onError(commandEnvelope, exception);

        loggingEvent = queue.poll();

        assertEquals(Level.ERROR, loggingEvent.getLevel());
        assertEquals(commandEnvelope, handler.getLastErrorEnvelope());
        assertEquals(exception, handler.getLastException());
    }

    private CommandEnvelope givenCommandEnvelope() {
        return TestActorRequestFactory.newInstance(getClass()).generateEnvelope();
    }
}
