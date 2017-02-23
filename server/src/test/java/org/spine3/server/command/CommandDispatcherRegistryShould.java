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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.CommandClass;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandEnvelope;
import org.spine3.server.BoundedContext;
import org.spine3.server.event.EventBus;
import org.spine3.server.procman.ProcessManagerRepository;
import org.spine3.server.procman.ProcessManagerRepositoryShould;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
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
import static org.spine3.base.Identifiers.newUuid;

/**
 * @author Alexander Yevsyukov
 */
public class CommandDispatcherRegistryShould {

    /**
     * The object we test.
     */
    private CommandDispatcherRegistry registry;

    /**
     * The instance of {@code EventBus} that we need for stub command handler classes.
     */
    private EventBus eventBus;

    @Before
    public void setUp() {
        eventBus = TestEventBusFactory.create(InMemoryStorageFactory.getInstance());

        registry = new CommandDispatcherRegistry();
    }

    @SafeVarargs
    private final void assertSupported(Class<? extends Message>... cmdClasses) {
        final Set<CommandClass> supportedClasses = registry.getMessageClasses();

        for (Class<? extends Message> clazz : cmdClasses) {
            final CommandClass cmdClass = CommandClass.of(clazz);
            assertTrue(supportedClasses.contains(cmdClass));
        }
    }

    @SafeVarargs
    private final void assertNotSupported(Class<? extends Message>... cmdClasses) {
        final Set<CommandClass> supportedClasses = registry.getMessageClasses();

        for (Class<? extends Message> clazz : cmdClasses) {
            final CommandClass cmdClass = CommandClass.of(clazz);
            assertFalse(supportedClasses.contains(cmdClass));
        }
    }

    /*
     * Registration tests.
     *********************/
    @Test
    public void remove_all_handlers_and_dispatchers() {
        // Pass real objects. We cannot use mocks because we need declared methods.
        registry.register(new CreateProjectHandler(newUuid()));
        registry.register(new AddTaskDispatcher());

        registry.unregisterAll();

        assertTrue(registry.getMessageClasses().isEmpty());
    }

    @Test
    public void state_that_no_commands_are_supported_if_nothing_registered() {
        assertNotSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test
    public void register_command_dispatcher() {
        registry.register(new AllCommandDispatcher());

        assertSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test
    public void unregister_command_dispatcher() {
        final CommandDispatcher dispatcher = new AllCommandDispatcher();

        registry.register(dispatcher);
        registry.unregister(dispatcher);

        assertNotSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test
    public void register_command_handler() {
        registry.register(new AllCommandHandler());

        assertSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test
    public void unregister_command_handler() {
        final AllCommandHandler handler = new AllCommandHandler();

        registry.register(handler);
        registry.unregister(handler);

        assertNotSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_empty_dispatchers() {
        registry.register(new EmptyDispatcher());
    }

    /**
     * Verifies if it's possible to pass a {@link ProcessManagerRepository}
     * which does not expose any command classes.
     */
    @Test
    public void accept_empty_process_manager_repository_dispatcher() {
        final ProcessManagerRepoDispatcher pmRepo =
                new ProcessManagerRepoDispatcher(BoundedContext.newBuilder()
                                                               .build());
        registry.register(pmRepo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_command_handlers_without_methods() {
        registry.register(new EmptyCommandHandler());
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_allow_another_dispatcher_for_already_registered_commands() {
        registry.register(new AllCommandDispatcher());
        registry.register(new AllCommandDispatcher());
    }

    /*
     * Tests for not overriding handlers by dispatchers and vice versa.
     ******************************************************************/

    @Test(expected = IllegalArgumentException.class)
    public void not_allow_to_register_dispatcher_for_the_command_with_registered_handler() {
        registry.register(new CreateProjectHandler(newUuid()));
        registry.register(new CreateProjectDispatcher());
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_allow_to_register_handler_for_the_command_with_registered_dispatcher() {
        registry.register(new CreateProjectDispatcher());
        registry.register(new CreateProjectHandler(newUuid()));
    }

    @Test
    public void unregister_handler() {
        final CommandHandler handler = new CreateProjectHandler(newUuid());
        registry.register(handler);
        registry.unregister(handler);
        assertNotSupported(CreateProject.class);
    }

    @Test
    public void return_commands_both_dispatched_and_handled() {
        registry.register(new CreateProjectHandler(newUuid()));
        registry.register(new AddTaskDispatcher());

        assertSupported(CreateProject.class, AddTask.class);
    }

    /*
     * Test command dispatchers.
     ***************************/

    private static class EmptyDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getMessageClasses() {
            return Collections.emptySet();
        }

        @Override
        public void dispatch(CommandEnvelope envelope) {
        }
    }

    //TODO:2017-02-11:alexander.yevsyukov: Fix inter-test dependency.
    private static class ProcessManagerRepoDispatcher
            extends ProcessManagerRepository<ProjectId,
            ProcessManagerRepositoryShould.TestProcessManager, Project> {

        protected ProcessManagerRepoDispatcher(BoundedContext boundedContext) {
            super(boundedContext);
        }

        /**
         * Always returns an empty set of command classes forwarded by this repository.
         */
        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public Set<CommandClass> getMessageClasses() {
            return newHashSet();
        }
    }

    private static class AllCommandDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(CreateProject.class, StartProject.class, AddTask.class);
        }

        @Override
        public void dispatch(CommandEnvelope envelope) {
        }
    }

    private static class CreateProjectDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(CreateProject.class);
        }

        @Override
        public void dispatch(CommandEnvelope envelope) {
        }
    }

    private static class AddTaskDispatcher implements CommandDispatcher {

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(AddTask.class);
        }

        @Override
        public void dispatch(CommandEnvelope envelope) {
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

        @Override
        public Set<CommandClass> getMessageClasses() {
            return ImmutableSet.of(CommandClass.of(CreateProject.class));
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

        @Override
        public Set<CommandClass> getMessageClasses() {
            return ImmutableSet.of(CommandClass.of(CreateProject.class),
                                   CommandClass.of(StartProject.class),
                                   CommandClass.of(AddTask.class));
        }
    }

    private class EmptyCommandHandler extends CommandHandler {
        protected EmptyCommandHandler() {
            super(newUuid(), eventBus);
        }

        @Override
        public Set<CommandClass> getMessageClasses() {
            return ImmutableSet.of();
        }
    }
}
