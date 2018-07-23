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

package io.spine.server.aggregate;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.spine.base.Time;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.Given;
import io.spine.server.aggregate.given.aggregate.AggregateWithMissingApplier;
import io.spine.server.aggregate.given.aggregate.AmishAggregate;
import io.spine.server.aggregate.given.aggregate.FaultyAggregate;
import io.spine.server.aggregate.given.aggregate.IntAggregate;
import io.spine.server.aggregate.given.aggregate.TaskAggregate;
import io.spine.server.aggregate.given.aggregate.TaskAggregateRepository;
import io.spine.server.aggregate.given.aggregate.TestAggregate;
import io.spine.server.aggregate.given.aggregate.TestAggregateRepository;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.DuplicateCommandException;
import io.spine.server.model.Model;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggAssignTask;
import io.spine.test.aggregate.command.AggCancelProject;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggPauseProject;
import io.spine.test.aggregate.command.AggReassignTask;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.command.ImportEvents;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.test.aggregate.event.AggTaskAssigned;
import io.spine.test.aggregate.event.AggUserNotified;
import io.spine.test.aggregate.rejection.Rejections;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.model.ModelTests;
import io.spine.time.testing.TimeTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.collect.Lists.newArrayList;
import static io.spine.core.CommandEnvelope.of;
import static io.spine.core.Events.getRootCommandId;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;
import static io.spine.server.aggregate.given.Given.EventMessage.projectStarted;
import static io.spine.server.aggregate.given.Given.EventMessage.taskAdded;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.assignTask;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.cannotModifyDeletedEntity;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.command;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.createTask;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.env;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.event;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.newTenantId;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.reassignTask;
import static io.spine.testing.client.blackbox.AcknowledgementsVerifier.acked;
import static io.spine.testing.client.blackbox.Count.once;
import static io.spine.testing.client.blackbox.Count.twice;
import static io.spine.testing.server.Assertions.assertCommandClasses;
import static io.spine.testing.server.Assertions.assertEventClasses;
import static io.spine.testing.server.aggregate.AggregateMessageDispatcher.dispatchCommand;
import static io.spine.testing.server.aggregate.AggregateMessageDispatcher.dispatchRejection;
import static io.spine.testing.server.blackbox.EmittedEventsVerifier.emitted;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukkov
 */
@SuppressWarnings({
        "InnerClassMayBeStatic", "ClassCanBeStatic" /* JUnit nested classes cannot be static. */,
})
@DisplayName("Aggregate should")
public class AggregateTest {

    private static final ProjectId ID = ProjectId.newBuilder()
                                                 .setId("prj-01")
                                                 .build();

    private static final AggCreateProject createProject = Given.CommandMessage.createProject(ID);
    private static final AggPauseProject pauseProject = Given.CommandMessage.pauseProject(ID);
    private static final AggCancelProject cancelProject = Given.CommandMessage.cancelProject(ID);
    private static final AggAddTask addTask = Given.CommandMessage.addTask(ID);
    private static final AggStartProject startProject = Given.CommandMessage.startProject(ID);

    private TestAggregate aggregate;
    private AmishAggregate amishAggregate;
    private BoundedContext boundedContext;
    private TestAggregateRepository repository;

    private static TestAggregate newAggregate(ProjectId id) {
        TestAggregate result = new TestAggregate(id);
        result.init();
        return result;
    }

    private static AmishAggregate newAmishAggregate(ProjectId id) {
        AmishAggregate result = new AmishAggregate(id);
        result.init();
        return result;
    }

    private static List<Event> generateProjectEvents() {
        String projectName = AggregateTest.class.getSimpleName();
        List<Event> events = ImmutableList.<Event>builder()
                .add(event(projectCreated(ID, projectName), 1))
                .add(event(taskAdded(ID), 3))
                .add(event(projectStarted(ID), 4))
                .build();
        return events;
    }

    /**
     * Casts {@linkplain TestAggregate the aggregate under the test} to {@link Aggregate},
     * class, which is in the same package with this test, so that we call package-access methods.
     */
    private Aggregate<?, ?, ?> aggregate() {
        return aggregate;
    }

