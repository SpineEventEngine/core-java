/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.procman;

import com.google.common.truth.IntegerSubject;
import com.google.common.truth.Subject;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.entity.given.Given;
import io.spine.server.entity.rejection.StandardRejections;
import io.spine.server.model.Nothing;
import io.spine.server.procman.given.pm.QuizProcmanRepository;
import io.spine.server.procman.given.pm.TestProcessManager;
import io.spine.server.procman.given.pm.TestProcessManagerRepo;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.given.GivenEvent;
import io.spine.test.procman.ElephantProcess;
import io.spine.test.procman.PmDontHandle;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmCancelIteration;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmPlanIteration;
import io.spine.test.procman.command.PmReviewBacklog;
import io.spine.test.procman.command.PmScheduleRetrospective;
import io.spine.test.procman.command.PmStartIteration;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.event.PmIterationCompleted;
import io.spine.test.procman.event.PmIterationPlanned;
import io.spine.test.procman.event.PmIterationStarted;
import io.spine.test.procman.event.PmNotificationSent;
import io.spine.test.procman.event.PmOwnerChanged;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.test.procman.quiz.PmQuestionId;
import io.spine.test.procman.quiz.command.PmAnswerQuestion;
import io.spine.test.procman.quiz.command.PmStartQuiz;
import io.spine.test.procman.quiz.event.PmQuestionAnswered;
import io.spine.test.procman.quiz.event.PmQuizStarted;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.logging.mute.MuteLogging;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.BlackBox;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.procman.given.dispatch.PmDispatcher.dispatch;
import static io.spine.server.procman.given.pm.GivenMessages.addTask;
import static io.spine.server.procman.given.pm.GivenMessages.cancelIteration;
import static io.spine.server.procman.given.pm.GivenMessages.createProject;
import static io.spine.server.procman.given.pm.GivenMessages.entityAlreadyArchived;
import static io.spine.server.procman.given.pm.GivenMessages.iterationPlanned;
import static io.spine.server.procman.given.pm.GivenMessages.ownerChanged;
import static io.spine.server.procman.given.pm.GivenMessages.quizStarted;
import static io.spine.server.procman.given.pm.GivenMessages.startProject;
import static io.spine.server.procman.given.pm.GivenMessages.throwEntityAlreadyArchived;
import static io.spine.server.procman.given.pm.GivenMessages.throwRuntimeException;
import static io.spine.server.procman.given.pm.QuizGiven.answerQuestion;
import static io.spine.server.procman.given.pm.QuizGiven.newAnswer;
import static io.spine.server.procman.given.pm.QuizGiven.newQuizId;
import static io.spine.server.procman.given.pm.QuizGiven.startQuiz;
import static io.spine.server.procman.model.ProcessManagerClass.asProcessManagerClass;
import static io.spine.testdata.Sample.messageOfType;
import static io.spine.testing.server.Assertions.assertCommandClassesExactly;
import static io.spine.testing.server.Assertions.assertEventClassesExactly;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`ProcessManager` should")
class ProcessManagerTest {

    private static final int VERSION = 2;
    private static final Any PRODUCER_ID = Identifier.pack(TestProcessManager.ID);

    private final TestEventFactory eventFactory =
            TestEventFactory.newInstance(PRODUCER_ID, getClass());
    private final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(getClass());

    private BoundedContext context;
    private TestProcessManager processManager;

    @BeforeEach
    void initContextAndProcessManager() {
        ModelTests.dropAllModels();
        context = BoundedContextBuilder
                .assumingTests(true)
                .build();
        processManager =
                Given.processManagerOfClass(TestProcessManager.class)
                     .withId(TestProcessManager.ID)
                     .withVersion(VERSION)
                     .withState(ElephantProcess.getDefaultInstance())
                     .build();
    }

    @AfterEach
    void closeContext() throws Exception {
        context.close();
    }

