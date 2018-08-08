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

package io.spine.server.command;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.client.CommandFactory;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.EventBus;
import io.spine.test.command.CmdCreateProject;
import io.spine.test.command.FirstCmdCreateProject;
import io.spine.test.command.ProjectId;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static io.spine.base.Identifier.newUuid;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("AbstractCommander should")
class AbstractCommanderTest {

    private final CommandFactory factory = TestActorRequestFactory.newInstance(getClass())
                                                                  .command();
    private final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                                .build();
    private CommandInterceptor interceptor;

    @BeforeEach
    void setUp() {
        CommandBus commandBus = boundedContext.getCommandBus();
        AbstractCommander commander = new Commendatore(commandBus, boundedContext.getEventBus());
        interceptor = new CommandInterceptor(boundedContext, FirstCmdCreateProject.class);
        commandBus.register(commander);
    }

    @Test
    @DisplayName("create a command in response to a command")
    void commandOnCommand() {
        CmdCreateProject commandMessage = CmdCreateProject
                .newBuilder()
                .setProjectId(newId())
                .build();
        createAndPost(commandMessage);

        assertTrue(interceptor.contains(FirstCmdCreateProject.class));
    }

    private static ProjectId newId() {
        return ProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    private void createAndPost(CmdCreateProject commandMessage) {
        io.spine.core.Command command = factory.create(commandMessage);
        boundedContext.getCommandBus()
                      .post(command, StreamObservers.noOpObserver());
    }

    /**
     * Test environment class that generates new commands in response to incoming messages.
     */
    private static final class Commendatore extends AbstractCommander {

        private Commendatore(CommandBus commandBus, EventBus eventBus) {
            super(commandBus, eventBus);
        }

        @Command
        FirstCmdCreateProject on(CmdCreateProject command) {
            return FirstCmdCreateProject
                    .newBuilder()
                    .setId(command.getProjectId())
                    .build();
        }
    }

    /**
     * Utility class that remembers all commands issued by a commander class.
     */
    static class CommandInterceptor extends AbstractCommandHandler {

        private final Set<CommandClass> intercept;
        private final CommandHistory history = new CommandHistory();

        @SafeVarargs
        @SuppressWarnings("ThisEscapedInObjectConstruction") // already configured
        private CommandInterceptor(BoundedContext context,
                                   Class<? extends Message>... commandClass) {
            super(context.getEventBus());
            this.intercept = ImmutableSet.copyOf(
                    Arrays.stream(commandClass)
                          .map(CommandClass::of)
                          .collect(Collectors.toSet())
            );
            context.getCommandBus()
                   .register(this);
        }

        @Override
        public String dispatch(CommandEnvelope envelope) {
            history.add(envelope);
            return getClass().getName();
        }

        @Override
        public Set<CommandClass> getMessageClasses() {
            return intercept;
        }

        public boolean contains(Class<? extends Message> commandClass) {
            return history.contains(commandClass);
        }
    }
}
