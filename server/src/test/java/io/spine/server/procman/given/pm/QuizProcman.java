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

package io.spine.server.procman.given.pm;

import io.spine.server.command.Assign;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.procman.ProcessManager;
import io.spine.server.tuple.EitherOf3;
import io.spine.test.procman.quiz.PmAnswer;
import io.spine.test.procman.quiz.PmQuestionId;
import io.spine.test.procman.quiz.PmQuiz;
import io.spine.test.procman.quiz.PmQuizId;
import io.spine.test.procman.quiz.PmQuizVBuilder;
import io.spine.test.procman.quiz.command.PmAnswerQuestion;
import io.spine.test.procman.quiz.command.PmStartQuiz;
import io.spine.test.procman.quiz.event.PmQuestionAnswered;
import io.spine.test.procman.quiz.event.PmQuestionFailed;
import io.spine.test.procman.quiz.event.PmQuestionSolved;
import io.spine.test.procman.quiz.event.PmQuizStarted;

import java.util.List;

/**
 * A quiz is started using {@link PmStartQuiz Start Quiz command} which defines a question set, and 
 * the question are answered using {@link PmAnswerQuestion Answer Question commands}.
 * 
 * @author Mykhailo Drachuk
 */
class QuizProcman extends ProcessManager<PmQuizId, PmQuiz, PmQuizVBuilder> {

    protected QuizProcman(PmQuizId id) {
        super(id);
    }

    @Assign
    PmQuizStarted handle(PmStartQuiz command) {
        getBuilder().setId(command.getQuizId());
        return PmQuizStarted.newBuilder()
                            .setQuizId(command.getQuizId())
                            .addAllQuestion(command.getQuestionList())
                            .build();
    }

    @Assign
    PmQuestionAnswered handle(PmAnswerQuestion command) {
        PmQuestionAnswered event =
                PmQuestionAnswered.newBuilder()
                                  .setQuizId(command.getQuizId())
                                  .setAnswer(command.getAnswer())
                                  .build();
        return event;
    }

    @React
    Nothing on(PmQuizStarted event) {
        getBuilder().setId(event.getQuizId());
        return nothing();
    }

    @React
    EitherOf3<PmQuestionSolved, PmQuestionFailed, Nothing> on(PmQuestionAnswered event) {
        PmAnswer answer = event.getAnswer();
        PmQuizId examId = event.getQuizId();
        PmQuestionId questionId = answer.getQuestionId();

        if (questionIsClosed(questionId)) {
            return EitherOf3.withC(nothing());
        }

        boolean answerIsCorrect = answer.getCorrect();
        if (answerIsCorrect) {
            PmQuestionSolved reaction =
                    PmQuestionSolved.newBuilder()
                                    .setQuizId(examId)
                                    .setQuestionId(questionId)
                                    .build();
            return EitherOf3.withA(reaction);
        } else {
            PmQuestionFailed reaction =
                    PmQuestionFailed.newBuilder()
                                    .setQuizId(examId)
                                    .setQuestionId(questionId)
                                    .build();
            return EitherOf3.withB(reaction);
        }
    }

    private boolean questionIsClosed(PmQuestionId questionId) {
        List<PmQuestionId> openQuestions = getBuilder().getOpenQuestion();
        boolean containedInOpenQuestions = openQuestions.contains(questionId);
        return !containedInOpenQuestions;
    }

    @React
    Nothing on(PmQuestionSolved event) {
        PmQuestionId questionId = event.getQuestionId();
        removeOpenQuestion(questionId);
        getBuilder().addSolvedQuestion(questionId);
        return nothing();
    }

    @React
    Nothing on(PmQuestionFailed event) {
        PmQuestionId questionId = event.getQuestionId();
        removeOpenQuestion(questionId);
        getBuilder().addFailedQuestion(questionId);
        return nothing();
    }

    private void removeOpenQuestion(PmQuestionId questionId) {
        List<PmQuestionId> openQuestions = getBuilder().getOpenQuestion();
        int index = openQuestions.indexOf(questionId);
        getBuilder().removeOpenQuestion(index);
    }
}