    @CanIgnoreReturnValue
    private List<? extends Message> testDispatchEvent(EventMessage eventMessage) {
        var event = eventFactory.createEvent(eventMessage);
        var result = dispatch(processManager, EventEnvelope.of(event))
                .getSuccess()
                .getProducedEvents()
                .getEventList();
        var expected = pack(eventMessage);
        assertState().isEqualTo(expected);
        return result;
    }

    private Subject assertState() {
        var pmState = processManager.state().getAny();
        var assertState = assertThat(pmState);
        return assertState;
    }

    @CanIgnoreReturnValue
    private List<Event> testDispatchCommand(CommandMessage commandMsg) {
        var command =
                requestFactory.command()
                              .create(commandMsg);
        var envelope = CommandEnvelope.of(command);
        var events = dispatch(processManager, envelope)
                .getSuccess()
                .getProducedEvents()
                .getEventList();
        var expected = pack(commandMsg);
        assertState().isEqualTo(expected);
        return events;
    }

    @Nested
    @DisplayName("dispatch")
    class Dispatch {

        @Test
        @DisplayName("command")
        void command() {
            testDispatchCommand(addTask());
        }

        @Test
        @DisplayName("event")
        void event() {
            var eventMessages = testDispatchEvent(messageOfType(PmProjectStarted.class));

            assertThat(eventMessages)
                    .hasSize(1);
            assertThat(eventMessages.get(0))
                    .isInstanceOf(Event.class);
        }
    }

    @Nested
    @DisplayName("increment version by one")
    class IncrementVersion {

        @Test
        @DisplayName("on handling command")
        void onCommandHandle() {
            checkIncrementsOnCommand(createProject());
        }

        @Test
        @DisplayName("on command substitution")
        void onCommandTransform() {
            checkIncrementsOnCommand(startProject());
        }

        @Test
        @DisplayName("when substituting command with multiple commands")
        void onCommandTransformIntoMultiple() {
            checkIncrementsOnCommand(cancelIteration());
        }

        @Test
        @DisplayName("on event react")
        void onEventReact() {
            checkIncrementsOnEvent(messageOfType(PmProjectStarted.class));
        }

        @Test
        @DisplayName("when producing command in response to incoming event")
        void onProducingCommand() {
            checkIncrementsOnEvent(messageOfType(PmOwnerChanged.class));
        }

        @Test
        @DisplayName("when producing several commands in response to incoming event")
        void onProducingSeveralCommands() {
            checkIncrementsOnEvent(messageOfType(PmIterationCompleted.class));
        }

        private void checkIncrementsOnCommand(CommandMessage commandMessage) {
            assertVersion().isEqualTo(VERSION);
            testDispatchCommand(commandMessage);
            assertVersion().isEqualTo(VERSION + 1);
        }

        private void checkIncrementsOnEvent(EventMessage eventMessage) {
            assertVersion().isEqualTo(VERSION);
            testDispatchEvent(eventMessage);
            assertVersion().isEqualTo(VERSION + 1);
        }

        private IntegerSubject assertVersion() {
            return assertThat(processManager.version().getNumber());
        }
    }

    @Test
    @DisplayName("dispatch command and return events")
    void dispatchCommandAndReturnEvents() {
        var events = testDispatchCommand(createProject());

        assertThat(events)
                .hasSize(1);
        var event = events.get(0);
        assertNotNull(event);
        var message = unpack(event.getMessage(), PmProjectCreated.class);
        assertThat(message.getProjectId())
                .isEqualTo(TestProcessManager.ID);
    }

    @Nested
    @DisplayName("dispatch rejection by")
    class DispatchRejectionBy {

        @Test
        @DisplayName("rejection message only")
        void rejectionMessage() {
            var re = entityAlreadyArchived(PmDontHandle.class);
            dispatch(processManager, re);
            var rejection = re.outerObject();
            assertReceived(rejection.getMessage());
        }

        @Test
        @DisplayName("rejection and command message")
        void rejectionAndCommandMessage() {
            var rejection = entityAlreadyArchived(PmAddTask.class);
            dispatch(processManager, rejection);
            var command = rejection.context().getRejection().getCommand();
            assertReceived(command.getMessage());
        }

