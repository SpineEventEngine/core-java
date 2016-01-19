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

import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.*;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.*;
import org.spine3.client.ClientUtil;
import org.spine3.client.CommandRequest;
import org.spine3.eventbus.EventBus;
import org.spine3.eventbus.Subscribe;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.aggregate.AggregateShould;
import org.spine3.server.error.UnsupportedCommandException;
import org.spine3.server.procman.ProcessManager;
import org.spine3.server.procman.ProcessManagerRepository;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.stream.EventStore;
import org.spine3.server.stream.StreamProjection;
import org.spine3.server.stream.StreamProjectionRepository;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;
import org.spine3.testdata.TestAggregateIdFactory;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.protobuf.util.TimeUtil.add;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.spine3.protobuf.Durations.seconds;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.testdata.TestCommandFactory.*;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods", "OverlyCoupledClass"})
public class BoundedContextShould {

    private final UserId userId = ClientUtil.newUserId("test_user");
    private final ProjectId projectId = TestAggregateIdFactory.createProjectId("test_project_id");
    private final EmptyHandler handler = new EmptyHandler();

    private StorageFactory storageFactory;
    private BoundedContext boundedContext;
    private boolean handlersRegistered = false;

    @Before
    public void setUp() {
        storageFactory = InMemoryStorageFactory.getInstance();
        boundedContext = BoundedContextTestStubs.create(storageFactory);
    }

    private static EventBus newEventBus(StorageFactory storageFactory) {
        return EventBus.newInstance(EventStore.newBuilder()
            .setStreamExecutor(MoreExecutors.directExecutor())
            .setStorage(storageFactory.createEventStorage())
            .build());
    }

    private static CommandDispatcher newCommandDispatcher(StorageFactory storageFactory) {
        return CommandDispatcher.create(new CommandStore(storageFactory.createCommandStorage()));
    }

    @After
    public void tearDown() throws Exception {
        if (handlersRegistered) {
            boundedContext.getEventBus().unregister(handler);
        }
        boundedContext.close();
    }

    /**
     * Registers all test repositories, handlers etc.
     */
    private void registerAll() {
        boundedContext.register(new ProjectAggregateRepository(boundedContext));
        boundedContext.getEventBus().register(handler);
        handlersRegistered = true;
    }

