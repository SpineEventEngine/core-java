/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Optional;
import io.grpc.stub.StreamObserver;
import io.spine.Identifier;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.Subscribe;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.AggregateCommandEndpointTestEnv.ProjectAggregate;
import io.spine.server.aggregate.given.AggregateCommandEndpointTestEnv.ProjectAggregateRepository;
import io.spine.server.event.EventSubscriber;
import io.spine.server.model.ModelTests;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.aggregate.given.Given.ACommand.addTask;
import static io.spine.server.aggregate.given.Given.ACommand.createProject;
import static io.spine.server.aggregate.given.Given.ACommand.startProject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mykhailo Drachuk
 */
public class AggregateCommandEndpointShould {

    private BoundedContext boundedContext;
    private AggregateRepository<ProjectId, ProjectAggregate> repository;

    private ProjectId projectId;
    private Subscriber subscriber;

    @Before
    public void setUp() {
        ModelTests.clearModel();
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

    @After
    public void tearDown() throws Exception {
        ProjectAggregate.clearCommandsHandled();
        boundedContext.close();
    }

    @Test
    public void post_events_on_command_dispatching() {
        final CommandEnvelope cmd = CommandEnvelope.of(createProject(projectId));

        repository.dispatch(cmd);

        final AggProjectCreated msg = subscriber.remembered;
        assertEquals(projectId, msg.getProjectId());
    }

    @Test
    public void store_aggregate_on_command_dispatching() {
        final CommandEnvelope cmd = CommandEnvelope.of(createProject(projectId));
        final AggCreateProject msg = (AggCreateProject) cmd.getMessage();

        repository.dispatch(cmd);

        final Optional<ProjectAggregate> optional = repository.find(projectId);
        assertTrue(optional.isPresent());

        final ProjectAggregate aggregate = optional.get();
        assertEquals(projectId, aggregate.getId());

        assertEquals(msg.getName(), aggregate.getState()
                                             .getName());
    }

    @Test
    public void dispatch_a_command() {
        assertDispatches(createProject());
    }

    @Test
    public void dispatch_several_commands() {
        assertDispatches(createProject(projectId));
        assertDispatches(addTask(projectId));
        assertDispatches(startProject(projectId));
    }

    @Test
    public void filter_out_a_reused_command() {
        final Command command = addTask(projectId);

        postToBoundedContext(command);
        postToBoundedContext(command);

        assertEquals(1, taskCountForProject(projectId));
    }

    @Test
    public void filter_out_the_same_command() {
        final Command command = addTask(projectId);

        postToBoundedContext(command, command);

        assertEquals(1, taskCountForProject(projectId));
    }

    /*
     * Utility methods.
     ****************************/

    private void assertDispatches(Command cmd) {
        final CommandEnvelope envelope = CommandEnvelope.of(cmd);
        repository.dispatch(envelope);
        ProjectAggregate.assertHandled(cmd);
    }

    private static class Subscriber extends EventSubscriber {

        private AggProjectCreated remembered;

        @Subscribe
        void on(AggProjectCreated msg) {
            remembered = msg;
        }
    }

    private void postToBoundedContext(Command command) {
        final StreamObserver<Ack> observer = noOpObserver();
        boundedContext.getCommandBus()
                      .post(command, observer);
    }

    private void postToBoundedContext(Command... commands) {
        final StreamObserver<Ack> observer = noOpObserver();
        boundedContext.getCommandBus()
                      .post(newArrayList(commands), observer);
    }

    private int taskCountForProject(ProjectId id) {
        final Optional<ProjectAggregate> optional = repository.find(id);
        assertTrue(optional.isPresent());

        final ProjectAggregate aggregate = optional.get();
        final Project project = aggregate.getState();
        final int taskCount = project.getTaskCount();
        return taskCount;
    }
}
