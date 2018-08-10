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

import com.google.protobuf.Message;
import io.spine.core.CommandClass;
import io.spine.server.BoundedContext;
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
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"OverlyCoupledClass",
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("CommandDispatcherRegistry should")
class CommandDispatcherRegistryTest {

    /**
     * The object we test.
     */
    private CommandDispatcherRegistry registry;

    /**
     * The instance of {@code EventBus} that we need for stub command handler classes.
     */
    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();

        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .setName(getClass().getSimpleName())
                                                      .build();
        eventBus = boundedContext.getEventBus();
        registry = new CommandDispatcherRegistry();
    }

    @SafeVarargs
    private final void assertSupported(Class<? extends Message>... cmdClasses) {
        Set<CommandClass> supportedClasses = registry.getRegisteredMessageClasses();

        for (Class<? extends Message> clazz : cmdClasses) {
            CommandClass cmdClass = CommandClass.from(clazz);
            assertTrue(supportedClasses.contains(cmdClass));
        }
    }

    @SafeVarargs
    private final void assertNotSupported(Class<? extends Message>... cmdClasses) {
        Set<CommandClass> supportedClasses = registry.getRegisteredMessageClasses();

        for (Class<? extends Message> clazz : cmdClasses) {
            CommandClass cmdClass = CommandClass.from(clazz);
            assertFalse(supportedClasses.contains(cmdClass));
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("command dispatcher")
        void commandDispatcher() {
            registry.register(new AllCommandDispatcher());

            assertSupported(CmdCreateProject.class, CmdAddTask.class, CmdStartProject.class);
        }

        @Test
        @DisplayName("command handler")
        void commandHandler() {
            registry.register(new AllCommandHandler(eventBus));

            assertSupported(CmdCreateProject.class, CmdAddTask.class, CmdStartProject.class);
        }
    }

    @Nested
    @DisplayName("unregister")
    class Unregister {

        @Test
        @DisplayName("command dispatcher")
        void commandDispatcher() {
            CommandDispatcher<Message> dispatcher = new AllCommandDispatcher();

            registry.register(dispatcher);
            registry.unregister(dispatcher);

            assertNotSupported(CmdCreateProject.class, CmdAddTask.class, CmdStartProject.class);
        }

        @Test
        @DisplayName("command handler")
        void commandHandler() {
            AllCommandHandler handler = new AllCommandHandler(eventBus);

            registry.register(handler);
            registry.unregister(handler);

            assertNotSupported(CmdCreateProject.class, CmdAddTask.class, CmdStartProject.class);
        }

        @Test
        @DisplayName("all command dispatchers and handlers")
        void everything() {
            registry.register(new CreateProjectHandler(eventBus));
            registry.register(new AddTaskDispatcher());

            registry.unregisterAll();

            assertTrue(registry.getRegisteredMessageClasses().isEmpty());
        }
    }

    @Nested
    @DisplayName("not accept empty")
    class NotAcceptEmpty {

        @Test
        @DisplayName("command dispatcher")
        void commandDispatcher() {
            assertThrows(IllegalArgumentException.class,
                         () -> registry.register(new EmptyDispatcher()));
        }

        @Test
        @DisplayName("command handler")
        void commandHandler() {
            assertThrows(IllegalArgumentException.class,
                         () -> registry.register(new EmptyCommandHandler(eventBus)));
        }

    }

    /**
     * Verifies if it's possible to pass a {@link ProcessManagerRepository}
     * which does not expose any command classes.
     */
    @Test
    @DisplayName("accept empty process manager repository dispatcher")
    void acceptEmptyProcessManagerRepository() {
        NoCommandsDispatcherRepo pmRepo = new NoCommandsDispatcherRepo();
        registry.register(DelegatingCommandDispatcher.of(pmRepo));
    }

    @Test
    @DisplayName("state both dispatched and handled commands as supported")
    void supportDispatchedAndHandled() {
        registry.register(new CreateProjectHandler(eventBus));
        registry.register(new AddTaskDispatcher());

        assertSupported(CmdCreateProject.class, CmdAddTask.class);
    }

    @Test
    @DisplayName("state that no commands are supported when nothing is registered")
    void supportNothingWhenEmpty() {
        assertNotSupported(CmdCreateProject.class, CmdAddTask.class, CmdStartProject.class);
    }

    @Nested
    @DisplayName("not allow to override")
    class NotOverride {

        @Test
        @DisplayName("registered dispatcher by another dispatcher")
        void dispatcherByDispatcher() {
            registry.register(new AllCommandDispatcher());
            assertThrows(IllegalArgumentException.class,
                         () -> registry.register(new AllCommandDispatcher()));
        }

        @Test
        @DisplayName("registered handler by another handler")
        void handlerByHandler() {
            registry.register(new CreateProjectHandler(eventBus));
            assertThrows(IllegalArgumentException.class,
                         () -> registry.register(new CreateProjectHandler(eventBus)));
        }

        @Test
        @DisplayName("registered dispatcher by handler")
        void dispatcherByHandler() {
            registry.register(new CreateProjectDispatcher());
            assertThrows(IllegalArgumentException.class,
                         () -> registry.register(new CreateProjectHandler(eventBus)));
        }

        @Test
        @DisplayName("registered handler by dispatcher")
        void handlerByDispatcher() {
            registry.register(new CreateProjectHandler(eventBus));
            assertThrows(IllegalArgumentException.class,
                         () -> registry.register(new CreateProjectDispatcher()));
        }
    }
}
