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


import com.google.common.collect.Sets;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.spine3.testdata.TestCommands.createProjectCmd;
import static org.spine3.testdata.TestEventMessageFactory.*;

@SuppressWarnings("InstanceMethodNamingConvention")
public class ClientServiceShould {

    private final Set<BoundedContext> boundedContexts = Sets.newHashSet();
    private ClientService clientService;

    @Before
    public void setUp() {
        final BoundedContext boundedContext = BoundedContextTestStubs.create(InMemoryStorageFactory.getInstance());
        final ProjectAggregateRepository repository = new ProjectAggregateRepository(boundedContext);
        boundedContext.register(repository);
        boundedContexts.add(boundedContext);

        //TODO:2016-05-27:alexander.yevsyukov: Add two more bounded contexts.

        final ClientService.Builder builder = ClientService.newBuilder();
        for (BoundedContext context : boundedContexts) {
            builder.addBoundedContext(context);
        }

        clientService = builder.build();
    }

    @After
    public void tearDown() throws Exception {
        if (!clientService.isShutdown()) {
            clientService.shutdown();
        }

        for (BoundedContext boundedContext : boundedContexts) {
            boundedContext.close();
        }
    }

    @Test
    public void accept_commands_from_linked_bounded_contexts() {
        final Command cmd = createProjectCmd();
        final TestResponseObserver responseObserver = new TestResponseObserver();

        clientService.post(cmd, responseObserver);
        assertEquals(Responses.ok(), responseObserver.getResponseHandled());
    }

    /*
     * Mock classes for a repository and an aggregate.
     ***************************************************/

    private static class ProjectAggregateRepository extends AggregateRepository<ProjectId, ProjectAggregate> {
        private ProjectAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    private static class ProjectAggregate extends Aggregate<ProjectId, Project, Project.Builder> {
        // an aggregate constructor must be public because it is used via reflection
        @SuppressWarnings("PublicConstructorInNonPublicClass")
        public ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            return projectCreatedMsg(cmd.getProjectId());
        }

        @Assign
        public TaskAdded handle(AddTask cmd, CommandContext ctx) {
            return taskAddedMsg(cmd.getProjectId());
        }

        @Assign
        public List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            final ProjectStarted message = projectStartedMsg(cmd.getProjectId());
            return newArrayList(message);
        }

        @Apply
        private void event(ProjectCreated event) {
            final Project newState = Project.newBuilder(getState())
                                            .setId(event.getProjectId())
                                            .setStatus(Project.Status.CREATED)
                                            .build();
            incrementState(newState);
        }

        @Apply
        private void event(TaskAdded event) {
        }

        @Apply
        private void event(ProjectStarted event) {
            final Project newState = Project.newBuilder(getState())
                                            .setId(event.getProjectId())
                                            .setStatus(Project.Status.STARTED)
                                            .build();
            incrementState(newState);
        }
    }

    private static class TestResponseObserver implements StreamObserver<Response> {

        private Response responseHandled;

        @Override
        public void onNext(Response response) {
            this.responseHandled = response;
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onCompleted() {
        }

        public Response getResponseHandled() {
            return responseHandled;
        }
    }

}
