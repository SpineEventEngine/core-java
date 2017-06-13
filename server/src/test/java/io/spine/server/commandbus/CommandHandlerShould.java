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

package io.spine.server.commandbus;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.Command;
import io.spine.base.CommandContext;
import io.spine.base.Event;
import io.spine.base.Events;
import io.spine.envelope.CommandEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHandler;
import io.spine.server.command.CommandHistory;
import io.spine.server.event.EventBus;
import io.spine.test.command.AddTask;
import io.spine.test.command.CreateProject;
import io.spine.test.command.StartProject;
import io.spine.test.command.event.ProjectCreated;
import io.spine.test.command.event.ProjectStarted;
import io.spine.test.command.event.TaskAdded;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Identifier.newUuid;
import static io.spine.test.Tests.nullRef;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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
        @SuppressWarnings("unchecked") // Can't create a Captor for a generic type
        final ArgumentCaptor<Iterable<Event>> eventCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(eventBus).post(eventCaptor.capture());
        final Iterable<Event> events = eventCaptor.getValue();
        final List<Event> eventsList = newArrayList(events);
        assertEquals(expectedEventCount, eventsList.size());
        return eventsList;
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
