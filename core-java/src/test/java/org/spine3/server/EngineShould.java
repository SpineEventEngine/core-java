/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.*;
import org.spine3.eventbus.Subscribe;
import org.spine3.server.aggregate.AggregateRepositoryBase;
import org.spine3.server.aggregate.AggregateShould;
import org.spine3.server.error.UnsupportedCommandException;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;
import org.spine3.testdata.TestAggregateIdFactory;
import org.spine3.util.Users;

import javax.annotation.Nonnull;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.protobuf.util.TimeUtil.add;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.*;
import static org.spine3.protobuf.Durations.seconds;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.testdata.TestCommandRequestFactory.*;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class EngineShould {

    private final UserId userId = Users.newUserId("test_user");
    private final ProjectId projectId = TestAggregateIdFactory.createProjectId("test_project_id");
    private final EmptyHandler handler = new EmptyHandler();

    private boolean handlersRegistered = false;

    @Before
    public void setUp() {

        Engine.start(InMemoryStorageFactory.getInstance());
    }

    @After
    public void tearDown() {
        if (handlersRegistered) {
            Engine.getInstance().getEventBus().unregister(handler);
        }
        Engine.stop();
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_not_started_and_try_to_get_instance() {

        Engine.stop();
        Engine.getInstance();
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_start_running_engine() {

        Engine.start(InMemoryStorageFactory.getInstance());
    }

    @Test
    public void return_instance_if_started() {

        final Engine engine = Engine.getInstance();
        assertNotNull(engine);
    }

    @Test
    public void return_EventBus() {

        assertNotNull(Engine.getInstance().getEventBus());
    }

    @Test
    public void return_CommandDispatcher() {

        assertNotNull(Engine.getInstance().getCommandDispatcher());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_call_process_with_null_parameter() {
        // noinspection ConstantConditions
        Engine.getInstance().process(null);
    }

    @Test(expected = UnsupportedCommandException.class)
    public void throw_exception_if_not_register_any_repositories_and_try_to_process_command() {

        Engine.getInstance().process(createProject());
    }

    @Test
    public void register_test_repositories_and_handlers() {

        registerAll();
    }

    @Test
    public void return_true_if_started() {

        final boolean isStarted = Engine.getInstance().isStarted();
        assertTrue(isStarted);
    }

    @Test
    public void process_one_command_and_return_appropriate_result() {

        registerAll();
        final CommandRequest request = createProject(userId, projectId, getCurrentTime());

        final CommandResult result = Engine.getInstance().process(request);

        assertCommandResultsAreValid(newArrayList(request), newArrayList(result));
    }

    @Test
    public void process_several_commands_and_return_appropriate_results() {

        registerAll();
        final List<CommandRequest> requests = generateRequests();

        final List<CommandResult> results = processRequests(requests);

        assertCommandResultsAreValid(requests, results);
    }

    private void assertCommandResultsAreValid(List<CommandRequest> requests, List<CommandResult> results) {

        assertEquals(requests.size(), results.size());

        for (int i = 0; i < requests.size(); i++) {
            assertRequestAndResultMatch(requests.get(i), results.get(i));
        }
    }

    private void assertRequestAndResultMatch(CommandRequestOrBuilder request, CommandResultOrBuilder result) {

        final Timestamp expectedTime = request.getContext().getCommandId().getTimestamp();

        final List<EventRecord> records = result.getEventRecordList();
        assertEquals(1, records.size());
        final EventRecord actualRecord = records.get(0);
        final ProjectId actualProjectId = fromAny(actualRecord.getContext().getAggregateId());
        final CommandId actualCommandId = actualRecord.getContext().getEventId().getCommandId();

        assertEquals(projectId, actualProjectId);
        assertEquals(userId, actualCommandId.getActor());
        assertEquals(expectedTime, actualCommandId.getTimestamp());
    }

    /**
     * Registers all test repositories, handlers etc.
     */
    private void registerAll() {

        final Engine engine = Engine.getInstance();
        engine.register(new ProjectAggregateRepository());
        engine.register(new TestEntityRepository());
        engine.getEventBus().register(handler);
        handlersRegistered = true;
    }

    private static List<CommandResult> processRequests(Iterable<CommandRequest> requests) {

        final List<CommandResult> results = newLinkedList();
        for (CommandRequest request : requests) {
            final CommandResult result = Engine.getInstance().process(request);
            results.add(result);
        }
        return results;
    }

    private List<CommandRequest> generateRequests() {

        final Duration delta = seconds(10);
        final Timestamp time1 = getCurrentTime();
        final Timestamp time2 = add(time1, delta);
        final Timestamp time3 = add(time2, delta);

        final CommandRequest createProject = createProject(userId, projectId, time1);
        final CommandRequest addTask = addTask(userId, projectId, time2);
        final CommandRequest startProject = startProject(userId, projectId, time3);

        return newArrayList(createProject, addTask, startProject);
    }

    private static class ProjectAggregateRepository extends AggregateRepositoryBase<ProjectId, AggregateShould.ProjectAggregate> {
    }

    private static class TestEntityRepository extends EntityRepository<String, TestEntity, Project> {
    }

    public static class TestEntity extends Entity<String, Project> {
        public TestEntity(String id) {
            super(id);
        }
        @Nonnull
        @Override
        protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    private static class EmptyHandler {

        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
        }

        @Subscribe
        public void on(TaskAdded event, EventContext context) {
        }

        @Subscribe
        public void on(ProjectStarted event, EventContext context) {
        }
    }
}
