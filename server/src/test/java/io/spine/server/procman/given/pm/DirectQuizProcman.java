/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import io.spine.test.procman.quiz.event.PmQuestionAlreadySolved;
import io.spine.test.procman.quiz.event.PmQuestionAnswered;
import io.spine.test.procman.quiz.event.PmQuestionFailed;
import io.spine.test.procman.quiz.event.PmQuestionSolved;
import io.spine.test.procman.quiz.event.PmQuizStarted;

import java.util.List;

/**
 * A quiz is started using {@link PmStartQuiz Start Quiz command} which defines a question set, and 
 * the questions are answered using {@link PmAnswerQuestion Answer Question commands}.
 * 
 * <p>Differs from the {@link QuizProcman} by scarcing the interjacent
 * {@link PmQuestionAnswered Question Answered event} and emits 
 * either of three when handling a command.
 */
class DirectQuizProcman extends ProcessManager<PmQuizId, PmQuiz, PmQuizVBuilder> {

    protected DirectQuizProcman(PmQuizId id) {
        super(id);
    }

    @Assign
    PmQuizStarted handle(PmStartQuiz command) {
        builder().setId(command.getQuizId());
        return PmQuizStarted.newBuilder()
                            .setQuizId(command.getQuizId())
                            .addAllQuestion(command.getQuestionList())
                            .build();
    }

    @Assign
    EitherOf3<PmQuestionSolved, PmQuestionFailed, PmQuestionAlreadySolved>
    handle(PmAnswerQuestion command) {
        PmAnswer answer = command.getAnswer();
        PmQuizId examId = command.getQuizId();
        PmQuestionId questionId = answer.getQuestionId();

        if (questionIsClosed(questionId)) {
            PmQuestionAlreadySolved event = PmQuestionAlreadySolved
                    .newBuilder()
                    .setQuizId(examId)
                    .setQuestionId(questionId)
                    .build();
            return EitherOf3.withC(event);
        }

        boolean answerIsCorrect = answer.getCorrect();
        if (answerIsCorrect) {
            PmQuestionSolved reaction =
                    PmQuestionSolved
                            .newBuilder()
                            .setQuizId(examId)
                            .setQuestionId(questionId)
                            .build();
            return EitherOf3.withA(reaction);
        } else {
            PmQuestionFailed reaction =
                    PmQuestionFailed
                            .newBuilder()
                            .setQuizId(examId)
                            .setQuestionId(questionId)
                            .build();
            return EitherOf3.withB(reaction);
        }
    }

    private boolean questionIsClosed(PmQuestionId questionId) {
        List<PmQuestionId> openQuestions = builder().getOpenQuestion();
        boolean containedInOpenQuestions = openQuestions.contains(questionId);
        return !containedInOpenQuestions;
    }

    @React
    Nothing on(PmQuizStarted event) {
        builder().setId(event.getQuizId());
        return nothing();
    }

    @React
    Nothing on(PmQuestionSolved event) {
        PmQuestionId questionId = event.getQuestionId();
        removeOpenQuestion(questionId);
        builder().addSolvedQuestion(questionId);
        return nothing();
    }

    @React
    Nothing on(PmQuestionFailed event) {
        PmQuestionId questionId = event.getQuestionId();
        removeOpenQuestion(questionId);
        builder().addFailedQuestion(questionId);
        return nothing();
    }

    private void removeOpenQuestion(PmQuestionId questionId) {
        PmQuizVBuilder builder = builder();
        List<PmQuestionId> openQuestions = builder.getOpenQuestion();
        int index = openQuestions.indexOf(questionId);
        builder.removeOpenQuestion(index);
    }
}