    @BeforeEach
    void setUp() {
        ModelTests.clearModel();
        aggregate = newAggregate(ID);
        amishAggregate = newAmishAggregate(ID);
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();

        repository = new TestAggregateRepository();
        boundedContext.register(repository);
    }

    @AfterEach
    void tearDown() throws Exception {
        boundedContext.close();
    }

    @Nested
    @DisplayName("expose")
    class Expose {

        @Test
        @DisplayName("handled command classes")
        void handledCommandClasses() {
            Set<CommandClass> commandClasses =
                    Model.getInstance()
                         .asAggregateClass(TestAggregate.class)
                         .getCommands();

            assertEquals(4, commandClasses.size());

            assertCommandClasses(commandClasses,
                                 AggCreateProject.class,
                                 AggAddTask.class,
                                 AggStartProject.class,
                                 ImportEvents.class);
        }

        @Test
        @DisplayName("current state")
        void currentState() {
            dispatchCommand(aggregate, env(createProject));
            assertEquals(Status.CREATED, aggregate.getState()
                                                  .getStatus());

            dispatchCommand(aggregate, env(startProject));
            assertEquals(Status.STARTED, aggregate.getState()
                                                  .getStatus());
        }

        @Test
        @DisplayName("non-null last modification time")
        void timeLastModified() {
            Timestamp creationTime = new TestAggregate(ID).whenModified();
            assertNotNull(creationTime);
        }
    }

