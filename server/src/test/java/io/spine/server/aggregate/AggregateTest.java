/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.MessageId;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.aggregate.given.Given;
import io.spine.server.aggregate.given.aggregate.AggregateWithMissingApplier;
import io.spine.server.aggregate.given.aggregate.AmishAggregate;
import io.spine.server.aggregate.given.aggregate.FaultyAggregate;
import io.spine.server.aggregate.given.aggregate.IntAggregate;
import io.spine.server.aggregate.given.aggregate.TaskAggregate;
import io.spine.server.aggregate.given.aggregate.TaskAggregateRepository;
import io.spine.server.aggregate.given.aggregate.TestAggregate;
import io.spine.server.aggregate.given.aggregate.TestAggregateRepository;
import io.spine.server.aggregate.given.thermometer.SafeThermometer;
import io.spine.server.aggregate.given.thermometer.SafeThermometerRepo;
import io.spine.server.aggregate.given.thermometer.Thermometer;
import io.spine.server.aggregate.given.thermometer.ThermometerId;
import io.spine.server.aggregate.given.thermometer.event.TemperatureChanged;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.delivery.MessageEndpoint;
import io.spine.server.dispatch.BatchDispatchOutcome;
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.CannotDispatchDuplicateCommand;
import io.spine.system.server.CannotDispatchDuplicateEvent;
import io.spine.system.server.DiagnosticMonitor;
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
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggProjectDeleted;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.test.aggregate.event.AggTaskAssigned;
import io.spine.test.aggregate.event.AggTaskCreated;
import io.spine.test.aggregate.event.AggUserNotified;
import io.spine.test.aggregate.rejection.Rejections.AggCannotReassignUnassignedTask;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.EventSubject;
import io.spine.testing.server.blackbox.ContextAwareTest;
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
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;
import static io.spine.server.aggregate.given.Given.EventMessage.projectStarted;
import static io.spine.server.aggregate.given.Given.EventMessage.taskAdded;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.assignTask;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.command;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.createTask;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.env;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.event;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.newTenantId;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.reassignTask;
import static io.spine.server.aggregate.given.dispatch.AggregateMessageDispatcher.dispatchCommand;
import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;
import static io.spine.server.tenant.TenantAwareRunner.with;
import static io.spine.testing.server.Assertions.assertCommandClasses;
import static io.spine.testing.server.Assertions.assertEventClasses;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Aggregate should")
@SuppressWarnings({
        "InnerClassMayBeStatic", "ClassCanBeStatic" /* JUnit nested classes cannot be static. */,
        "ClassWithTooManyMethods", "OverlyCoupledClass" /* Testing a centerpiece. */
})
@MuteLogging    /* Explicitly triggering failures in tests. */
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
    private BoundedContext context;
    private TestAggregateRepository repository;

    private static TestAggregate newAggregate(ProjectId id) {
        TestAggregate result = new TestAggregate(id);
        return result;
    }

    private static AmishAggregate newAmishAggregate(ProjectId id) {
        AmishAggregate result = new AmishAggregate(id);
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
        ModelTests.dropAllModels();
        aggregate = newAggregate(ID);
        amishAggregate = newAmishAggregate(ID);
        context = BoundedContextBuilder.assumingTests(true)
                                       .build();
        repository = new TestAggregateRepository();
        context.internalAccess()
               .register(repository);
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
    }

    @Test
    @DisplayName("not allow a negative event count after last snapshot")
    void negativeEventCount() {
        Aggregate<?, ?, ?> aggregate = this.aggregate;
        assertThrows(IllegalArgumentException.class,
                     () -> aggregate.setEventCountAfterLastSnapshot(-1));
    }

    @Nested
    @DisplayName("expose")
    class Expose {

        @Test
        @DisplayName("handled command classes")
        void handledCommandClasses() {
            Set<CommandClass> commandClasses =
                    asAggregateClass(TestAggregate.class)
                            .commands();

            assertEquals(3, commandClasses.size());

            assertCommandClasses(commandClasses,
                                 AggCreateProject.class,
                                 AggAddTask.class,
                                 AggStartProject.class);
        }

        @Test
        @DisplayName("current state")
        void currentState() {
            dispatchCommand(aggregate, command(createProject));
            assertEquals(Status.CREATED, aggregate.state()
                                                  .getStatus());

            dispatchCommand(aggregate, command(startProject));
            assertEquals(Status.STARTED, aggregate.state()
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
        dispatchCommand(aggregate, command(createProject));

        assertTrue(aggregate.createProjectCommandHandled);
        assertTrue(aggregate.projectCreatedEventApplied);
    }

    @Nested
    @DisplayName("advance version")
    class AdvanceVersion {

        @Test
        @DisplayName("by one upon handling command with one event")
        void byOne() {
            int version = aggregate.versionNumber();

            dispatchCommand(aggregate, command(createProject));

            assertEquals(version + 1, aggregate.versionNumber());
        }

        /**
         * This is a most typical use-case with a single event returned in response to a command.
         */
        @Test
        @DisplayName("by one upon handling command with single event and empty event applier")
        void byOneForEmptyApplier() {
            int version = amishAggregate.versionNumber();

            Command command = command(pauseProject);
            List<? extends Message> messages = dispatchCommand(amishAggregate, command)
                    .getSuccess()
                    .getProducedEvents()
                    .getEventList();
            assertEquals(1, messages.size());

            assertEquals(version + 1, amishAggregate.versionNumber());
        }

        /**
         * This tests a use-case implying returning a {@code List} of events in response to a
         * command.
         */
        @Test
        @DisplayName("by number of events upon handling command with several events")
        void byNumberOfEvents() {
            int version = amishAggregate.versionNumber();

            Command command = command(cancelProject);
            List<? extends Message> eventMessages =
                    dispatchCommand(amishAggregate, command)
                            .getSuccess()
                            .getProducedEvents()
                            .getEventList();
            // Expecting to return more than one to differ from other testing scenarios.
            assertTrue(eventMessages.size() > 1);

            assertEquals(version + eventMessages.size(), amishAggregate.versionNumber());
        }

        @Test
        @DisplayName("by number of commands upon handling several commands")
        void byNumberOfCommands() {
            int version = aggregate.versionNumber();

            dispatchCommand(aggregate, command(createProject));
            dispatchCommand(aggregate, command(startProject));
            dispatchCommand(aggregate, command(addTask));

            assertEquals(version + 3, aggregate.versionNumber());
        }
    }

    @Test
    @DisplayName("write its version into event context")
    void writeVersionIntoEventContext() {
        dispatchCommand(aggregate, command(createProject));

        // Get the first event since the command handler produces only one event message.
        Aggregate<?, ?, ?> agg = aggregate;
        List<Event> uncommittedEvents = agg.getUncommittedEvents()
                                           .list();
        Event event = uncommittedEvents.get(0);
        EventContext context = event.context();
        assertThat(aggregate.version())
                .isEqualTo(context.getVersion());
    }

    @Test
    @DisplayName("handle only dispatched commands")
    void handleOnlyDispatchedCommands() {
        dispatchCommand(aggregate, command(createProject));

        assertTrue(aggregate.createProjectCommandHandled);
        assertTrue(aggregate.projectCreatedEventApplied);

        assertFalse(aggregate.addTaskCommandHandled);
        assertFalse(aggregate.taskAddedEventApplied);

        assertFalse(aggregate.startProjectCommandHandled);
        assertFalse(aggregate.projectStartedEventApplied);
    }

    @Test
    @DisplayName("invoke event applier after command handler")
    void invokeApplierAfterCommandHandler() {
        dispatchCommand(aggregate, command(createProject));
        assertTrue(aggregate.createProjectCommandHandled);
        assertTrue(aggregate.projectCreatedEventApplied);

        dispatchCommand(aggregate, command(addTask));
        assertTrue(aggregate.addTaskCommandHandled);
        assertTrue(aggregate.taskAddedEventApplied);

        dispatchCommand(aggregate, command(startProject));
        assertTrue(aggregate.startProjectCommandHandled);
        assertTrue(aggregate.projectStartedEventApplied);
    }

    @Nested
    @DisplayName("throw when missing")
    class ThrowOnMissing {

        @Test
        @DisplayName("command handler")
        void commandHandler() {
            ModelTests.dropAllModels();
            AggregateWithMissingApplier aggregate = new AggregateWithMissingApplier(ID);

            // Pass a command for which the target aggregate does not have a handling method.
            assertThrows(IllegalStateException.class,
                         () -> dispatchCommand(aggregate, command(addTask)));
        }

        @Test
        @DisplayName("event applier for the event emitted in a result of command handling")
        void eventApplier() {
            ModelTests.dropAllModels();
            AggregateWithMissingApplier aggregate =
                    new AggregateWithMissingApplier(ID);
            Command command = command(createProject);
            DispatchOutcome outcome = dispatchCommand(aggregate, command);
            assertTrue(aggregate.commandHandled());
            assertTrue(outcome.hasError());
            Error error = outcome.getError();
            assertThat(error.getType())
                    .isEqualTo(IllegalStateException.class.getCanonicalName());
        }
    }

    @Nested
    @DisplayName("have state")
    class HaveState {

        @Test
        @DisplayName("updated when command is handled")
        void updatedUponCommandHandled() {
            dispatchCommand(aggregate, command(createProject));

            Project state = aggregate.state();

            assertEquals(ID, state.getId());
            assertEquals(Status.CREATED, state.getStatus());
        }
    }

    @Test
    @DisplayName("record modification time when command is handled")
    void recordModificationUponCommandHandled() {
        try {
            Timestamp frozenTime = Time.currentTime();
            Time.setProvider(new TimeTests.FrozenMadHatterParty(frozenTime));

            dispatchCommand(aggregate, command(createProject));

            assertEquals(frozenTime, aggregate.whenModified());
        } finally {
            Time.resetProvider();
        }
    }

    @Test
    @DisplayName("play events")
    void playEvents() {
        List<Event> events = generateProjectEvents();
        AggregateHistory aggregateHistory =
                AggregateHistory.newBuilder()
                                .addAllEvent(events)
                                .build();

        AggregateTransaction<?, ?, ?> tx = AggregateTransaction.start(aggregate);
        aggregate().play(aggregateHistory);
        tx.commit();

        assertTrue(aggregate.projectCreatedEventApplied);
        assertTrue(aggregate.taskAddedEventApplied);
        assertTrue(aggregate.projectStartedEventApplied);
    }

    @Test
    @DisplayName("restore snapshot during play")
    void restoreSnapshot() {
        dispatchCommand(aggregate, command(createProject));

        Snapshot snapshot = aggregate().toSnapshot();

        Aggregate<?, ?, ?> anotherAggregate = newAggregate(aggregate.id());

        AggregateTransaction<?, ?, ?> tx = AggregateTransaction.start(anotherAggregate);
        anotherAggregate.play(AggregateHistory.newBuilder()
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

            List<Event> events = aggregate().getUncommittedEvents()
                                            .list();

            assertEventClasses(getEventClasses(events),
                               AggProjectCreated.class, AggTaskAdded.class,
                               AggProjectStarted.class);
        }

        @Test
        @DisplayName("which are being committed")
        void beingCommitedAfterDispatch() {
            aggregate.dispatchCommands(command(createProject),
                                       command(addTask),
                                       command(startProject));
            aggregate().commitEvents();
            ImmutableList<Event> historyBackward =
                    ImmutableList.copyOf(aggregate().historyBackward());
            assertEventClasses(
                    getEventClasses(historyBackward),
                    AggProjectCreated.class, AggTaskAdded.class, AggProjectStarted.class
            );
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
            UncommittedEvents events = aggregate().getUncommittedEvents();

            assertFalse(events.nonEmpty());
        }

        @Test
        @DisplayName("which are being committed")
        void beingCommittedByDefault() {
            aggregate().commitEvents();
            assertFalse(aggregate.historyBackward()
                                 .hasNext());
        }
    }

    @Test
    @DisplayName("clear event records when commit after dispatch")
    void clearEventsWhenCommitAfterDispatch() {
        aggregate.dispatchCommands(command(createProject),
                                   command(addTask),
                                   command(startProject));
        assertTrue(aggregate().getUncommittedEvents()
                              .nonEmpty());
        aggregate().commitEvents();
        assertFalse(aggregate().getUncommittedEvents()
                               .nonEmpty());
    }

    @Test
    @DisplayName("transform current state to snapshot event")
    void transformCurrentStateToSnapshot() {

        dispatchCommand(aggregate, command(createProject));

        Snapshot snapshot = aggregate().toSnapshot();
        Project state = unpack(snapshot.getState(), Project.class);

        assertEquals(ID, state.getId());
        assertEquals(Status.CREATED, state.getStatus());
    }

    @Test
    @DisplayName("restore state from snapshot")
    void restoreStateFromSnapshot() {

        dispatchCommand(aggregate, command(createProject));

        Snapshot snapshotNewProject = aggregate().toSnapshot();

        Aggregate<?, ?, ?> anotherAggregate = newAggregate(aggregate.id());

        AggregateTransaction<?, ?, ?> tx = AggregateTransaction.start(anotherAggregate);
        anotherAggregate.restore(snapshotNewProject);
        tx.commit();

        assertEquals(aggregate.state(), anotherAggregate.state());
        assertEquals(aggregate.version(), anotherAggregate.version());
        assertEquals(aggregate.lifecycleFlags(), anotherAggregate.lifecycleFlags());
    }

    @Test
    @DisplayName("increment version upon state changing event applied")
    void incrementVersionOnEventApplied() {
        int version = aggregate.version()
                               .getNumber();
        // Dispatch two commands that cause events that modify aggregate state.
        aggregate.dispatchCommands(command(createProject), command(startProject));

        assertEquals(version + 2, aggregate.version()
                                           .getNumber());
    }

    @Test
    @DisplayName("record modification timestamp")
    void recordModificationTimestamp() {
        try {
            TimeTests.BackToTheFuture provider = new TimeTests.BackToTheFuture();
            Time.setProvider(provider);

            Timestamp currentTime = Time.currentTime();

            aggregate.dispatchCommands(command(createProject));

            assertEquals(currentTime, aggregate.whenModified());

            currentTime = provider.forward(10);

            aggregate.dispatchCommands(command(startProject));

            assertEquals(currentTime, aggregate.whenModified());
        } finally {
            Time.resetProvider();
        }
    }

    @Nested
    @DisplayName("prohibit")
    class Prohibit {
        @Test
        @DisplayName("to call `state()` from within applier")
        void callStateFromApplier() {
            ModelTests.dropAllModels();
            FaultyAggregate faultyAggregate =
                    new FaultyAggregate(ID, false, false);

            Command command = Given.ACommand.addTask(ID);
            DispatchOutcome outcome = dispatchCommand(faultyAggregate, env(command));

            assertThat(outcome.hasError()).isTrue();
            Error error = outcome.getError();
            assertThat(error)
                    .comparingExpectedFieldsOnly()
                    .isEqualTo(Error.newBuilder()
                                    .setType(IllegalStateException.class.getCanonicalName())
                                    .buildPartial());
        }
    }

    @Nested
    @DisplayName("catch RuntimeExceptions in")
    class CatchHandlerFailures {

        @Test
        @DisplayName("handlers")
        void whenHandlerThrows() {
            ModelTests.dropAllModels();

            FaultyAggregate faultyAggregate = new FaultyAggregate(ID, true, false);

            Command command = Given.ACommand.createProject();
            DispatchOutcome outcome = dispatchCommand(faultyAggregate, env(command));
            assertTrue(outcome.hasError());
            Error error = outcome.getError();
            assertThat(error)
                    .comparingExpectedFieldsOnly()
                    .isEqualTo(Error.newBuilder()
                                    .setType(IllegalStateException.class.getCanonicalName())
                                    .setMessage(FaultyAggregate.BROKEN_HANDLER)
                                    .buildPartial());
        }

        @Test
        @DisplayName("appliers")
        void whenApplierThrows() {
            ModelTests.dropAllModels();
            FaultyAggregate faultyAggregate =
                    new FaultyAggregate(ID, false, true);

            Command command = Given.ACommand.createProject();
            DispatchOutcome outcome = dispatchCommand(faultyAggregate, env(command));

            assertThat(outcome.hasError()).isTrue();
            Error error = outcome.getError();
            assertThat(error)
                    .comparingExpectedFieldsOnly()
                    .isEqualTo(Error.newBuilder()
                                    .setType(IllegalStateException.class.getCanonicalName())
                                    .setMessage(FaultyAggregate.BROKEN_APPLIER)
                                    .buildPartial());
        }

        @Test
        @DisplayName("the event replay")
        void whenPlayThrows() {
            ModelTests.dropAllModels();
            FaultyAggregate faultyAggregate =
                    new FaultyAggregate(ID, false, true);

            Event event = event(projectCreated(ID, getClass().getSimpleName()), 1);
            AggregateTransaction.start(faultyAggregate);
            AggregateHistory history = AggregateHistory
                    .newBuilder()
                    .addEvent(event)
                    .build();
            BatchDispatchOutcome batchDispatchOutcome =
                    ((Aggregate<?, ?, ?>) faultyAggregate).play(history);
            assertThat(batchDispatchOutcome.getSuccessful()).isFalse();
            MessageId expectedTarget = MessageId
                    .newBuilder()
                    .setId(Identifier.pack(faultyAggregate.id()))
                    .setTypeUrl(faultyAggregate.modelClass()
                                               .stateTypeUrl()
                                               .value())
                    .buildPartial();
            assertThat(batchDispatchOutcome.getTargetEntity())
                    .comparingExpectedFieldsOnly()
                    .isEqualTo(expectedTarget);
            assertThat(batchDispatchOutcome.getOutcomeCount()).isEqualTo(1);
            DispatchOutcome outcome = batchDispatchOutcome.getOutcome(0);
            assertThat(outcome.hasError()).isTrue();
            assertThat(outcome.getPropagatedSignal()).isEqualTo(event.messageId());
            Error error = outcome.getError();
            assertThat(error.getType()).isEqualTo(IllegalStateException.class.getCanonicalName());
            assertThat(error.getMessage()).isEqualTo(FaultyAggregate.BROKEN_APPLIER);
        }
    }

    @Test
    @DisplayName("not allow getting state builder from outside event applier")
    void notGetStateBuilderOutsideOfApplier() {
        assertThrows(IllegalStateException.class, () -> new IntAggregate(100).builder());
    }

    @Nested
    @DisplayName("traverse history")
    class TraverseHistory {

        private Iterator<Event> history;

        @Test
        @DisplayName("iterating through newest events first")
        void throughNewestEventsFirst() {
            TenantId tenantId = newTenantId();
            Command createCommand = command(createProject, tenantId);
            Command startCommand = command(startProject, tenantId);
            Command addTaskCommand = command(addTask, tenantId);
            Command addTaskCommand2 = command(addTask, tenantId);

            CommandBus commandBus = context.commandBus();
            StreamObserver<Ack> noOpObserver = noOpObserver();
            commandBus.post(createCommand, noOpObserver);
            commandBus.post(addTaskCommand, noOpObserver);
            commandBus.post(newArrayList(addTaskCommand2, startCommand), noOpObserver);

            TestAggregate aggregate = repository.loadAggregate(tenantId, ID);
            history = aggregate.historyBackward();

            assertNextCommandId().isEqualTo(startCommand.id());
            assertNextCommandId().isEqualTo(addTaskCommand2.id());
            assertNextCommandId().isEqualTo(addTaskCommand.id());
            assertNextCommandId().isEqualTo(createCommand.id());

            assertThat(history.hasNext())
                    .isFalse();
        }

        private ProtoSubject assertNextCommandId() {
            Event event = history.next();
            return assertThat(event.rootMessage()
                                   .asCommandId());
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

            CommandBus commandBus = context.commandBus();
            StreamObserver<Ack> noOpObserver = noOpObserver();
            commandBus.post(createCommand, noOpObserver);
            commandBus.post(startCommand, noOpObserver);
            commandBus.post(ImmutableList.of(addTaskCommand, addTaskCommand2), noOpObserver);

            TestAggregate aggregate = repository.loadAggregate(tenantId, ID);

            history = aggregate.historyBackward();

            assertNextCommandId()
                    .isEqualTo(addTaskCommand2.id());
            assertThat(history.hasNext())
                    .isFalse();
        }
    }

    @MuteLogging
    @Test
    @DisplayName("throw DuplicateCommandException for a duplicated command")
    void acknowledgeExceptionForDuplicateCommand() {
        DiagnosticMonitor monitor = new DiagnosticMonitor();
        context.internalAccess()
               .registerEventDispatcher(monitor);

        TenantId tenantId = newTenantId();
        Command createCommand = command(createProject, tenantId);
        CommandEnvelope envelope = CommandEnvelope.of(createCommand);
        repository.dispatch(envelope);
        repository.dispatch(envelope);
        List<CannotDispatchDuplicateCommand> duplicateCommandEvents =
                monitor.duplicateCommandEvents();
        assertThat(duplicateCommandEvents).hasSize(1);
        CannotDispatchDuplicateCommand event = duplicateCommandEvents.get(0);
        assertThat(event.getDuplicateCommand())
                .isEqualTo(envelope.messageId());
    }

    @Test
    @DisplayName("run Idempotency guard when dispatching commands")
    void checkCommandsUponHistory() {
        DiagnosticMonitor monitor = new DiagnosticMonitor();
        context.internalAccess()
               .registerEventDispatcher(monitor);
        Command createCommand = command(createProject);
        CommandEnvelope cmd = CommandEnvelope.of(createCommand);
        TenantId tenantId = newTenantId();
        Supplier<MessageEndpoint<ProjectId, ?>> endpoint =
                () -> new AggregateCommandEndpoint<>(repository, cmd);
        dispatch(tenantId, endpoint);
        dispatch(tenantId, endpoint);
        List<CannotDispatchDuplicateCommand> events = monitor.duplicateCommandEvents();
        assertThat(events).hasSize(1);
        CannotDispatchDuplicateCommand systemEvent = events.get(0);
        assertThat(systemEvent.getDuplicateCommand())
                .isEqualTo(cmd.messageId());
    }

    @Test
    @DisplayName("run Idempotency guard when dispatching events")
    void checkEventsUponHistory() {
        DiagnosticMonitor monitor = new DiagnosticMonitor();
        context.internalAccess()
               .registerEventDispatcher(monitor);
        AggProjectDeleted eventMessage = AggProjectDeleted
                .newBuilder()
                .setProjectId(ID)
                .vBuild();
        Event event = event(eventMessage, 2);
        EventEnvelope envelope = EventEnvelope.of(event);
        Supplier<MessageEndpoint<ProjectId, ?>> endpoint =
                () -> new AggregateEventReactionEndpoint<>(repository, envelope);
        TenantId tenantId = newTenantId();
        dispatch(tenantId, endpoint);
        dispatch(tenantId, endpoint);
        List<CannotDispatchDuplicateEvent> events = monitor.duplicateEventEvents();
        assertThat(events).hasSize(1);
        CannotDispatchDuplicateEvent systemEvent = events.get(0);
        assertThat(systemEvent.getDuplicateEvent())
                .isEqualTo(envelope.messageId());
    }

    private static void dispatch(TenantId tenant,
                                 Supplier<MessageEndpoint<ProjectId, ?>> endpoint) {
        with(tenant).run(
                () -> endpoint.get()
                              .dispatchTo(ID)
        );
    }

    @Nested
    @DisplayName("create a single event when emitting a pair without second value")
    class CreateSingleEventForPair extends ContextAwareTest {

        @Override
        protected BoundedContextBuilder contextBuilder() {
            return BoundedContextBuilder
                    .assumingTests()
                    .add(new TaskAggregateRepository());
        }

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
            context().receivesCommand(createTask())
                     .assertEvents()
                     .withType(AggTaskCreated.class)
                     .isNotEmpty();
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
            EventSubject assertEvents = context().receivesCommand(assignTask())
                                                 .assertEvents();
            assertEvents.hasSize(2);
            assertEvents.withType(AggTaskAssigned.class)
                        .hasSize(1);
            assertEvents.withType(AggUserNotified.class)
                        .hasSize(1);
        }

        /**
         * Ensures that a {@linkplain io.spine.server.tuple.Pair pair} with an empty second optional
         * value returned from a reaction on a rejection stores a single event.
         *
         * <p>The rejection is fired by the {@link TaskAggregate#handle(AggReassignTask)
         * TaskAggregate.handle(AggReassignTask)}
         * and handled by the {@link TaskAggregate#on(AggCannotReassignUnassignedTask)
         * TaskAggregate.on(AggCannotReassignUnassignedTask)}.
         */
        @Test
        @DisplayName("when reacting on a rejection")
        void fromRejectionReact() {
            EventSubject assertEvents = context().receivesCommand(reassignTask())
                                                 .assertEvents();
            assertEvents.hasSize(2);
            assertEvents.withType(AggCannotReassignUnassignedTask.class)
                        .hasSize(1);
            assertEvents.withType(AggUserNotified.class)
                        .hasSize(1);
        }
    }

    @Nested
    @DisplayName("allow having validation on the aggregate state and")
    class AllowValidatedAggregates extends ContextAwareTest {

        private final ThermometerId thermometer = ThermometerId.generate();

        @Override
        protected BoundedContextBuilder contextBuilder() {
            return BoundedContextBuilder
                    .assumingTests()
                    .add(new SafeThermometerRepo(thermometer));
        }

        @Test
        @DisplayName("not change the Aggregate state when there is no reaction on the event")
        void notChangeStateIfNoReaction() {
            TemperatureChanged booksOnFire =
                    TemperatureChanged.newBuilder()
                                      .setFahrenheit(451)
                                      .vBuild();
            context().receivesExternalEvent(booksOnFire)
                     .assertEntity(thermometer, SafeThermometer.class)
                     .doesNotExist();
        }

        @Test
        @DisplayName("save valid aggregate state on change")
        void safelySaveValidState() {
            TemperatureChanged gettingWarmer =
                    TemperatureChanged.newBuilder()
                                      .setFahrenheit(72)
                                      .vBuild();
            context().receivesExternalEvent(gettingWarmer);
            Thermometer expected = Thermometer
                    .newBuilder()
                    .setId(thermometer)
                    .setFahrenheit(72)
                    .vBuild();
            context().assertState(thermometer, expected);
        }
    }
}
