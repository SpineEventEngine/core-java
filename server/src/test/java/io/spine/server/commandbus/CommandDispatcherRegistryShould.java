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

import com.google.protobuf.Message;
import io.spine.core.CommandClass;
import io.spine.server.BoundedContext;
import io.spine.server.command.CommandHandler;
import io.spine.server.commandbus.given.CommandDispatcherRegistryTestEnv.AddTaskDispatcher;
import io.spine.server.commandbus.given.CommandDispatcherRegistryTestEnv.AllCommandDispatcher;
import io.spine.server.commandbus.given.CommandDispatcherRegistryTestEnv.AllCommandHandler;
import io.spine.server.commandbus.given.CommandDispatcherRegistryTestEnv.CreateProjectDispatcher;
import io.spine.server.commandbus.given.CommandDispatcherRegistryTestEnv.CreateProjectHandler;
import io.spine.server.commandbus.given.CommandDispatcherRegistryTestEnv.EmptyCommandHandler;
import io.spine.server.commandbus.given.CommandDispatcherRegistryTestEnv.EmptyDispatcher;
import io.spine.server.commandbus.given.CommandDispatcherRegistryTestEnv.NoCommandsDispatcherRepo;
import io.spine.server.event.EventBus;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.CmdCreateProject;
import io.spine.test.command.CmdStartProject;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("OverlyCoupledClass")
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
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .setName(getClass().getSimpleName())
                                                            .build();
        eventBus = boundedContext.getEventBus();
        registry = new CommandDispatcherRegistry();
    }

    @SafeVarargs
    private final void assertSupported(Class<? extends Message>... cmdClasses) {
        final Set<CommandClass> supportedClasses = registry.getRegisteredMessageClasses();

        for (Class<? extends Message> clazz : cmdClasses) {
            final CommandClass cmdClass = CommandClass.of(clazz);
            assertTrue(supportedClasses.contains(cmdClass));
        }
    }

    @SafeVarargs
    private final void assertNotSupported(Class<? extends Message>... cmdClasses) {
        final Set<CommandClass> supportedClasses = registry.getRegisteredMessageClasses();

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
        registry.register(new CreateProjectHandler(eventBus));
        registry.register(new AddTaskDispatcher());

        registry.unregisterAll();

        assertTrue(registry.getRegisteredMessageClasses().isEmpty());
    }

    @Test
    public void state_that_no_commands_are_supported_if_nothing_registered() {
        assertNotSupported(CmdCreateProject.class, CmdAddTask.class, CmdStartProject.class);
    }

    @Test
    public void register_command_dispatcher() {
        registry.register(new AllCommandDispatcher());

        assertSupported(CmdCreateProject.class, CmdAddTask.class, CmdStartProject.class);
    }

    @Test
    public void unregister_command_dispatcher() {
        final CommandDispatcher<Message> dispatcher = new AllCommandDispatcher();

        registry.register(dispatcher);
        registry.unregister(dispatcher);

        assertNotSupported(CmdCreateProject.class, CmdAddTask.class, CmdStartProject.class);
    }

    @Test
    public void register_command_handler() {
        registry.register(new AllCommandHandler(eventBus));

        assertSupported(CmdCreateProject.class, CmdAddTask.class, CmdStartProject.class);
    }

    @Test
    public void unregister_command_handler() {
        final AllCommandHandler handler = new AllCommandHandler(eventBus);

        registry.register(handler);
        registry.unregister(handler);

        assertNotSupported(CmdCreateProject.class, CmdAddTask.class, CmdStartProject.class);
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
        final NoCommandsDispatcherRepo pmRepo = new NoCommandsDispatcherRepo();
        registry.register(DelegatingCommandDispatcher.of(pmRepo));
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_command_handlers_without_methods() {
        registry.register(new EmptyCommandHandler(eventBus));
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
        registry.register(new CreateProjectHandler(eventBus));
        registry.register(new CreateProjectDispatcher());
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_allow_to_register_handler_for_the_command_with_registered_dispatcher() {
        registry.register(new CreateProjectDispatcher());
        registry.register(new CreateProjectHandler(eventBus));
    }

    @Test
    public void unregister_handler() {
        final CommandHandler handler = new CreateProjectHandler(eventBus);
        registry.register(handler);
        registry.unregister(handler);
        assertNotSupported(CmdCreateProject.class);
    }

    @Test
    public void return_commands_both_dispatched_and_handled() {
        registry.register(new CreateProjectHandler(eventBus));
        registry.register(new AddTaskDispatcher());

        assertSupported(CmdCreateProject.class, CmdAddTask.class);
    }
}
