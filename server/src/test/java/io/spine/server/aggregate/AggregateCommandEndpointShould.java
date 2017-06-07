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

package io.spine.server.aggregate;

import com.google.common.base.Optional;
import io.spine.annotation.Subscribe;
import io.spine.base.Command;
import io.spine.base.Identifier;
import io.spine.envelope.CommandEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.AggregateCommandEndpointTestEnv.ProjectAggregate;
import io.spine.server.aggregate.given.AggregateCommandEndpointTestEnv.ProjectAggregateRepository;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.event.EventSubscriber;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.CreateProject;
import io.spine.test.aggregate.event.ProjectCreated;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static io.spine.server.aggregate.Given.ACommand.addTask;
import static io.spine.server.aggregate.Given.ACommand.createProject;
import static io.spine.server.aggregate.Given.ACommand.startProject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AggregateCommandEndpointShould {

    private BoundedContext boundedContext;
    private AggregateRepository<ProjectId, ProjectAggregate> repository;

    /**
     * The spy to be used when it is required to avoid problems, make tests faster and easier.
     */
    private AggregateRepository<ProjectId, ProjectAggregate> repositorySpy;

    private ProjectId projectId;
    private Subscriber subscriber;

    @Before
    public void setUp() {
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
        repositorySpy = spy(repository);
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

        final ProjectCreated msg = subscriber.remembered;
        assertEquals(projectId, msg.getProjectId());
    }

    @Test
    public void store_aggregate_on_command_dispatching() {
        final CommandEnvelope cmd = CommandEnvelope.of(createProject(projectId));
        final CreateProject msg = (CreateProject) cmd.getMessage();

        repository.dispatch(cmd);

        final Optional<ProjectAggregate> optional = repository.find(projectId);
        assertTrue(optional.isPresent());

        final ProjectAggregate aggregate = optional.get();
        assertEquals(projectId, aggregate.getId());

        assertEquals(msg.getName(), aggregate.getState()
                                             .getName());
    }

    @Ignore //TODO:2017-06-06:alexander.yevsyukov: Restore when the Mockito bug is fixed.
            // Currently it complains about the non-stubbed method call `dispatch()` as if it was stabbed.
    @Test
    public void repeat_command_dispatching_if_event_count_is_changed_during_dispatching() {
        @SuppressWarnings("unchecked") //
        final AggregateStorage<ProjectId> storage = mock(AggregateStorage.class);

        // Change reported event count upon the second invocation and trigger re-dispatch.
        doReturn(0, 1)
                .when(storage)
                .readEventCountAfterLastSnapshot(projectId);
//        doReturn(Optional.absent())
//                .when(storage)
//                .read(projectId);
        when(storage.read(projectId))
                .thenReturn(Optional.<AggregateStateRecord>absent());
        when(storage.readLifecycleFlags(projectId))
                .thenReturn(Optional.<LifecycleFlags>absent());

//        doReturn(Optional.absent())
//                .when(storage)
//                .readLifecycleFlags(projectId);

        doReturn(storage)
                .when(repositorySpy)
                .aggregateStorage();


        final CommandEnvelope cmd = CommandEnvelope.of(createProject(projectId));
        repositorySpy.dispatch(cmd);

        // Load should be executed twice due to repeated dispatching.
        verify(repositorySpy, times(2))
                .loadOrCreate(projectId);

        // Reading event count is executed 2 times per dispatch (so 2 * 2)
        // plus once upon storing the state.
        verify(storage, times(2 * 2 + 1))
                .readEventCountAfterLastSnapshot(projectId);
    }

    @Test
    public void dispatch_command() {
        assertDispatches(createProject());
    }

    @Test
    public void dispatch_several_commands() {
        assertDispatches(createProject(projectId));
        assertDispatches(addTask(projectId));
        assertDispatches(startProject(projectId));
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

        private ProjectCreated remembered;

        @Subscribe
        void on(ProjectCreated msg) {
            remembered = msg;
        }
    }
}
