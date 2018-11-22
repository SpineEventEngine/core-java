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

package io.spine.server.procman;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.given.GivenEvent;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.EventBus;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.model.Nothing;
import io.spine.server.procman.given.pm.AddTaskDispatcher;
import io.spine.server.procman.given.pm.DirectQuizProcmanRepository;
import io.spine.server.procman.given.pm.QuizProcmanRepository;
import io.spine.server.procman.given.pm.TestProcessManager;
import io.spine.server.procman.given.pm.TestProcessManagerRepo;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.TenantIndex;
import io.spine.system.server.NoOpSystemWriteSide;
import io.spine.test.procman.PmDontHandle;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmCancelIteration;
import io.spine.test.procman.command.PmPlanIteration;
import io.spine.test.procman.command.PmReviewBacklog;
import io.spine.test.procman.command.PmScheduleRetrospective;
import io.spine.test.procman.command.PmStartIteration;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.event.PmOwnerChanged;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.test.procman.quiz.PmQuestionId;
import io.spine.test.procman.quiz.PmQuizId;
import io.spine.test.procman.quiz.command.PmAnswerQuestion;
import io.spine.test.procman.quiz.command.PmStartQuiz;
import io.spine.test.procman.quiz.event.PmQuestionAnswered;
import io.spine.test.procman.quiz.event.PmQuizStarted;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.ShardingReset;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;
import io.spine.testing.server.entity.given.Given;
import io.spine.testing.server.model.ModelTests;
import io.spine.testing.server.procman.InjectCommandBus;
import io.spine.testing.server.tenant.TenantAwareTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.procman.given.pm.GivenMessages.addTask;
import static io.spine.server.procman.given.pm.GivenMessages.cancelIteration;
import static io.spine.server.procman.given.pm.GivenMessages.createProject;
import static io.spine.server.procman.given.pm.GivenMessages.entityAlreadyArchived;
import static io.spine.server.procman.given.pm.GivenMessages.iterationPlanned;
import static io.spine.server.procman.given.pm.GivenMessages.ownerChanged;
import static io.spine.server.procman.given.pm.GivenMessages.startProject;
import static io.spine.server.procman.given.pm.QuizGiven.answerQuestion;
import static io.spine.server.procman.given.pm.QuizGiven.newAnswer;
import static io.spine.server.procman.given.pm.QuizGiven.newQuizId;
import static io.spine.server.procman.given.pm.QuizGiven.startQuiz;
import static io.spine.testdata.Sample.messageOfType;
import static io.spine.testing.client.blackbox.Count.none;
import static io.spine.testing.client.blackbox.Count.once;
import static io.spine.testing.client.blackbox.Count.twice;
import static io.spine.testing.client.blackbox.VerifyAcknowledgements.acked;
import static io.spine.testing.server.blackbox.VerifyCommands.emittedCommand;
import static io.spine.testing.server.blackbox.VerifyCommands.emittedCommands;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvent;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvents;
import static io.spine.testing.server.procman.PmDispatcher.dispatch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

