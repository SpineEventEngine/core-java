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
package io.spine.server.procman

import com.google.common.collect.Lists
import com.google.common.truth.IntegerSubject
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.Any
import com.google.protobuf.Message
import io.spine.base.CommandMessage
import io.spine.base.EventMessage
import io.spine.base.Identifier
import io.spine.core.Event
import io.spine.protobuf.AnyPacker
import io.spine.protobuf.pack
import io.spine.server.BoundedContext
import io.spine.server.BoundedContextBuilder
import io.spine.server.entity.given.Given
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived
import io.spine.server.procman.given.dispatch.PmDispatcher
import io.spine.server.procman.given.pm.GivenMessages
import io.spine.server.procman.given.pm.GivenMessages.addTask
import io.spine.server.procman.given.pm.GivenMessages.cancelIteration
import io.spine.server.procman.given.pm.GivenMessages.createProject
import io.spine.server.procman.given.pm.GivenMessages.entityAlreadyArchived
import io.spine.server.procman.given.pm.GivenMessages.ownerChanged
import io.spine.server.procman.given.pm.GivenMessages.quizStarted
import io.spine.server.procman.given.pm.GivenMessages.startProject
import io.spine.server.procman.given.pm.GivenMessages.throwEntityAlreadyArchived
import io.spine.server.procman.given.pm.GivenMessages.throwRuntimeException
import io.spine.server.procman.given.pm.QuizGiven
import io.spine.server.procman.given.pm.QuizGiven.answerQuestion
import io.spine.server.procman.given.pm.QuizGiven.startQuiz
import io.spine.server.procman.given.pm.QuizProcmanRepository
import io.spine.server.procman.given.pm.TestProcessManager
import io.spine.server.procman.given.pm.TestProcessManagerRepo
import io.spine.server.procman.model.ProcessManagerClass.asProcessManagerClass
import io.spine.server.type.CommandEnvelope
import io.spine.server.type.EventEnvelope
import io.spine.server.type.given.GivenEvent
import io.spine.test.procman.ElephantProcess
import io.spine.test.procman.PmDontHandle
import io.spine.test.procman.command.PmAddTask
import io.spine.test.procman.command.PmCreateProject
import io.spine.test.procman.command.PmPlanIteration
import io.spine.test.procman.command.PmReviewBacklog
import io.spine.test.procman.command.PmScheduleRetrospective
import io.spine.test.procman.command.PmStartIteration
import io.spine.test.procman.event.PmIterationCompleted
import io.spine.test.procman.event.PmIterationPlanned
import io.spine.test.procman.event.PmIterationStarted
import io.spine.test.procman.event.PmNotificationSent
import io.spine.test.procman.event.PmOwnerChanged
import io.spine.test.procman.event.PmProjectCreated
import io.spine.test.procman.event.PmProjectStarted
import io.spine.test.procman.event.PmTaskAdded
import io.spine.test.procman.quiz.PmQuestionId
import io.spine.test.procman.quiz.event.PmQuestionAnswered
import io.spine.test.procman.quiz.event.PmQuizStarted
import io.spine.testdata.Sample.messageOfType
import io.spine.testing.client.TestActorRequestFactory
import io.spine.testing.logging.mute.MuteLogging
import io.spine.testing.server.Assertions.assertCommandClassesExactly
import io.spine.testing.server.Assertions.assertEventClassesExactly
import io.spine.testing.server.TestEventFactory
import io.spine.testing.server.blackbox.BlackBox
import io.spine.testing.server.model.ModelTests
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`ProcessManager` should")
internal class ProcessManagerSpec {

    private val eventFactory = TestEventFactory.newInstance(PRODUCER_ID, javaClass)
    private val requestFactory = TestActorRequestFactory(javaClass)

    private lateinit var context: BoundedContext
    private lateinit var processManager: TestProcessManager

