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
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.server.BoundedContext;
import io.spine.server.event.EventBus;
import io.spine.server.event.given.CommandHandlerTestEnv.EventCatcher;
import io.spine.server.event.given.CommandHandlerTestEnv.TestCommandHandler;
import io.spine.server.model.ModelTests;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.event.EventRecodingLogger;
import org.slf4j.event.Level;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.SubstituteLogger;

import java.util.List;
import java.util.Queue;

import static io.spine.base.Identifier.newUuid;
import static io.spine.test.Tests.nullRef;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 */
public class CommandHandlerShould {

    private CommandBus commandBus;
    private EventBus eventBus;
    private TestCommandHandler handler;

    @Before
    public void setUp() {
        ModelTests.clearModel();
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .setMultitenant(true)
                                                            .build();
        commandBus = boundedContext.getCommandBus();
        eventBus = boundedContext.getEventBus();
        handler = new TestCommandHandler(eventBus);

        commandBus.register(handler);
    }

    @After
    public void tearDown() throws Exception {
        commandBus.close();
        eventBus.close();
    }

    @Test
    public void handle_command() {
        assertHandles(Given.ACommand.createProject());
    }

    @Test
    public void handle_several_commands() {
        assertHandles(Given.ACommand.createProject());
        assertHandles(Given.ACommand.addTask());
        assertHandles(Given.ACommand.startProject());
    }

    @Test
    public void post_generated_events_to_event_bus() {
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

    @Test
    public void generate_non_zero_hash_code_if_handler_has_non_empty_id() {
        final int hashCode = handler.hashCode();

        assertTrue(hashCode != 0);
    }

    @Test
    public void generate_same_hash_code_for_same_instances() {
        assertEquals(handler.hashCode(), handler.hashCode());
    }

    @Test
    public void assure_same_handlers_are_equal() {
        final TestCommandHandler same = new TestCommandHandler(eventBus);

        assertTrue(handler.equals(same));
    }

    @SuppressWarnings("EqualsWithItself") // is the goal of the test
    @Test
    public void assure_handler_is_equal_to_itself() {
        assertTrue(handler.equals(handler));
    }

    @Test
    public void assure_handler_is_not_equal_to_null() {
        assertFalse(handler.equals(nullRef()));
    }

    @SuppressWarnings("EqualsBetweenInconvertibleTypes") // is the goal of the test
    @Test
    public void assure_handler_is_not_equal_to_object_of_another_class() {
        assertFalse(handler.equals(newUuid()));
    }

    private void assertHandles(Command cmd) {
        handler.handle(cmd);
        handler.assertHandled(cmd);
    }

    @Test
    public void have_class_specific_logger() {
        final Logger logger = handler.log();
        assertNotNull(logger);
        assertEquals(logger.getName(), handler.getClass()
                                              .getName());
    }

    @Test
    public void log_errors() {
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

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(CommandEnvelope.class, givenCommandEnvelope())
                .testAllPublicInstanceMethods(handler);
    }
}
