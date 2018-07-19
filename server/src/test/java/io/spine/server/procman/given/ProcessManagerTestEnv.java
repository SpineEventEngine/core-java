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

package io.spine.server.procman.given;

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventContext;
import io.spine.core.React;
import io.spine.server.command.Assign;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived;
import io.spine.server.procman.CommandTransformed;
import io.spine.server.procman.ProcessManager;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.event.PmNotificationSent;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.test.procman.quiz.PmAnswer;
import io.spine.test.procman.quiz.PmQuestionId;
import io.spine.test.procman.quiz.PmQuizId;
import io.spine.test.procman.quiz.command.PmAnswerQuestion;
import io.spine.test.procman.quiz.command.PmStartQuiz;
import io.spine.testdata.Sample;
import io.spine.validate.AnyVBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static io.spine.base.Identifier.newUuid;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;

/**
 * @author Alexander Yevsyukov
 */
public class ProcessManagerTestEnv {

    /** Prevents instantiation of this utility class. */
    private ProcessManagerTestEnv() {
        // Do nothing.
    }

    public static PmQuizId newQuizId() {
        return PmQuizId.newBuilder()
                       .setId(newUuid())
                       .build();
    }

    public static PmStartQuiz startQuiz(PmQuizId id, Iterable<? extends PmQuestionId> problems) {
        return PmStartQuiz.newBuilder()
                          .setQuizId(id)
                          .addAllQuestion(problems)
                          .build();
    }

    public static PmAnswerQuestion answerQuestion(PmQuizId id, PmAnswer answer) {
        return PmAnswerQuestion.newBuilder()
                               .setQuizId(id)
                               .setAnswer(answer)
                               .build();
    }

    public static PmAnswer newAnswer() {
        return newAnswer(newQuestionId(), true);
    }

    private static PmQuestionId newQuestionId() {
        return PmQuestionId.newBuilder()
                           .setId(newUuid())
                           .build();
    }

    public static PmAnswer newAnswer(PmQuestionId id, boolean correct) {
        return PmAnswer.newBuilder()
                       .setQuestionId(id)
                       .setCorrect(correct)
                       .build();
    }

    /**
     * A test Process Manager which remembers past message as its state.
     */
    public static class TestProcessManager
            extends ProcessManager<ProjectId, Any, AnyVBuilder> {

        public TestProcessManager(ProjectId id) {
            super(id);
        }

        /**
         * Injects the passed CommandBus instance via Reflection since
         * {@link #setCommandBus(CommandBus)} is package-private and this
         * test environment class is outside of the parent's class package.
         */
        public void injectCommandBus(CommandBus commandBus) {
            try {
                final Method method = ProcessManager.class.getDeclaredMethod("setCommandBus",
                                                                             CommandBus.class);
                method.setAccessible(true);
                method.invoke(this, commandBus);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw illegalStateWithCauseOf(e);
            }
        }

        /*
         * Handled commands
         ********************/

        @Assign
        PmProjectCreated handle(PmCreateProject command, CommandContext ignored) {
            getBuilder().mergeFrom(pack(command));
            return ((PmProjectCreated.Builder) Sample.builderForType(PmProjectCreated.class))
                    .setProjectId(command.getProjectId())
                    .build();
        }

        @Assign
        PmTaskAdded handle(PmAddTask command, CommandContext ignored) {
            getBuilder().mergeFrom(pack(command));
            return ((PmTaskAdded.Builder) Sample.builderForType(PmTaskAdded.class))
                    .setProjectId(command.getProjectId())
                    .build();
        }

        @Assign
        CommandTransformed handle(PmStartProject command, CommandContext context) {
            getBuilder().mergeFrom(pack(command));

            Message addTask = ((PmAddTask.Builder)
                    Sample.builderForType(PmAddTask.class))
                    .setProjectId(command.getProjectId())
                    .build();
            CommandTransformed event = transform(command, context)
                    .to(addTask)
                    .post();
            return event;
        }

        /*
         * Reactions on events
         ************************/

        @React
        public Empty on(PmProjectCreated event, EventContext ignored) {
            getBuilder().mergeFrom(pack(event));
            return Empty.getDefaultInstance();
        }

        @React
        public Empty on(PmTaskAdded event, EventContext ignored) {
            getBuilder().mergeFrom(pack(event));
            return Empty.getDefaultInstance();
        }

        @React
        public Message on(PmProjectStarted event, EventContext ignored) {
            getBuilder().mergeFrom(pack(event));
            return Sample.messageOfType(PmNotificationSent.class);
        }


        /*
         * Reactions on rejections
         **************************/

        @React
        Empty on(EntityAlreadyArchived rejection, PmAddTask command) {
            getBuilder().mergeFrom(pack(command));
            return Empty.getDefaultInstance();
        }

        @React
        Empty on(EntityAlreadyArchived rejection) {
            getBuilder().mergeFrom(pack(rejection));
            return Empty.getDefaultInstance();
        }
    }

    /**
     * Helper dispatcher class to verify that the Process Manager routes
     * the {@link PmAddTask} command.
     */
    public static class AddTaskDispatcher implements CommandDispatcher<Message> {

        private final List<CommandEnvelope> commands = Lists.newLinkedList();

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(PmAddTask.class);
        }

        @Override
        public Message dispatch(CommandEnvelope envelope) {
            commands.add(envelope);
            return Empty.getDefaultInstance();
        }

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }

        @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK for tests.
        public List<CommandEnvelope> getCommands() {
            return commands;
        }
    }
}