    @BeforeEach
    fun initContextAndProcessManager() {
        ModelTests.dropAllModels()
        context = BoundedContextBuilder.assumingTests(true).build()
        processManager = Given.processManagerOfClass(TestProcessManager::class.java)
            .withId(TestProcessManager.ID)
            .withVersion(VERSION)
            .withState(ElephantProcess.getDefaultInstance())
            .build()
    }

    @AfterEach
    fun closeContext() {
        context.close()
    }

    @CanIgnoreReturnValue
    private fun testDispatchEvent(eventMessage: EventMessage): List<Message> {
        val event = eventFactory.createEvent(eventMessage)
        val result = PmDispatcher.dispatch(processManager, EventEnvelope.of(event))
            .success
            .producedEvents
            .eventList
        val expected = eventMessage.pack()
        assertSignal().isEqualTo(expected)
        return result
    }

    private fun <E: EventMessage> testDispatchEvent(eventClass: Class<E>): List<Message> {
        val event = messageOfType(eventClass)
        return testDispatchEvent(event)
    }

    private fun assertSignal(): Subject {
        val lastSignal = processManager.state().any
        return assertThat(lastSignal)
    }

    @CanIgnoreReturnValue
    private fun testDispatchCommand(commandMsg: CommandMessage): List<Event> {
        val command = requestFactory.command()
            .create(commandMsg)
        val envelope = CommandEnvelope.of(command)
        val events = PmDispatcher.dispatch(processManager, envelope)
            .success
            .producedEvents
            .eventList
        val expected = commandMsg.pack()
        assertSignal().isEqualTo(expected)
        return events
    }

    @Nested
    @DisplayName("dispatch")
    internal inner class Dispatch {
        @Test
        @DisplayName("command")
        fun command() {
            testDispatchCommand(addTask())
        }

        @Test
        @DisplayName("event")
        fun event() {
            val eventMessages = testDispatchEvent(PmProjectStarted::class.java)
            assertThat(eventMessages).hasSize(1)
            assertThat(eventMessages[0])
                .isInstanceOf(Event::class.java)
        }
    }

    @Nested internal inner class
    `increment version by one` {

        @Test
        fun `on handling command`() =
            checkIncrementsOnCommand(createProject())

        @Test
        fun `on command substitution`() =
            checkIncrementsOnCommand(startProject())

        @Test
        fun `when substituting command with multiple commands`() {
            checkIncrementsOnCommand(cancelIteration())
        }

        @Test
        fun `on event react`() =
            checkIncrementsOnEvent(PmProjectStarted::class.java)

        @Test
        fun `when producing command in response to incoming event`() =
            checkIncrementsOnEvent(PmOwnerChanged::class.java)

        @Test
        fun `when producing several commands in response to incoming event`() =
            checkIncrementsOnEvent(PmIterationCompleted::class.java)

        private fun checkIncrementsOnCommand(commandMessage: CommandMessage) {
            assertVersion().isEqualTo(VERSION)
            testDispatchCommand(commandMessage)
            assertVersion().isEqualTo(VERSION + 1)
        }

        private fun <E: EventMessage> checkIncrementsOnEvent(eventClass: Class<E>) {
            val eventMessage = messageOfType(eventClass)
            assertVersion().isEqualTo(VERSION)
            testDispatchEvent(eventMessage)
            assertVersion().isEqualTo(VERSION + 1)
        }

        private fun assertVersion(): IntegerSubject {
            return assertThat(processManager.version().number)
        }
    }

    @Test
    fun `dispatch command and return events`() {
        val events = testDispatchCommand(createProject())
        assertThat(events).hasSize(1)
        val event = events[0]
        assertNotNull(event)
        val message = AnyPacker.unpack(event.message, PmProjectCreated::class.java)
        assertThat(message.projectId)
            .isEqualTo(TestProcessManager.ID)
    }

