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

package org.spine3.server.aggregate;

import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.server.Assign;
import org.spine3.server.BoundedContext;
import org.spine3.server.BoundedContextTestStubs;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.TaskAdded;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.project.Project.getDefaultInstance;
import static org.spine3.testdata.TestEventFactory.projectCreatedEvent;
import static org.spine3.testdata.TestEventFactory.taskAddedEvent;

@SuppressWarnings("InstanceMethodNamingConvention")
public class AggregateRepositoryShould {

    private StorageFactory storageFactory;
    private BoundedContext boundedContext;
    private AggregateRepository<ProjectId, ProjectAggregate> repository;

    @Before
    public void setUp() {
        storageFactory = InMemoryStorageFactory.getInstance();
        boundedContext = BoundedContextTestStubs.create(storageFactory);
        repository = new ProjectAggregateRepository(boundedContext);

    }

    private static class ProjectAggregate extends Aggregate<ProjectId, Project> {

        public ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Override
        protected Project getDefaultState() {
            return getDefaultInstance();
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            return projectCreatedEvent(cmd.getProjectId());
        }

        @Assign
        public TaskAdded handle(AddTask cmd, CommandContext ctx) {
            return taskAddedEvent(cmd.getProjectId());
        }
    }

    private static class ProjectAggregateRepository extends AggregateRepository<ProjectId, ProjectAggregate> {
        private ProjectAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    @Test
    public void return_aggregate_class() {
        assertEquals(ProjectAggregate.class, repository.getAggregateClass());
    }

    @Test
    public void have_default_value_for_snapshot_trigger() {
        assertEquals(AggregateRepository.DEFAULT_SNAPSHOT_TRIGGER, repository.getSnapshotTrigger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_negative_snapshot_trigger() {
        repository.setSnapshotTrigger(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_zero_snapshot_trigger() {
        repository.setSnapshotTrigger(0);
    }

    @Test
    public void allow_to_change_snapshot_trigger() {
        final int newSnapshotTrigger = 1000;
        repository.setSnapshotTrigger(newSnapshotTrigger);
        assertEquals(newSnapshotTrigger, repository.getSnapshotTrigger());
    }

    @Test
    public void return_map_of_methods_to_handled_commands() {
        final Multimap<Method, Class<? extends Message>> handlers = repository.getHandlers();
        assertTrue(handlers.containsValue(CreateProject.class));
        assertTrue(handlers.containsValue(AddTask.class));
    }

    //TODO:2016-01-21:alexander.yevsyukov: Cover more.
}
