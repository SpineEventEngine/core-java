/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.aggregate;

import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.Subscribe;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.AggregateCommandEndpointTestEnv.ProjectAggregate;
import io.spine.server.aggregate.given.AggregateCommandEndpointTestEnv.ProjectAggregateRepository;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.server.aggregate.given.Given.ACommand.addTask;
import static io.spine.server.aggregate.given.Given.ACommand.createProject;
import static io.spine.server.aggregate.given.Given.ACommand.startProject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AggregateCommandEndpoint should")
class AggregateCommandEndpointTest {

    private BoundedContext boundedContext;
    private AggregateRepository<ProjectId, ProjectAggregate> repository;

    private ProjectId projectId;
    private Subscriber subscriber;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(false)
                                       .build();
        projectId = ProjectId.newBuilder()
                             .setId(Identifier.newUuid())
                             .build();

        // Create a subscriber of ProjectCreated event.
        subscriber = new Subscriber();
        boundedContext.getEventBus()
                      .register(subscriber);

        repository = new ProjectAggregateRepository();
        boundedContext.register(repository);
    }

    @AfterEach
    void tearDown() throws Exception {
        ProjectAggregate.clearCommandsHandled();
        boundedContext.close();
    }

    @Test
    @DisplayName("post events on command dispatching")
    void postEventsOnCommandDispatching() {
        CommandEnvelope cmd = CommandEnvelope.of(createProject(projectId));

        ProjectId id = repository.dispatch(cmd);
        assertEquals(projectId, id);

        AggProjectCreated msg = subscriber.remembered;
        assertEquals(projectId, msg.getProjectId());
    }

    @Test
    @DisplayName("store aggregate on command dispatching")
    void storeAggregateOnCommandDispatching() {
        CommandEnvelope cmd = CommandEnvelope.of(createProject(projectId));
        AggCreateProject msg = (AggCreateProject) cmd.message();

        ProjectId id = repository.dispatch(cmd);
        assertEquals(projectId, id);

        Optional<ProjectAggregate> optional = repository.find(projectId);
        assertTrue(optional.isPresent());

        ProjectAggregate aggregate = optional.get();
        assertEquals(projectId, aggregate.getId());

        assertEquals(msg.getName(), aggregate.getState()
                                             .getName());
    }

    @Test
    @DisplayName("dispatch command")
    void dispatchCommand() {
        assertDispatches(createProject());
    }

    @Test
    @DisplayName("dispatch several commands")
    void dispatchSeveralCommands() {
        assertDispatches(createProject(projectId));
        assertDispatches(addTask(projectId));
        assertDispatches(startProject(projectId));
    }

    /*
     * Utility methods.
     ****************************/

    @SuppressWarnings("CheckReturnValue")
    // ignore ID of the aggregate returned by the repository
    private void assertDispatches(Command cmd) {
        CommandEnvelope envelope = CommandEnvelope.of(cmd);
        repository.dispatch(envelope);
        ProjectAggregate.assertHandled(cmd);
    }

    private static class Subscriber extends AbstractEventSubscriber {

        private AggProjectCreated remembered;

        @Subscribe
        public void on(AggProjectCreated msg) {
            remembered = msg;
        }
    }
}
