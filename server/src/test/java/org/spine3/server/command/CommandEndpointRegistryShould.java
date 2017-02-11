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

package org.spine3.server.command;

import com.google.protobuf.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.server.BoundedContext;
import org.spine3.server.event.EventBus;
import org.spine3.server.procman.ProcessManagerRepository;
import org.spine3.server.procman.ProcessManagerRepositoryShould;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.CommandClass;
import org.spine3.test.command.AddTask;
import org.spine3.test.command.CreateProject;
import org.spine3.test.command.StartProject;
import org.spine3.test.command.event.ProjectCreated;
import org.spine3.test.command.event.ProjectStarted;
import org.spine3.test.command.event.TaskAdded;
import org.spine3.test.procman.Project;
import org.spine3.test.procman.ProjectId;
import org.spine3.testdata.TestEventBusFactory;

import java.util.Collections;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.spine3.base.Identifiers.newUuid;

/**
 * @author Alexander Yevsyukov
 */
public class CommandEndpointRegistryShould {

    private CommandBus commandBus;
    private CommandStore commandStore;
    private EventBus eventBus;
    private CreateProjectHandler createProjectHandler;

    @Before
    public void setUp() {
        final InMemoryStorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        commandStore = spy(new CommandStore(storageFactory.createCommandStorage()));
        commandBus = CommandBus.newBuilder()
                               .setCommandStore(commandStore)
                               .setThreadSpawnAllowed(true)
                               .setAutoReschedule(false)
                               .build();
        eventBus = TestEventBusFactory.create(storageFactory);
        createProjectHandler = new CreateProjectHandler(newUuid());
    }

    @After
    public void tearDown() throws Exception {
        if (commandStore.isOpen()) { // then command bus is opened, too
            commandBus.close();
        }
        eventBus.close();
    }


    @SafeVarargs
    private final void assertSupported(Class<? extends Message>... cmdClasses) {
        for (Class<? extends Message> clazz : cmdClasses) {
            final CommandClass cmdClass = CommandClass.of(clazz);
            assertTrue(commandBus.isSupportedCommand(cmdClass));
        }
    }

    @SafeVarargs
    private final void assertNotSupported(Class<? extends Message>... cmdClasses) {
        for (Class<? extends Message> clazz : cmdClasses) {
            final CommandClass cmdClass = CommandClass.of(clazz);
            assertFalse(commandBus.isSupportedCommand(cmdClass));
        }
    }

    //TODO:2017-02-11:alexander.yevsyukov: Test removing dispatchers too.
    @Test
    public void remove_all_handlers_on_close() throws Exception {
        commandBus.register(createProjectHandler);

        commandBus.close();
        assertNotSupported(CreateProject.class);
    }

    /*
     * Registration tests.
     *********************/

    @Test
    public void state_that_no_commands_are_supported_if_nothing_registered() {
        assertNotSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test
    public void register_command_dispatcher() {
        commandBus.register(new AllCommandDispatcher());

        assertSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test
    public void unregister_command_dispatcher() {
        final CommandDispatcher dispatcher = new AllCommandDispatcher();

        commandBus.register(dispatcher);
        commandBus.unregister(dispatcher);

        assertNotSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test
    public void register_command_handler() {
        commandBus.register(new AllCommandHandler());

        assertSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test
    public void unregister_command_handler() {
        final AllCommandHandler handler = new AllCommandHandler();

        commandBus.register(handler);
        commandBus.unregister(handler);

        assertNotSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_empty_dispatchers() {
        commandBus.register(new EmptyDispatcher());
    }

    @Test
    public void accept_empty_process_manager_repository_dispatcher() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        final ProcessManagerRepoDispatcher pmRepo = new ProcessManagerRepoDispatcher(boundedContext);
        commandBus.register(pmRepo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_command_handlers_without_methods() {
        commandBus.register(new EmptyCommandHandler());
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_allow_another_dispatcher_for_already_registered_commands() {
        commandBus.register(new AllCommandDispatcher());
        commandBus.register(new AllCommandDispatcher());
    }

    /*
     * Tests for not overriding handlers by dispatchers and vice versa.
     ******************************************************************/

    @Test(expected = IllegalArgumentException.class)
    public void not_allow_to_register_dispatcher_for_the_command_with_registered_handler() {
        final CommandDispatcher createProjectDispatcher = new CreateProjectDispatcher();
        commandBus.register(createProjectHandler);
        commandBus.register(createProjectDispatcher);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_allow_to_register_handler_for_the_command_with_registered_dispatcher() {
        final CommandDispatcher createProjectDispatcher = new CreateProjectDispatcher();
        commandBus.register(createProjectDispatcher);
        commandBus.register(createProjectHandler);
    }

    @Test
    public void unregister_handler() {
        commandBus.register(createProjectHandler);
        commandBus.unregister(createProjectHandler);
        assertNotSupported(CreateProject.class);
    }

    @Test
    public void validate_commands_both_dispatched_and_handled() {
        commandBus.register(createProjectHandler);
        commandBus.register(new AddTaskDispatcher());

        assertSupported(CreateProject.class, AddTask.class);
    }

    /*
     * Test command dispatchers.
     ***************************/

    private static class EmptyDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getCommandClasses() {
            return Collections.emptySet();
        }

        @Override
        public void dispatch(Command request) {
        }
    }

    //TODO:2017-02-11:alexander.yevsyukov: Fix inter-test dependency.
    private static class ProcessManagerRepoDispatcher
            extends ProcessManagerRepository<ProjectId, ProcessManagerRepositoryShould.TestProcessManager, Project> {

        protected ProcessManagerRepoDispatcher(BoundedContext boundedContext) {
            super(boundedContext);
        }

        /**
         * Always returns an empty set of command classes forwarded by this repository.
         */
        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public Set<CommandClass> getCommandClasses() {
            return newHashSet();
        }
    }

    private static class AllCommandDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getCommandClasses() {
            return CommandClass.setOf(CreateProject.class, StartProject.class, AddTask.class);
        }

        @Override
        public void dispatch(Command request) {
        }
    }

    private static class CreateProjectDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getCommandClasses() {
            return CommandClass.setOf(CreateProject.class);
        }

        @Override
        public void dispatch(Command request) {
        }
    }

    private static class AddTaskDispatcher implements CommandDispatcher {

        @Override
        public Set<CommandClass> getCommandClasses() {
            return CommandClass.setOf(AddTask.class);
        }

        @Override
        public void dispatch(Command request) {
            // Do nothing.
        }
    }

    /*
     * Test command handlers.
     ************************/
    
    private class CreateProjectHandler extends CommandHandler {

        protected CreateProjectHandler(String id) {
            super(id, eventBus);
        }

        @Assign
        ProjectCreated handle(CreateProject command, CommandContext ctx) {
            return ProjectCreated.getDefaultInstance();
        }
    }

    private class AllCommandHandler extends CommandHandler {

        protected AllCommandHandler() {
            super(newUuid(), eventBus);
        }

        @Assign
        ProjectCreated handle(CreateProject command, CommandContext ctx) {
            return ProjectCreated.getDefaultInstance();
        }

        @Assign
        TaskAdded handle(AddTask command) {
            return TaskAdded.getDefaultInstance();
        }

        @Assign
        ProjectStarted handle(StartProject command) {
            return ProjectStarted.getDefaultInstance();
        }
    }

    private class EmptyCommandHandler extends CommandHandler {
        protected EmptyCommandHandler() {
            super(newUuid(), eventBus);
        }
    }
}