    private List<CommandResult> processRequests(Iterable<CommandRequest> requests) {

        final List<CommandResult> results = newLinkedList();
        for (CommandRequest request : requests) {
            final CommandResult result = boundedContext.process(request);
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

    @Test
    public void return_instance_if_started() {
        assertNotNull(boundedContext);
    }

    @Test
    public void return_EventBus() {
        assertNotNull(boundedContext.getEventBus());
    }

    @Test
    public void return_CommandDispatcher() {
        assertNotNull(boundedContext.getCommandDispatcher());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_call_process_with_null_parameter() {
        // noinspection ConstantConditions
        boundedContext.process(null);
    }

    @Test(expected = UnsupportedCommandException.class)
    public void throw_exception_if_not_register_any_repositories_and_try_to_process_command() {
        boundedContext.process(createProject());
    }

    @Test
    public void register_AggregateRepository() {
        boundedContext.register(new ProjectAggregateRepository(boundedContext));
    }

    @Test
    public void register_ProcessManagerRepository() {
        boundedContext.register(new ProjectPmRepo(boundedContext));
    }

    @Test
    public void register_ProjectionRepository() {
        boundedContext.register(new ProjectReportRepository(boundedContext));
    }

    @Test
    public void process_one_command_and_return_appropriate_result() {
        registerAll();
        final CommandRequest request = createProject(userId, projectId, getCurrentTime());

        final CommandResult result = boundedContext.process(request);

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

    private void assertRequestAndResultMatch(CommandRequest request, CommandResult result) {

        final Timestamp expectedTime = request.getContext().getTimestamp();

        final List<EventRecord> records = result.getEventRecordList();
        assertEquals(1, records.size());
        final EventRecord actualRecord = records.get(0);
        final ProjectId actualProjectId = fromAny(actualRecord.getContext().getAggregateId());

        assertEquals(projectId, actualProjectId);
        assertEquals(userId, actualRecord.getContext().getCommandContext().getActor());
        assertEquals(expectedTime, actualRecord.getContext().getCommandContext().getTimestamp());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_StorageFactory() {
        //noinspection ConstantConditions
        BoundedContext.newBuilder().setStorageFactory(null);
    }

    @Test
    public void return_StorageFactory_from_builder() {
        final StorageFactory sf = InMemoryStorageFactory.getInstance();
        final BoundedContext.Builder builder = BoundedContext.newBuilder().setStorageFactory(sf);
        assertEquals(sf, builder.getStorageFactory());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_CommandDispatcher() {
        //noinspection ConstantConditions
        BoundedContext.newBuilder().setCommandDispatcher(null);
    }

    @Test
    public void return_CommandDispatcher_from_builder() {
        final CommandDispatcher expected = newCommandDispatcher(storageFactory);
        final BoundedContext.Builder builder = BoundedContext.newBuilder().setCommandDispatcher(expected);
        assertEquals(expected, builder.getCommandDispatcher());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_EventBus() {
        //noinspection ConstantConditions
        BoundedContext.newBuilder().setEventBus(null);
    }

    @Test
    public void return_EventBus_from_builder() {
        final EventBus expected = newEventBus(storageFactory);
        final BoundedContext.Builder builder = BoundedContext.newBuilder().setEventBus(expected);
        assertEquals(expected, builder.getEventBus());
    }

    private static class ResponseObserver implements StreamObserver<Response> {

        private Response response;

        @Override
        public void onNext(Response commandResponse) {
            this.response = commandResponse;
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onCompleted() {
        }

        public Response getResponse() {
            return response;
        }
    }

    @Test
    public void verify_namespace_attribute_if_multitenant() {
        final BoundedContext bc = BoundedContext.newBuilder()
                .setStorageFactory(InMemoryStorageFactory.getInstance())
                .setCommandDispatcher(newCommandDispatcher(storageFactory))
                .setEventBus(newEventBus(storageFactory))
                .setMultitenant(true)
                .build();

        final ResponseObserver observer = new ResponseObserver();

        final CommandRequest request = CommandRequest.newBuilder()
                // Pass empty command so that we have something valid to unpack in the context.
                .setCommand(Any.pack(StringValue.getDefaultInstance()))
                .build();
        bc.post(request, observer);

        assertEquals(CommandValidationError.NAMESPACE_UNKNOWN.getNumber(), observer.getResponse().getError().getCode());
    }

    private static class ProjectAggregateRepository extends AggregateRepository<ProjectId, AggregateShould.ProjectAggregate> {
        private ProjectAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    @SuppressWarnings("UnusedParameters") // It is intended in this empty handler class.
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

    private static class ProjectProcessManager extends ProcessManager<ProjectId, Empty> {

        @SuppressWarnings("PublicConstructorInNonPublicClass")
        // Constructor must be public to be called from a repository. It's a part of PM public API.
        public ProjectProcessManager(ProjectId id) {
            super(id);
        }

        @Override
        protected Empty getDefaultState() {
            return Empty.getDefaultInstance();
        }
    }

    private static class ProjectPmRepo extends ProcessManagerRepository<ProjectId, ProjectProcessManager, Empty> {
        private ProjectPmRepo(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    private static class ProjectReport extends StreamProjection<ProjectId, Empty> {

        @SuppressWarnings("PublicConstructorInNonPublicClass")
        // Public constructor is a part of projection public API. It's called by a repository.
        public ProjectReport(ProjectId id) {
            super(id);
        }

        @Override
        protected Empty getDefaultState() {
            return Empty.getDefaultInstance();
        }
    }

    private static class ProjectReportRepository extends StreamProjectionRepository<ProjectId, ProjectReport, Empty> {
        protected ProjectReportRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }
}
