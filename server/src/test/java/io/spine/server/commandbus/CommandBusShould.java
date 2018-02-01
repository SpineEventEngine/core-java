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

package io.spine.server.commandbus;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.given.CommandBusTestEnv.ProjectAggregateRepository;
import io.spine.test.commandbus.CProject;
import io.spine.test.commandbus.CProjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.commandbus.given.CommandBusTestEnv.ProjectAggregate;
import static io.spine.server.commandbus.given.CommandBusTestEnv.addTask;
import static io.spine.server.commandbus.given.CommandBusTestEnv.projectId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandBusShould {

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(CommandBusShould.class);

    private BoundedContext boundedContext;
    private ProjectAggregateRepository projectRepository;

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        projectRepository = new ProjectAggregateRepository();
        boundedContext.register(projectRepository);
    }

    @After
    public void tearDown() throws Exception {
        boundedContext.close();
    }

    @Test
    public void dispatch_a_command() {
        final CProjectId projectId = projectId();
        final Command command = command(addTask(projectId));

        postCommand(command);

        assertEquals(1, taskCountForProject(projectId));
    }

    @Test
    public void filter_out_a_reused_command() {
        final CProjectId projectId = projectId();
        final Command command = command(addTask(projectId));

        postCommand(command);
        postCommand(command);

        assertEquals(1, taskCountForProject(projectId));
    }

    private static Command command(Message message) {
        return requestFactory.createCommand(message);
    }

    private void postCommand(Command command) {
        final StreamObserver<Ack> observer = noOpObserver();
        boundedContext.getCommandBus()
                      .post(command, observer);
    }

    private int taskCountForProject(CProjectId id) {
        final Optional<ProjectAggregate> projectAggregateOptional = projectRepository.find(id);
        assertTrue(projectAggregateOptional.isPresent());

        final ProjectAggregate aggregate = projectAggregateOptional.get();
        final CProject project = aggregate.getState();
        final int taskCount = project.getTaskCount();

        return taskCount;
    }
}