    @Test
    @DisplayName("handle one command and apply appropriate event")
    void handleCommandProperly() {
        dispatchCommand(aggregate, env(createProject));

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);
    }

    @Nested
    @DisplayName("advance version")
    class AdvanceVersion {

        @Test
        @DisplayName("by one upon handling command with one event")
        void byOne() {
            int version = aggregate.versionNumber();

            dispatchCommand(aggregate, env(createProject));

            assertEquals(version + 1, aggregate.versionNumber());
        }

        /**
         * This is a most typical use-case with a single event returned in response to a command.
         */
        @Test
        @DisplayName("by one upon handling command with single event and empty event applier")
        void byOneForEmptyApplier() {
            int version = amishAggregate.versionNumber();

            List<? extends Message> messages = dispatchCommand(amishAggregate, env(pauseProject));
            assertEquals(1, messages.size());

            assertEquals(version + 1, amishAggregate.versionNumber());
        }

        /**
         * This tests a use-case implying returning a {@code List} of events in response to a command.
         */
        @Test
        @DisplayName("by number of events upon handling command with several events")
        void byNumberOfEvents() {
            int version = amishAggregate.versionNumber();

            List<? extends Message> eventMessages =
                    dispatchCommand(amishAggregate, env(cancelProject));
            // Expecting to return more than one to differ from other testing scenarios.
            assertTrue(eventMessages.size() > 1);

            assertEquals(version + eventMessages.size(), amishAggregate.versionNumber());
        }

        @Test
        @DisplayName("by number of commands upon handling several commands")
        void byNumberOfCommands() {
            int version = aggregate.versionNumber();

            dispatchCommand(aggregate, env(createProject));
            dispatchCommand(aggregate, env(startProject));
            dispatchCommand(aggregate, env(addTask));

            assertEquals(version + 3, aggregate.versionNumber());
        }
    }

    @Test
    @DisplayName("write its version into event context")
    void writeVersionIntoEventContext() {
        dispatchCommand(aggregate, env(createProject));

        // Get the first event since the command handler produces only one event message.
        Aggregate<?, ?, ?> agg = this.aggregate;
        List<Event> uncommittedEvents = agg.getUncommittedEvents();
        Event event = uncommittedEvents.get(0);

        assertEquals(this.aggregate.getVersion(), event.getContext()
                                                       .getVersion());
    }

    @Test
    @DisplayName("handle only dispatched commands")
    void handleOnlyDispatchedCommands() {
        dispatchCommand(aggregate, env(createProject));

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);

        assertFalse(aggregate.isAddTaskCommandHandled);
        assertFalse(aggregate.isTaskAddedEventApplied);

        assertFalse(aggregate.isStartProjectCommandHandled);
        assertFalse(aggregate.isProjectStartedEventApplied);
    }

    @Test
    @DisplayName("invoke event applier after command handler")
    void invokeApplierAfterCommandHandler() {
        dispatchCommand(aggregate, env(createProject));
        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);

        dispatchCommand(aggregate, env(addTask));
        assertTrue(aggregate.isAddTaskCommandHandled);
        assertTrue(aggregate.isTaskAddedEventApplied);

        dispatchCommand(aggregate, env(startProject));
        assertTrue(aggregate.isStartProjectCommandHandled);
        assertTrue(aggregate.isProjectStartedEventApplied);
    }

    @Nested
    @DisplayName("react on rejection")
    class ReactOnRejection {

        @Test
        @DisplayName("by rejection message")
        void byRejectionMessage() {
            dispatchRejection(aggregate, cannotModifyDeletedEntity(StringValue.class));
            assertTrue(aggregate.isRejectionHandled);
            assertFalse(aggregate.isRejectionWithCmdHandled);
        }

        @Test
        @DisplayName("by rejection and command message")
        void byRejectionAndCommandMessage() {
            dispatchRejection(aggregate, cannotModifyDeletedEntity(AggAddTask.class));
            assertTrue(aggregate.isRejectionWithCmdHandled);
            assertFalse(aggregate.isRejectionHandled);
        }
    }

    @Nested
    @DisplayName("throw when missing")
    class ThrowOnMissing {

        @Test
        @DisplayName("command handler")
        void commandHandler() {
            ModelTests.clearModel();
            AggregateWithMissingApplier aggregate = new AggregateWithMissingApplier(ID);

            // Pass a command for which the target aggregate does not have a handling method.
            assertThrows(IllegalStateException.class,
                         () -> dispatchCommand(aggregate, env(addTask)));
        }

        @Test
        @DisplayName("event applier for the event emitted in a result of command handling")
        void eventApplier() {
            ModelTests.clearModel();
            AggregateWithMissingApplier aggregate =
                    new AggregateWithMissingApplier(ID);
            assertThrows(IllegalStateException.class, () -> {
                try {
                    dispatchCommand(aggregate, env(createProject));
                } catch (IllegalStateException e) { // expected exception
                    assertTrue(aggregate.isCreateProjectCommandHandled());
                    throw e;
                }
            });
        }
    }

    @Nested
    @DisplayName("have state")
    class HaveState {

        @Test
        @DisplayName("default on creation")
        void defaultOnCreation() {
            Project state = aggregate.getState();

            assertEquals(aggregate.getDefaultState(), state);
        }

        @Test
        @DisplayName("updated when command is handled")
        void updatedUponCommandHandled() {
            dispatchCommand(aggregate, env(createProject));

            Project state = aggregate.getState();

            assertEquals(ID, state.getId());
            assertEquals(Status.CREATED, state.getStatus());
        }
    }

    @Test
    @DisplayName("record modification time when command is handled")
    void recordModificationUponCommandHandled() {
        try {
            Timestamp frozenTime = Time.getCurrentTime();
            Time.setProvider(new TimeTests.FrozenMadHatterParty(frozenTime));

            dispatchCommand(aggregate, env(createProject));

            assertEquals(frozenTime, aggregate.whenModified());
        } finally {
            Time.resetProvider();
        }
    }

    @Test
    @DisplayName("play events")
    void playEvents() {
        List<Event> events = generateProjectEvents();
        AggregateStateRecord aggregateStateRecord =
                AggregateStateRecord.newBuilder()
                                    .addAllEvent(events)
                                    .build();

        AggregateTransaction tx = AggregateTransaction.start(aggregate);
        aggregate().play(aggregateStateRecord);
        tx.commit();

        assertTrue(aggregate.isProjectCreatedEventApplied);
        assertTrue(aggregate.isTaskAddedEventApplied);
        assertTrue(aggregate.isProjectStartedEventApplied);
    }

    @Test
    @DisplayName("restore snapshot during play")
    void restoreSnapshot() {
        dispatchCommand(aggregate, env(createProject));

        Snapshot snapshot = aggregate().toShapshot();

        Aggregate anotherAggregate = newAggregate(aggregate.getId());

        AggregateTransaction tx = AggregateTransaction.start(anotherAggregate);
        anotherAggregate.play(AggregateStateRecord.newBuilder()
                                                  .setSnapshot(snapshot)
                                                  .build());
        tx.commit();

        assertEquals(aggregate, anotherAggregate);
    }

    @Nested
    @DisplayName("after dispatch, return event records")
    class ReturnEventRecords {

        @Test
        @DisplayName("which are uncommitted")
        void uncommitedAfterDispatch() {
            aggregate.dispatchCommands(command(createProject),
                                       command(addTask),
                                       command(startProject));

            List<Event> events = aggregate().getUncommittedEvents();

            assertEventClasses(getEventClasses(events),
                               AggProjectCreated.class, AggTaskAdded.class, AggProjectStarted.class);
        }

        @Test
        @DisplayName("which are being committed")
        void beingCommitedAfterDispatch() {
            aggregate.dispatchCommands(command(createProject),
                                       command(addTask),
                                       command(startProject));

            List<Event> events = aggregate().commitEvents();

            assertEventClasses(getEventClasses(events),
                               AggProjectCreated.class, AggTaskAdded.class, AggProjectStarted.class);
        }

        private Collection<EventClass> getEventClasses(Collection<Event> events) {
            List<EventClass> result =
                    events.stream()
                          .map(EventClass::of)
                          .collect(toList());
            return result;
        }

    }

    @Nested
    @DisplayName("by default, not have any event records")
    class NotHaveEventRecords {

        @Test
        @DisplayName("which are uncommitted")
        void uncommitedByDefault() {
            List<Event> events = aggregate().getUncommittedEvents();

            assertTrue(events.isEmpty());
        }

        @Test
        @DisplayName("which are being committed")
        void beingCommitedByDefault() {
            List<Event> events = aggregate().commitEvents();

            assertTrue(events.isEmpty());
        }
    }

    @Test
    @DisplayName("clear event records when commit after dispatch")
    void clearEventsWhenCommitAfterDispatch() {
        aggregate.dispatchCommands(command(createProject),
                                   command(addTask),
                                   command(startProject));

        List<Event> events = aggregate().commitEvents();
        assertFalse(events.isEmpty());

        List<Event> emptyList = aggregate().commitEvents();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    @DisplayName("transform current state to snapshot event")
    void transformCurrentStateToSnapshot() {

        dispatchCommand(aggregate, env(createProject));

        Snapshot snapshot = aggregate().toShapshot();
        Project state = unpack(snapshot.getState());

        assertEquals(ID, state.getId());
        assertEquals(Status.CREATED, state.getStatus());
    }

    @Test
    @DisplayName("restore state from snapshot")
    void restoreStateFromSnapshot() {

        dispatchCommand(aggregate, env(createProject));

        Snapshot snapshotNewProject = aggregate().toShapshot();

        Aggregate anotherAggregate = newAggregate(aggregate.getId());

        AggregateTransaction tx = AggregateTransaction.start(anotherAggregate);
        anotherAggregate.restore(snapshotNewProject);
        tx.commit();

        assertEquals(aggregate.getState(), anotherAggregate.getState());
        assertEquals(aggregate.getVersion(), anotherAggregate.getVersion());
        assertEquals(aggregate.getLifecycleFlags(), anotherAggregate.getLifecycleFlags());
    }

    @Test
    @DisplayName("import events")
    void importEvents() {
        String projectName = getClass().getSimpleName();
        ProjectId id = aggregate.getId();
        ImportEvents importCmd =
                ImportEvents.newBuilder()
                            .setProjectId(id)
                            .addEvent(event(projectCreated(id, projectName), 1))
                            .addEvent(event(taskAdded(id), 2))
                            .build();
        aggregate.dispatchCommands(command(importCmd));

        assertTrue(aggregate.isProjectCreatedEventApplied);
        assertTrue(aggregate.isTaskAddedEventApplied);
    }

    @Test
    @DisplayName("increment version upon state changing event applied")
    void incrementVersionOnEventApplied() {
        int version = aggregate.getVersion()
                               .getNumber();
        // Dispatch two commands that cause events that modify aggregate state.
        aggregate.dispatchCommands(command(createProject), command(startProject));

        assertEquals(version + 2, aggregate.getVersion()
                                           .getNumber());
    }

    @Test
    @DisplayName("record modification timestamp")
    void recordModificationTimestamp() {
        try {
            TimeTests.BackToTheFuture provider = new TimeTests.BackToTheFuture();
            Time.setProvider(provider);

            Timestamp currentTime = Time.getCurrentTime();

            aggregate.dispatchCommands(command(createProject));

            assertEquals(currentTime, aggregate.whenModified());

            currentTime = provider.forward(10);

            aggregate.dispatchCommands(command(startProject));

            assertEquals(currentTime, aggregate.whenModified());
        } finally {
            Time.resetProvider();
        }
    }

    @SuppressWarnings("NonExceptionNameEndsWithException") // OK for test case name.
    @Nested
    @DisplayName("propagate RuntimeException")
    class PropagateRuntimeException {

        @Test
        @DisplayName("when handler throws")
        void whenHandlerThrows() {
            ModelTests.clearModel();
            FaultyAggregate faultyAggregate = new FaultyAggregate(ID, true, false);

            Command command = Given.ACommand.createProject();
            try {
                dispatchCommand(faultyAggregate, env(command.getMessage()));
                failNotThrows();
            } catch (RuntimeException e) {
                Throwable cause = getRootCause(e);
                assertTrue(cause instanceof IllegalStateException);
                assertEquals(FaultyAggregate.BROKEN_HANDLER, cause.getMessage());
            }
        }

        @Test
        @DisplayName("when applier throws")
        void whenApplierThrows() {
            ModelTests.clearModel();
            FaultyAggregate faultyAggregate =
                    new FaultyAggregate(ID, false, true);

            Command command = Given.ACommand.createProject();
            try {
                dispatchCommand(faultyAggregate, env(command.getMessage()));
                failNotThrows();
            } catch (RuntimeException e) {
                Throwable cause = getRootCause(e);
                assertTrue(cause instanceof IllegalStateException);
                assertEquals(FaultyAggregate.BROKEN_APPLIER, cause.getMessage());
            }
        }

        @Test
        @DisplayName("when play raises exception")
        void whenPlayThrows() {
            ModelTests.clearModel();
            FaultyAggregate faultyAggregate =
                    new FaultyAggregate(ID, false, true);
            try {
                Event event = event(projectCreated(ID, getClass().getSimpleName()), 1);

                AggregateTransaction tx = AggregateTransaction.start(faultyAggregate);
                ((Aggregate) faultyAggregate).play(AggregateStateRecord.newBuilder()
                                                                       .addEvent(event)
                                                                       .build());
                tx.commit();
                failNotThrows();
            } catch (RuntimeException e) {
                Throwable cause = getRootCause(e);
                assertTrue(cause instanceof IllegalStateException);
                assertEquals(FaultyAggregate.BROKEN_APPLIER, cause.getMessage());
            }
        }

        private void failNotThrows() {
            fail("Should have thrown RuntimeException.");
        }
    }

    @Test
    @DisplayName("not allow getting state builder from outside event applier")
    void notGetStateBuilderOutsideOfApplier() {
        assertThrows(IllegalStateException.class, () -> new IntAggregate(100).getBuilder());
    }

    @Nested
    @DisplayName("traverse history")
    class TraverseHistory {

        @Test
        @DisplayName("iterating through newest events first")
        void throughNewestEventsFirst() {
            TenantId tenantId = newTenantId();
            Command createCommand = command(createProject, tenantId);
            Command startCommand = command(startProject, tenantId);
            Command addTaskCommand = command(addTask, tenantId);
            Command addTaskCommand2 = command(addTask, tenantId);

            CommandBus commandBus = boundedContext.getCommandBus();
            StreamObserver<Ack> noOpObserver = noOpObserver();
            commandBus.post(createCommand, noOpObserver);
            commandBus.post(addTaskCommand, noOpObserver);
            commandBus.post(newArrayList(addTaskCommand2, startCommand), noOpObserver);

            TestAggregate aggregate = repository.loadAggregate(tenantId, ID);

            Iterator<Event> history = aggregate.historyBackward();

            assertEquals(startCommand.getId(), getRootCommandId(history.next()));
            assertEquals(addTaskCommand2.getId(), getRootCommandId(history.next()));
            assertEquals(addTaskCommand.getId(), getRootCommandId(history.next()));
            assertEquals(createCommand.getId(), getRootCommandId(history.next()));
            assertFalse(history.hasNext());
        }

        @Test
        @DisplayName("up to latest snapshot")
        void upToLatestSnapshot() {
            repository.setSnapshotTrigger(3);

            TenantId tenantId = newTenantId();
            Command createCommand = command(createProject, tenantId);
            Command startCommand = command(startProject, tenantId);
            Command addTaskCommand = command(addTask, tenantId);
            Command addTaskCommand2 = command(addTask, tenantId);

            CommandBus commandBus = boundedContext.getCommandBus();
            StreamObserver<Ack> noOpObserver = noOpObserver();
            commandBus.post(createCommand, noOpObserver);
            commandBus.post(startCommand, noOpObserver);
            commandBus.post(newArrayList(addTaskCommand, addTaskCommand2), noOpObserver);

            TestAggregate aggregate = repository.loadAggregate(tenantId, ID);

            Iterator<Event> history = aggregate.historyBackward();

            assertEquals(addTaskCommand2.getId(), getRootCommandId(history.next()));
            assertFalse(history.hasNext());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
        // We're not interested in what dispatch() returns
    @Test
    @DisplayName("throw DuplicateCommandException for a duplicated command")
    void acknowledgeExceptionForDuplicateCommand() {
        TenantId tenantId = newTenantId();
        Command createCommand = command(createProject, tenantId);
        CommandEnvelope envelope = of(createCommand);
        repository.dispatch(envelope);

        assertThrows(DuplicateCommandException.class, () -> repository.dispatch(envelope));
    }

    @Nested
    @DisplayName("create a single event when emitting a pair without second value")
    class CreateSingleEventForPair {

        /**
         * Ensures that a {@linkplain io.spine.server.tuple.Pair pair} with an empty second
         * optional value returned from a command handler stores a single event.
         *
         * <p>The command handler that should return a pair is
         * {@link TaskAggregate#handle(AggAssignTask)
         * TaskAggregate#handle(AggAssignTask)}.
         */
        @Test
        @DisplayName("when dispatching a command")
        void fromCommandDispatch() {
            BlackBoxBoundedContext
                    .with(new TaskAggregateRepository())
                    .receivesCommand(createTask())
                    .verifiesThat(acked(once()).withoutErrorsOrRejections())
                    .verifiesThat(emitted(once()))
                    .close();
        }

        /**
         * Ensures that a {@linkplain io.spine.server.tuple.Pair pair} with an empty second optional
         * value returned from a reaction on an event stores a single event.
         *
         * <p>The first event is produced while handling a command by the
         * {@link TaskAggregate#handle(AggAssignTask) TaskAggregate#handle(AggAssignTask)}.
         * Then as a reaction to this event a single event should be fired as part of the pair by
         * {@link TaskAggregate#on(AggTaskAssigned) TaskAggregate#on(AggTaskAssigned)}.
         */
        @Test
        @DisplayName("when reacting on an event")
        void fromEventReact() {
            BlackBoxBoundedContext
                    .with(new TaskAggregateRepository())
                    .receivesCommand(assignTask())
                    .verifiesThat(acked(once()).withoutErrorsOrRejections())
                    .verifiesThat(emitted(twice()))
                    .verifiesThat(emitted(AggTaskAssigned.class))
                    .verifiesThat(emitted(AggUserNotified.class))
                    .close();
        }

        /**
         * Ensures that a {@linkplain io.spine.server.tuple.Pair pair} with an empty second optional
         * value returned from a reaction on a rejection stores a single event.
         *
         * <p>The rejection is fired by the {@link TaskAggregate#handle(AggReassignTask)
         * TaskAggregate.handle(AggReassignTask)}
         * and handled by the {@link TaskAggregate#on(Rejections.AggCannotReassignUnassignedTask)
         * TaskAggregate.on(AggCannotReassignUnassignedTask)}.
         */
        @Test
        @DisplayName("when reacting on a rejection")
        void fromRejectionReact() {
            BlackBoxBoundedContext
                    .with(new TaskAggregateRepository())
                    .receivesCommand(reassignTask())
                    .verifiesThat(acked(once()).withoutErrorsOrRejections())
                    .verifiesThat(emitted(once()))
                    .verifiesThat(emitted(AggUserNotified.class))
                    .close();
        }
    }
}
