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

package org.spine3.server;

import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.Responses;
import org.spine3.client.CommandFactory;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.TestCommandFactory;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.time.ZoneOffsets;
import org.spine3.type.CommandClass;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.spine3.server.CommandValidation.isUnsupportedCommand;
import static org.spine3.testdata.TestCommands.*;

@SuppressWarnings("InstanceMethodNamingConvention")
public class CommandBusShould {

    private CommandBus commandBus;
    private CommandStore commandStore;

    @Before
    public void setUp() {
        final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();

        commandStore = new CommandStore(storageFactory.createCommandStorage());
        commandBus = CommandBus.create(commandStore);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_CommandStore_on_construction() {
        //noinspection ConstantConditions
        CommandBus.create(null);
    }

    //
    // Test for empty handler
    //--------------------------
    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_empty_dispatchers() {
        commandBus.register(new EmptyDispatcher());
    }

    private static class EmptyDispatcher implements CommandDispatcher {

        @Override
        public Set<CommandClass> getCommandClasses() {
            return Collections.emptySet();
        }

        @Override
        public List<Event> dispatch(Command request) throws Exception {
            //noinspection ReturnOfNull
            return null;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_command_handlers_without_methods() {
        commandBus.register(new EmptyCommandHandler());
    }

    @SuppressWarnings("EmptyClass")
    private static class EmptyCommandHandler implements CommandHandler {

    }

    //
    // Test for duplicate dispatchers
    //----------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_another_dispatcher_for_already_registered_commands() {
        commandBus.register(new TwoCommandDispatcher());
        commandBus.register(new TwoCommandDispatcher());
    }

    private static class TwoCommandDispatcher implements CommandDispatcher {

        @Override
        public Set<CommandClass> getCommandClasses() {
            return CommandClass.setOf(CreateProject.class, AddTask.class);
        }

        @Override
        public List<Event> dispatch(Command request) throws Exception {
            //noinspection ReturnOfNull
            return null;
        }
    }

    /**
     * Test for successful dispatcher registration.
     */
    @Test
    public void register_command_dispatcher() {
        commandBus.register(new CommandDispatcher() {
            @Override
            public Set<CommandClass> getCommandClasses() {
                return CommandClass.setOf(CreateProject.class, StartProject.class, AddTask.class);
            }

            @Override
            public List<Event> dispatch(Command request) throws Exception {
                //noinspection ReturnOfNull
                return null;
            }
        });

        final String projectId = "@Test register_command_dispatcher";
        assertEquals(Responses.ok(), commandBus.validate(createProject(projectId)));
        assertEquals(Responses.ok(), commandBus.validate(startProject(projectId)));
        assertEquals(Responses.ok(), commandBus.validate(addTask(projectId)));
    }

    /**
     * Test that unregistering dispatcher makes commands unsupported.
     */
    @Test
    public void turn_commands_unsupported_when_dispatcher_unregistered() {
        final CommandDispatcher dispatcher = new CommandDispatcher() {
            @Override
            public Set<CommandClass> getCommandClasses() {
                return CommandClass.setOf(CreateProject.class, StartProject.class, AddTask.class);
            }

            @Override
            public List<Event> dispatch(Command request) throws Exception {
                //noinspection ReturnOfNull
                return null;
            }
        };

        commandBus.register(dispatcher);
        commandBus.unregister(dispatcher);

        final String projectId = "@Test unregister_dispatcher";
        assertTrue(isUnsupportedCommand(commandBus.validate(createProject(projectId))));
        assertTrue(isUnsupportedCommand(commandBus.validate(startProject(projectId))));
        assertTrue(isUnsupportedCommand(commandBus.validate(addTask(projectId))));
    }

    //
    // Test for not overriding handlers by dispatchers and vice versa
    //-------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_to_register_dispatcher_for_the_command_with_registered_handler() {

        final CommandHandler createProjectHandler = new CreateProjectHandler();
        final CommandDispatcher createProjectDispatcher = new CreateProjectDispatcher();

        commandBus.register(createProjectHandler);
        commandBus.register(createProjectDispatcher);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_to_register_handler_for_the_command_with_registered_dispatcher() {

        final CommandHandler createProjectHandler = new CreateProjectHandler();
        final CommandDispatcher createProjectDispatcher = new CreateProjectDispatcher();

        commandBus.register(createProjectDispatcher);
        commandBus.register(createProjectHandler);
    }

    private static class CreateProjectHandler implements CommandHandler {

        private boolean handlerInvoked = false;
        @Assign
        public ProjectCreated handle(CreateProject command, CommandContext ctx) {
            handlerInvoked = true;
            return ProjectCreated.getDefaultInstance();
        }

        public boolean wasHandlerInvoked() {
            return handlerInvoked;
        }
    }

    private static class CreateProjectDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getCommandClasses() {
            return CommandClass.setOf(CreateProject.class);
        }

        @Override
        public List<Event> dispatch(Command request) throws Exception {
            //noinspection ReturnOfNull
            return null;
        }
    }

    @Test
    public void unregister_handler() {
        final CreateProjectHandler handler = new CreateProjectHandler();
        commandBus.register(handler);
        commandBus.unregister(handler);
        final String projectId = "@Test unregister_handler";
        assertTrue(isUnsupportedCommand(commandBus.validate(createProject(projectId))));
    }

    @Test
    public void validate_commands_both_dispatched_and_handled() {
        final CreateProjectHandler handler = new CreateProjectHandler();
        final AddTaskDispatcher dispatcher = new AddTaskDispatcher();
        commandBus.register(handler);
        commandBus.register(dispatcher);

        final String projectId = "@Test validate_commands_both_dispatched_and_handled";
        assertEquals(Responses.ok(), commandBus.validate(createProject(projectId)));
        assertEquals(Responses.ok(), commandBus.validate(addTask(projectId)));
    }

    private static class AddTaskDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getCommandClasses() {
            return CommandClass.setOf(AddTask.class);
        }

        @Override
        public List<Event> dispatch(Command request) throws Exception {
            //noinspection ReturnOfNull
            return null;
        }
    }

    @Test // To improve coverage stats.
    public void have_log() {
        assertNotNull(CommandBus.log());
    }

    @Test
    public void close_CommandStore_when_closed() throws Exception {
        commandBus.close();
        assertTrue(commandStore.isClosed());
    }

    @Test
    public void remove_all_handlers_on_close() throws Exception {
        final CreateProjectHandler handler = new CreateProjectHandler();
        commandBus.register(handler);

        commandBus.close();
        assertTrue(isUnsupportedCommand(commandBus.validate(createProject("remove_all_handlers_on_close"))));
    }

    @Test
    public void invoke_handler_when_command_posted() {
        final CreateProjectHandler handler = new CreateProjectHandler();
        commandBus.register(handler);

        final CommandFactory factory = TestCommandFactory.newInstance("invoke_handler",
                ZoneOffsets.UTC);
        final Command command = factory.create(createProject("_when_command_posted"));
        commandBus.post(command);

        assertTrue(handler.wasHandlerInvoked());
    }
}