    @Nested internal inner class
    `dispatch rejection by` {

        @Test
        fun `rejection message only`() {
            val re = entityAlreadyArchived(PmDontHandle::class.java)
            PmDispatcher.dispatch(processManager, re)
            val rejection = re.outerObject()
            assertReceived(rejection.message)
        }

        @Test
        fun `rejection and command message`() {
            val rejection = entityAlreadyArchived(PmAddTask::class.java)
            PmDispatcher.dispatch(processManager, rejection)
            val command = rejection.context().rejection.command
            assertReceived(command.message)
        }

        private fun assertReceived(expected: Any) {
            assertSignal().isEqualTo(expected)
        }
    }

    @Nested internal inner class
    `dispatch several` {

        @Test
        fun commands() {
            testDispatchCommand(createProject())
            testDispatchCommand(addTask())
            testDispatchCommand(startProject())
        }

        @Test
        fun events() {
            testDispatchEvent(PmProjectCreated::class.java)
            testDispatchEvent(PmTaskAdded::class.java)
            testDispatchEvent(PmProjectStarted::class.java)
        }
    }

    @Nested @MuteLogging internal inner class
    `rollback state on` {

        private lateinit var context: BlackBox

        @BeforeEach
        fun setupContext() {
            context = blackBoxWith(TestProcessManagerRepo()).tolerateFailures()
        }

        @AfterEach
        fun closeContext() = context.close()

        @Test
        fun rejection() {
            context.receivesCommand(throwEntityAlreadyArchived())
            context.assertEvents()
                .withType(EntityAlreadyArchived::class.java)
                .hasSize(1)
            assertNoEntity()
        }

        @Test
        fun exception() {
            context.receivesCommand(throwRuntimeException())
            assertNoEntity()
        }

        private fun assertNoEntity() {
            context.assertEntity(TestProcessManager.ID, TestProcessManager::class.java)
                .doesNotExist()
        }
    }

    @Nested
    internal inner class `create command(s)` {

        private lateinit var context: BlackBox

        @BeforeEach
        fun setupContext() {
            context = blackBoxWith(TestProcessManagerRepo())
        }

        @AfterEach
        fun closeContext() {
            context.close()
        }

        @Nested internal inner class
        `single command` {

            /**
             * Tests transformation of a command into another command.
             *
             * @see TestProcessManager.transform
             */
            @Test
            fun `by transform incoming command`() =
                context.receivesCommand(startProject())
                    .assertCommands()
                    .withType(PmAddTask::class.java)
                    .hasSize(1)

            /**
             * Tests generation of a command in response to incoming event.
             *
             * @see TestProcessManager.on
             */
            @Test
            fun `on incoming event`() =
                context.receivesEvent(ownerChanged())
                    .assertCommands()
                    .withType(PmReviewBacklog::class.java)
                    .hasSize(1)

            @Test
            fun `on incoming external event`() =
                context.receivesExternalEvent(quizStarted())
                    .assertCommands()
                    .withType(PmCreateProject::class.java)
                    .hasSize(1)
        }

        @Nested internal inner class
        `several commands` {

            /**
             * Tests splitting incoming command into two.
             *
             * @see TestProcessManager.split
             */
            @Test
            fun `when splitting incoming command`() {
                val assertCommands = context.receivesCommand(cancelIteration())
                    .assertCommands()
                assertCommands.withType(PmScheduleRetrospective::class.java)
                    .hasSize(1)
                assertCommands.withType(PmPlanIteration::class.java)
                    .hasSize(1)
            }
        }

        @Nested internal inner class
        `optionally on incoming event` {

            @Test
            fun `when command is generated`() {
                context.receivesEvent(GivenMessages.iterationPlanned(true))
                    .assertCommands()
                    .withType(PmStartIteration::class.java)
                    .hasSize(1)
            }

            @Test
            fun `when command is NOT generated`() {
                context.receivesEvent(GivenMessages.iterationPlanned(false))
                    .assertCommands()
                    .isEmpty()
            }
        }
    }

