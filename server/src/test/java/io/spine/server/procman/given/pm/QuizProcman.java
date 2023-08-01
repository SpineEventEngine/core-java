/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.server.procman.given.pm;

import io.spine.server.command.Assign;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.procman.ProcessManager;
import io.spine.server.tuple.EitherOf3;
import io.spine.test.procman.quiz.PmQuestionId;
import io.spine.test.procman.quiz.PmQuiz;
import io.spine.test.procman.quiz.PmQuizId;
import io.spine.test.procman.quiz.command.PmAnswerQuestion;
import io.spine.test.procman.quiz.command.PmStartQuiz;
import io.spine.test.procman.quiz.event.PmQuestionAnswered;
import io.spine.test.procman.quiz.event.PmQuestionFailed;
import io.spine.test.procman.quiz.event.PmQuestionSolved;
import io.spine.test.procman.quiz.event.PmQuizStarted;

/**
 * A quiz is started using {@link PmStartQuiz Start Quiz command} which defines a question set, and 
 * the question are answered using {@link PmAnswerQuestion Answer Question commands}.
 */
class QuizProcman extends ProcessManager<PmQuizId, PmQuiz, PmQuiz.Builder> {

    protected QuizProcman(PmQuizId id) {
        super(id);
    }

    @Assign
    PmQuizStarted handle(PmStartQuiz command) {
        builder().setId(command.getQuiz());
        return PmQuizStarted.newBuilder()
                .setQuiz(command.getQuiz())
                .addAllQuestion(command.getQuestionList())
                .build();
    }

    @Assign
    PmQuestionAnswered handle(PmAnswerQuestion command) {
        var event = PmQuestionAnswered.newBuilder()
                .setQuiz(command.getQuiz())
                .setAnswer(command.getAnswer())
                .build();
        return event;
    }

    @React
    Nothing on(PmQuizStarted event) {
        builder().setId(event.getQuiz());
        return nothing();
    }

    @React
    EitherOf3<PmQuestionSolved, PmQuestionFailed, Nothing> on(PmQuestionAnswered event) {
        var answer = event.getAnswer();
        var quiz = event.getQuiz();
        var questionId = answer.getQuestion();

        if (questionIsClosed(questionId)) {
            return EitherOf3.withC(nothing());
        }

        var answerIsCorrect = answer.getCorrect();
        if (answerIsCorrect) {
            var reaction = PmQuestionSolved.newBuilder()
                    .setQuiz(quiz)
                    .setQuestion(questionId)
                    .build();
            return EitherOf3.withA(reaction);
        } else {
            var reaction = PmQuestionFailed.newBuilder()
                    .setQuiz(quiz)
                    .setQuestion(questionId)
                    .build();
            return EitherOf3.withB(reaction);
        }
    }

    private boolean questionIsClosed(PmQuestionId questionId) {
        var openQuestions = builder().getOpenQuestionList();
        var containedInOpenQuestions = openQuestions.contains(questionId);
        return !containedInOpenQuestions;
    }

    @React
    Nothing on(PmQuestionSolved event) {
        var questionId = event.getQuestion();
        removeOpenQuestion(questionId);
        builder().addSolvedQuestion(questionId);
        return nothing();
    }

    @React
    Nothing on(PmQuestionFailed event) {
        var questionId = event.getQuestion();
        removeOpenQuestion(questionId);
        builder().addFailedQuestion(questionId);
        return nothing();
    }

    private void removeOpenQuestion(PmQuestionId questionId) {
        var openQuestions = builder().getOpenQuestionList();
        var index = openQuestions.indexOf(questionId);
        builder().removeOpenQuestion(index);
    }
}
