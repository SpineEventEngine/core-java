/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.command;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.base.Event;
import org.spine3.base.Events;
import org.spine3.server.event.EventBus;
import org.spine3.test.command.AddTask;
import org.spine3.test.command.CreateProject;
import org.spine3.test.command.StartProject;
import org.spine3.test.command.event.ProjectCreated;
import org.spine3.test.command.event.ProjectStarted;
import org.spine3.test.command.event.TaskAdded;
import org.spine3.testdata.TestCommandBusFactory;
import org.spine3.testdata.TestEventBusFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.newHashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.base.Commands.getMessage;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.test.Tests.nullRef;

/**
 * @author Alexander Litus
 */
public class CommandHandlerShould {

    private final CommandBus commandBus = TestCommandBusFactory.create();
    private final EventBus eventBus = spy(TestEventBusFactory.create());
    private final TestCommandHandler handler = new TestCommandHandler();

    @Before
    public void setUpTest() {
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
        assertFalse(handler.getId().trim().isEmpty());

        final int hashCode = handler.hashCode();

        assertTrue(hashCode != 0);
    }

    @Test
    public void generate_same_hash_code_for_same_instances() {
        assertEquals(handler.hashCode(), handler.hashCode());
    }

    @Test
    public void generate_different_hash_codes_for_different_instances() {
        assertNotEquals(new TestCommandHandler().hashCode(), new TestCommandHandler().hashCode());
    }

    @Test
    public void assure_same_handlers_are_equal() {
        final TestCommandHandler same = new TestCommandHandler(handler.getId());

        assertTrue(handler.equals(same));
    }

    @Test
    public void assure_handler_is_equal_to_itself() {
        // noinspection EqualsWithItself
        assertTrue(handler.equals(handler));
    }

    @Test
    public void assure_handler_is_not_equal_to_null() {
        assertFalse(handler.equals(nullRef()));
    }

    @Test
    public void assure_handler_is_not_equal_to_object_of_another_class() {
        //noinspection EqualsBetweenInconvertibleTypes
        assertFalse(handler.equals(newUuid()));
    }

    @Test
    public void assure_handlers_with_different_ids_are_not_equal() {
        final TestCommandHandler another = new TestCommandHandler();

        assertNotEquals(handler.getId(), another.getId());
        assertFalse(handler.equals(another));
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

        private final ImmutableList<Message> eventsOnStartProjectCmd = createEventsOnStartProjectCmd();

        private final Map<CommandId, Command> commandsHandled = newHashMap();

        protected TestCommandHandler(String id) {
            super(id, eventBus);
        }

        protected TestCommandHandler() {
            this(newUuid());
        }

        /* package */ void assertHandled(Command expected) {
            final CommandId id = Commands.getId(expected);
            final Command actual = commandsHandled.get(id);
            final String cmdName = getMessage(expected).getClass().getName();
            assertNotNull("Expected but wasn't handled, command: " + cmdName, actual);
            assertEquals(expected, actual);
        }

        /* package */ void handle(Command cmd) {
            handle(getMessage(cmd), cmd.getContext());
        }

        /* package */ ImmutableList<Message> getEventsOnStartProjectCmd() {
            return eventsOnStartProjectCmd;
        }

        @Override
        public void handle(Message commandMessage, CommandContext context) {
            try {
                super.handle(commandMessage, context);
            } catch (InvocationTargetException e) {
                throw propagate(e);
            }
        }

        @Assign
        public ProjectCreated handle(CreateProject msg, CommandContext context) {
            final Command cmd = Commands.create(msg, context);
            commandsHandled.put(context.getCommandId(), cmd);
            return ProjectCreated.getDefaultInstance();
        }

        @Assign
        public TaskAdded handle(AddTask msg, CommandContext context) {
            final Command cmd = Commands.create(msg, context);
            commandsHandled.put(context.getCommandId(), cmd);
            return TaskAdded.getDefaultInstance();
        }

        @Assign
        public List<Message> handle(StartProject msg, CommandContext context) {
            final Command cmd = Commands.create(msg, context);
            commandsHandled.put(context.getCommandId(), cmd);
            return eventsOnStartProjectCmd;
        }

        private ImmutableList<Message> createEventsOnStartProjectCmd() {
            final ImmutableList.Builder<Message> builder = ImmutableList.builder();
            builder.add(ProjectStarted.getDefaultInstance(), StringValue.getDefaultInstance());
            return builder.build();
        }
    }
}