    @Nested
    internal inner class `fail when dispatching unknown` {

        @Test
        fun command() {
            val envelope = CommandEnvelope.of(
                requestFactory.createCommand(PmDontHandle.getDefaultInstance())
            )
            assertThrows<IllegalStateException> {
                processManager.dispatchCommand(envelope)
            }
        }
    }

    @Nested internal inner class
    `ignore when dispatching unknown` {

        @Test
        fun event() {
            val envelope = EventEnvelope.of(GivenEvent.arbitrary())
            val outcome = PmDispatcher.dispatch(processManager, envelope)
            assertTrue(outcome.hasIgnored())
        }
    }

    @Nested internal inner class
    `not create 'Nothing' event` {

        /**
         * This test executes two commands, thus checks for 2 Acks:
         *
         *  1. [Start Quiz][io.spine.test.procman.quiz.command.PmStartQuiz] — to start the process;
         *  2. [Answer Question][io.spine.test.procman.quiz.command.PmAnswerQuestion] — a target
         *     command that produces either of 3 events.
         *
         * The first command emits a [Quiz Started][PmQuizStarted] event.
         *
         * The second command emits a [Question Answered][PmQuestionAnswered] event.
         *
         * As a reaction to [Question Answered][PmQuestionAnswered]
         * the process manager emits an [EitherOf3][io.spine.server.tuple.EitherOf3]
         * containing [Nothing]. This is done because the answered
         * question is not part of a quiz.
         *
         * @see io.spine.server.procman.given.pm.QuizProcman
         */
        @Test
        fun `for an either of three event reaction`() {
            val quizId = QuizGiven.newQuizId()
            val questions: Iterable<PmQuestionId> = Lists.newArrayList()
            val startQuiz = startQuiz(quizId, questions)
            val answerQuestion = answerQuestion(quizId, QuizGiven.newAnswer())
            val context = blackBoxWith(QuizProcmanRepository())
            val assertEvents = context
                .receivesCommands(startQuiz, answerQuestion)
                .assertEvents()
            assertEvents.hasSize(2)
            assertEvents.withType(PmQuizStarted::class.java)
                .hasSize(1)
            assertEvents.withType(PmQuestionAnswered::class.java)
                .hasSize(1)
            context.close()
        }
    }

    @Nested internal inner class
    `in its class, expose` {

        @Test
        fun `produced commands`() {
            val pmClass = asProcessManagerClass(TestProcessManager::class.java)
            val commands = pmClass.outgoingCommands()
            assertCommandClassesExactly(
                commands,
                PmCreateProject::class.java,
                PmAddTask::class.java,
                PmReviewBacklog::class.java,
                PmScheduleRetrospective::class.java,
                PmPlanIteration::class.java,
                PmStartIteration::class.java
            )
        }

        @Test
        fun `produced events`() {
            val pmClass = asProcessManagerClass(TestProcessManager::class.java)
            val events = pmClass.outgoingEvents()
            assertEventClassesExactly(
                events,
                PmProjectCreated::class.java,
                PmTaskAdded::class.java,
                PmNotificationSent::class.java,
                PmIterationPlanned::class.java,
                PmIterationStarted::class.java,
                EntityAlreadyArchived::class.java
            )
        }

        @Test
        fun `handled external event classes`() {
            val pmClass = asProcessManagerClass(TestProcessManager::class.java)
            val externalEvents = pmClass.externalEvents()
            assertEventClassesExactly(
                externalEvents,
                PmQuizStarted::class.java,
                PmQuestionAnswered::class.java
            )
        }
    }

    companion object {
        private const val VERSION = 2
        private val PRODUCER_ID = Identifier.pack(TestProcessManager.ID)
    }
}

/**
 * Creates a [BlackBox] with the given [ProcessManagerRepository] as the only repository.
 */
fun blackBoxWith(repository: ProcessManagerRepository<*, *, *>): BlackBox =
    BlackBox.from(
        BoundedContextBuilder.assumingTests()
            .add(repository)
    )