@SuppressWarnings({"OverlyCoupledClass",
        "InnerClassMayBeStatic", "ClassCanBeStatic" /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@ExtendWith(ShardingReset.class)
@DisplayName("ProcessManager should")
class ProcessManagerTest {

    private final TestEventFactory eventFactory =
            TestEventFactory.newInstance(Identifier.pack(TestProcessManager.ID), getClass());
    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private BoundedContext context;
    private CommandBus commandBus;
    private TestProcessManager processManager;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        context = BoundedContext
                .newBuilder()
                .setMultitenant(true)
                .build();
        StorageFactory storageFactory = context.getStorageFactory();
        TenantIndex tenantIndex = TenantAwareTest.createTenantIndex(false, storageFactory);

        EventBus eventBus = EventBus.newBuilder()
                                    .setStorageFactory(storageFactory)
                                    .build();
        commandBus = spy(CommandBus.newBuilder()
                                   .injectTenantIndex(tenantIndex)
                                   .injectSystem(NoOpSystemWriteSide.INSTANCE)
                                   .injectEventBus(eventBus)
                                   .build());
        processManager = Given.processManagerOfClass(TestProcessManager.class)
                              .withId(TestProcessManager.ID)
                              .withVersion(2)
                              .withState(Any.getDefaultInstance())
                              .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
    }

    @CanIgnoreReturnValue
    private List<? extends Message> testDispatchEvent(EventMessage eventMessage) {
        Event event = eventFactory.createEvent(eventMessage);
        List<Event> result = dispatch(processManager, EventEnvelope.of(event));
        assertEquals(pack(eventMessage), processManager.getState());
        return result;
    }

    @CanIgnoreReturnValue
    private List<Event> testDispatchCommand(CommandMessage commandMsg) {
        CommandEnvelope envelope = CommandEnvelope.of(requestFactory.command()
                                                                    .create(commandMsg));
        List<Event> events = dispatch(processManager, envelope);
        assertEquals(pack(commandMsg), processManager.getState());
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
            List<? extends Message> eventMessages =
                    testDispatchEvent(messageOfType(PmProjectStarted.class));

            assertEquals(1, eventMessages.size());
            assertTrue(eventMessages.get(0) instanceof Event);
        }
    }

    @Test
    @DisplayName("dispatch command and return events")
    void dispatchCommandAndReturnEvents() {
        List<Event> events = testDispatchCommand(createProject());

        assertEquals(1, events.size());
        Event event = events.get(0);
        assertNotNull(event);
        PmProjectCreated message = unpack(event.getMessage(), PmProjectCreated.class);
        assertEquals(TestProcessManager.ID, message.getProjectId());
    }

    @Nested
    @DisplayName("dispatch rejection by")
    class DispatchRejectionBy {

        @Test
        @DisplayName("rejection message only")
        void rejectionMessage() {
            RejectionEnvelope rejection = entityAlreadyArchived(PmDontHandle.class);
            dispatch(processManager, rejection.getEvent());
            assertEquals(rejection.getOuterObject().getMessage(),
                         processManager.getState());
        }

        @Test
        @DisplayName("rejection and command message")
        void rejectionAndCommandMessage() {
            RejectionEnvelope rejection = entityAlreadyArchived(PmAddTask.class);
            dispatch(processManager, rejection.getEvent());
            assertEquals(rejection.getOrigin().getMessage(),
                         processManager.getState());
        }
    }

    @Nested
    @DisplayName("dispatch several")
    class DispatchSeveral {

        @Test
        @DisplayName("commands")
        void commands() {
            commandBus.register(new AddTaskDispatcher());
            InjectCommandBus.of(commandBus)
                            .to(processManager);

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
    @DisplayName("create command(s)")
    class CommandCreation {

        private SingleTenantBlackBoxContext boundedContext;

        @BeforeEach
        void setUp() {
            boundedContext = BlackBoxBoundedContext.newInstance()
                                                   .with(new TestProcessManagerRepo());
        }

        @AfterEach
        void tearDown() {
            boundedContext.close();
        }

        @Nested
        @DisplayName("single command")
        class SingleCommand {

            /**
             * Tests transformation of a command into another command.
             * @see TestProcessManager#transform(PmStartProject)
             */
            @Test
            @DisplayName("by transform incoming command")
            void transformCommand() {
                boundedContext.receivesCommand(startProject())
                              .assertThat(emittedCommand(PmAddTask.class, once()));
            }

            /**
             * Tests generation of a command in response to incoming event.
             * @see TestProcessManager#on(PmOwnerChanged)
             */
            @Test
            @DisplayName("on incoming event")
            void commandOnEvent() {
                boundedContext.receivesEvent(ownerChanged())
                              .assertThat(emittedCommand(PmReviewBacklog.class));
            }
        }

        @Nested
        @DisplayName("several commands")
        class SeveralCommands {

            /**
             * Tests splitting incoming command into two.
             * @see TestProcessManager#split(PmCancelIteration)
             */
            @Test
            @DisplayName("when splitting incoming command")
            void splitCommand() {
                boundedContext.receivesCommand(cancelIteration())
                              .assertThat(emittedCommands(PmScheduleRetrospective.class,
                                                          PmPlanIteration.class));
            }
        }

        @Nested
        @DisplayName("optionally on incoming event")
        class OptionalCommand {

            @Test
            @DisplayName("when command is generated")
            void commandGenerated() {
                boundedContext.receivesEvent(iterationPlanned(true))
                              .assertThat(emittedCommand(PmStartIteration.class, once()));
            }

            @Test
            @DisplayName("when command is NOT generated")
            void noCommand() {
                boundedContext.receivesEvent(iterationPlanned(false))
                              .assertThat(emittedCommand(PmStartIteration.class, none()));
            }
        }
    }

    @Nested
    @DisplayName("throw ISE when dispatching unknown")
    class ThrowOnUnknown {

        @Test
        @DisplayName("command")
        void command() {
            CommandEnvelope envelope = CommandEnvelope.of(
                    requestFactory.createCommand(PmDontHandle.getDefaultInstance())
            );
            assertThrows(IllegalStateException.class,
                         () -> processManager.dispatchCommand(envelope));
        }

        @Test
        @DisplayName("event")
        void event() {
            EventEnvelope envelope = EventEnvelope.of(GivenEvent.arbitrary());

            assertThrows(IllegalStateException.class, () -> dispatch(processManager, envelope));
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
            PmQuizId quizId = newQuizId();
            Iterable<PmQuestionId> questions = newArrayList();
            PmStartQuiz startQuiz = startQuiz(quizId, questions);
            PmAnswerQuestion answerQuestion = answerQuestion(quizId, newAnswer());

            BlackBoxBoundedContext
                    .newInstance()
                    .with(new QuizProcmanRepository())
                    .receivesCommands(startQuiz, answerQuestion)
                    .assertThat(acked(twice()).withoutErrorsOrRejections())
                    .assertThat(emittedEvent(twice()))
                    .assertThat(emittedEvents(PmQuizStarted.class))
                    .assertThat(emittedEvents(PmQuestionAnswered.class))
                    .assertThat(emittedEvent(Nothing.class, none()))
                    .close();
        }

        /**
         * This test executes two commands, thus checks for 2 Acks:
         * <ol>
         *     <li>{@link PmStartQuiz Start Quiz} — to initialize the process;
         *     <li>{@link PmAnswerQuestion Answer Question } — a target
         * command that produces either of 3 events.
         * </ol>
         *
         * <p>First command emits a {@link PmQuizStarted Quiz Started}
         * event.
         *
         * <p>Because the quiz is started without any questions to solve,
         * an {@link PmAnswerQuestion answer question command} can not
         * match any questions. This results in emitting
         * {@link io.spine.server.tuple.EitherOf3 EitherOf3}
         * containing {@link Nothing}.
         *
         * @see io.spine.server.procman.given.pm.DirectQuizProcman
         */
        @Test
        @DisplayName("for an either of three emitted upon handling a command")
        void afterEmittingEitherOfThreeOnCommandDispatch() {
            PmQuizId quizId = newQuizId();
            Iterable<PmQuestionId> questions = newArrayList();
            PmStartQuiz startQuiz = startQuiz(quizId, questions);
            PmAnswerQuestion answerQuestion = answerQuestion(quizId, newAnswer());

            BlackBoxBoundedContext
                    .newInstance()
                    .with(new DirectQuizProcmanRepository())
                    .receivesCommands(startQuiz, answerQuestion)
                    .assertThat(acked(twice()).withoutErrorsOrRejections())
                    .assertThat(emittedEvent(once()))
                    .assertThat(emittedEvents(PmQuizStarted.class))
                    .assertThat(emittedEvent(Nothing.class, none()))
                    .close();
        }
    }
}