        private void assertReceived(Any expected) {
            assertState().isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("dispatch several")
    class DispatchSeveral {

        @Test
        @DisplayName("commands")
        void commands() {
            testDispatchCommand(createProject());
            testDispatchCommand(addTask());
            testDispatchCommand(startProject());
        }

        @Test
        @DisplayName("events")
        void events() {
            testDispatchEvent(messageOfType(PmProjectCreated.class));
            testDispatchEvent(messageOfType(PmTaskAdded.class));
            testDispatchEvent(messageOfType(PmProjectStarted.class));
        }
    }

    @Nested
    @MuteLogging
    @DisplayName("rollback state on")
    class RollbackOn {

        private BlackBox context;

        @BeforeEach
        void setupContext() {
            context = BlackBox.from(
                    BoundedContextBuilder.assumingTests()
                                         .add(new TestProcessManagerRepo())
            ).tolerateFailures();
        }

        @AfterEach
        void closeContext() {
            context.close();
        }

        @Test
        @DisplayName("rejection")
        void rejection() {
            context.receivesCommand(throwEntityAlreadyArchived());
            context.assertEvents()
                   .withType(StandardRejections.EntityAlreadyArchived.class)
                   .hasSize(1);
            assertNoEntity();
        }

        @Test
        @DisplayName("exception")
        void exception() {
            context.receivesCommand(throwRuntimeException());
            assertNoEntity();
        }

        private void assertNoEntity() {
            context.assertEntity(TestProcessManager.ID, TestProcessManager.class)
                   .doesNotExist();
        }
    }

    @Nested
    @DisplayName("create command(s)")
    class CommandCreation {

        private BlackBox context;

        @BeforeEach
        void setupContext() {
            context = BlackBox.from(
                    BoundedContextBuilder.assumingTests()
                                         .add(new TestProcessManagerRepo())
            );
        }

        @AfterEach
        void closeContext() {
            context.close();
        }

        @Nested
        @DisplayName("single command")
        class SingleCommand {

            /**
             * Tests transformation of a command into another command.
             *
             * @see TestProcessManager#transform(PmStartProject)
             */
            @Test
            @DisplayName("by transform incoming command")
            void transformCommand() {
                context.receivesCommand(startProject())
                       .assertCommands()
                       .withType(PmAddTask.class)
                       .hasSize(1);
            }

            /**
             * Tests generation of a command in response to incoming event.
             *
             * @see TestProcessManager#on(PmOwnerChanged)
             */
            @Test
            @DisplayName("on incoming event")
            void commandOnEvent() {
                context.receivesEvent(ownerChanged())
                       .assertCommands()
                       .withType(PmReviewBacklog.class)
                       .hasSize(1);
            }

            @Test
            @DisplayName("on incoming external event")
            void commandOnExternalEvent() {
                context.receivesExternalEvent(quizStarted())
                       .assertCommands()
                       .withType(PmCreateProject.class)
                       .hasSize(1);
            }
        }

        @Nested
        @DisplayName("several commands")
        class SeveralCommands {

            /**
             * Tests splitting incoming command into two.
             *
             * @see TestProcessManager#split(PmCancelIteration)
             */
            @Test
            @DisplayName("when splitting incoming command")
            void splitCommand() {
                var assertCommands = context.receivesCommand(cancelIteration())
                                            .assertCommands();
                assertCommands.withType(PmScheduleRetrospective.class)
                              .hasSize(1);
                assertCommands.withType(PmPlanIteration.class)
                              .hasSize(1);
            }
        }

        @Nested
        @DisplayName("optionally on incoming event")
        class OptionalCommand {

            @Test
            @DisplayName("when command is generated")
            void commandGenerated() {
                context.receivesEvent(iterationPlanned(true))
                       .assertCommands()
                       .withType(PmStartIteration.class)
                       .hasSize(1);
            }

            @Test
            @DisplayName("when command is NOT generated")
            void noCommand() {
                context.receivesEvent(iterationPlanned(false))
                       .assertCommands()
                       .isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("fail when dispatching unknown")
    class ThrowOnUnknown {

        @Test
        @DisplayName("command")
        void command() {
            var envelope = CommandEnvelope.of(
                    requestFactory.createCommand(PmDontHandle.getDefaultInstance())
            );
            assertThrows(IllegalStateException.class,
                         () -> processManager.dispatchCommand(envelope));
        }
    }

    @Nested
    @DisplayName("ignore when dispatching unknown")
    class IgnoresUnknown {

        @Test
        @DisplayName("event")
        void event() {
            var envelope = EventEnvelope.of(GivenEvent.arbitrary());

            var outcome = dispatch(processManager, envelope);
            assertTrue(outcome.hasIgnored());
        }
    }

    @Nested
    @DisplayName("not create `Nothing` event")
    class NoEmpty {

        /**
         * This test executes two commands, thus checks for 2 Acks:
         * <ol>
         *     <li>{@link PmStartQuiz Start Quiz} — to start the process;
         *     <li>{@link PmAnswerQuestion Answer Question } — a target
         * command that produces either of 3 events.
         * </ol>
         *
         * <p>First command emits a {@link PmQuizStarted Quiz Started}
         * event.
         *
         * <p>Second command emits a {@link PmQuestionAnswered Question Answered}
         * event.
         *
         * <p>As a reaction to {@link PmQuestionAnswered Quiestion Answered}
         * the process manager emits an {@link io.spine.server.tuple.EitherOf3 EitherOf3}
         * containing {@link Nothing}. This is done because the answered
         * question is not part of a quiz.
         *
         * @see io.spine.server.procman.given.pm.QuizProcman
         */
        @Test
        @DisplayName("for an either of three event reaction")
        void afterEmittingEitherOfThreeOnEventReaction() {
            var quizId = newQuizId();
            Iterable<PmQuestionId> questions = newArrayList();
            var startQuiz = startQuiz(quizId, questions);
            var answerQuestion = answerQuestion(quizId, newAnswer());

            var context = BlackBox.from(
                    BoundedContextBuilder.assumingTests()
                                         .add(new QuizProcmanRepository())
            );
            var assertEvents = context
                    .receivesCommands(startQuiz, answerQuestion)
                    .assertEvents();
            assertEvents.hasSize(2);
            assertEvents.withType(PmQuizStarted.class)
                        .hasSize(1);
            assertEvents.withType(PmQuestionAnswered.class)
                        .hasSize(1);
            context.close();
        }
    }

    @Nested
    @DisplayName("in its class, expose")
    class ExposeInClass {

        @Test
        @DisplayName("produced commands")
        void producedCommands() {
            var pmClass =
                    asProcessManagerClass(TestProcessManager.class);
            Set<CommandClass> commands = pmClass.outgoingCommands();
            assertCommandClassesExactly(commands,
                                        PmCreateProject.class,
                                        PmAddTask.class,
                                        PmReviewBacklog.class,
                                        PmScheduleRetrospective.class,
                                        PmPlanIteration.class,
                                        PmStartIteration.class);
        }

        @Test
        @DisplayName("produced events")
        void producedEvents() {
            var pmClass =
                    asProcessManagerClass(TestProcessManager.class);
            Set<EventClass> events = pmClass.outgoingEvents();
            assertEventClassesExactly(events,
                                      PmProjectCreated.class,
                                      PmTaskAdded.class,
                                      PmNotificationSent.class,
                                      PmIterationPlanned.class,
                                      PmIterationStarted.class,
                                      StandardRejections.EntityAlreadyArchived.class);
        }

        @Test
        @DisplayName("handled external event classes")
        void handledExternalEvents() {
            var pmClass =
                    asProcessManagerClass(TestProcessManager.class);
            Set<EventClass> externalEvents = pmClass.externalEvents();
            assertEventClassesExactly(externalEvents,
                                      PmQuizStarted.class,
                                      PmQuestionAnswered.class);
        }
    }
}
