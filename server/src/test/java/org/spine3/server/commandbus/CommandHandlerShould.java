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

package org.spine3.server.commandbus;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.Events;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.Assign;
import org.spine3.server.command.CommandHandler;
import org.spine3.server.command.CommandHistory;
import org.spine3.server.event.EventBus;
import org.spine3.test.command.AddTask;
import org.spine3.test.command.CreateProject;
import org.spine3.test.command.StartProject;
import org.spine3.test.command.event.ProjectCreated;
import org.spine3.test.command.event.ProjectStarted;
import org.spine3.test.command.event.TaskAdded;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.base.Identifier.newUuid;
import static org.spine3.test.Tests.nullRef;

/**
 * @author Alexander Litus
 */
public class CommandHandlerShould {

    private CommandBus commandBus;
    private EventBus eventBus;
    private TestCommandHandler handler;

    @Before
    public void setUp() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .setMultitenant(true)
                                                            .build();
        commandBus = boundedContext.getCommandBus();
        eventBus = spy(boundedContext.getEventBus());
        handler = new TestCommandHandler();

        commandBus.register(handler);
    }

    @After
    public void tearDown() throws Exception {
        commandBus.close();
        eventBus.close();
    }

    @Test
    public void handle_command() {
        assertHandles(Given.Command.createProject());
    }

    @Test
    public void handle_several_commands() {
        assertHandles(Given.Command.createProject());
        assertHandles(Given.Command.addTask());
        assertHandles(Given.Command.startProject());
    }

    @Test
    public void post_generated_events_to_event_bus() {
        final Command cmd = Given.Command.startProject();

        handler.handle(cmd);

        final ImmutableList<Message> expectedMessages = handler.getEventsOnStartProjectCmd();
        final List<Event> actualEvents = verifyPostedEvents(expectedMessages.size());
        for (int i = 0; i < expectedMessages.size(); i++) {
            final Message expected = expectedMessages.get(i);
            final Message actual = Events.getMessage(actualEvents.get(i));
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
        final TestCommandHandler same = new TestCommandHandler();

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

    private List<Event> verifyPostedEvents(int expectedEventCount) {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(expectedEventCount)).post(eventCaptor.capture());
        final List<Event> events = eventCaptor.getAllValues();
        assertEquals(expectedEventCount, events.size());
        return events;
    }

    private void assertHandles(Command cmd) {
        handler.handle(cmd);
        handler.assertHandled(cmd);
    }

    @SuppressWarnings({"OverloadedMethodsWithSameNumberOfParameters", "ReturnOfCollectionOrArrayField"})
    private class TestCommandHandler extends CommandHandler {

        private final ImmutableList<Message> eventsOnStartProjectCmd =
                createEventsOnStartProjectCmd();

        private final CommandHistory commandsHandled = new CommandHistory();

        private TestCommandHandler() {
            super(eventBus);
        }

        private void assertHandled(Command expected) {
            commandsHandled.assertHandled(expected);
        }

        private void handle(Command cmd) {
            final CommandEnvelope commandEnvelope = CommandEnvelope.of(cmd);
            dispatch(commandEnvelope);
        }

        private ImmutableList<Message> getEventsOnStartProjectCmd() {
            return eventsOnStartProjectCmd;
        }

        @Assign
        ProjectCreated handle(CreateProject msg, CommandContext context) {
            commandsHandled.add(msg, context);
            return ProjectCreated.getDefaultInstance();
        }

        @Assign
        TaskAdded handle(AddTask msg, CommandContext context) {
            commandsHandled.add(msg, context);
            return TaskAdded.getDefaultInstance();
        }

        @Assign
        List<Message> handle(StartProject msg, CommandContext context) {
            commandsHandled.add(msg, context);
            return eventsOnStartProjectCmd;
        }

        private ImmutableList<Message> createEventsOnStartProjectCmd() {
            final ImmutableList.Builder<Message> builder = ImmutableList.builder();
            builder.add(ProjectStarted.getDefaultInstance(), StringValue.getDefaultInstance());
            return builder.build();
        }
    }
}
