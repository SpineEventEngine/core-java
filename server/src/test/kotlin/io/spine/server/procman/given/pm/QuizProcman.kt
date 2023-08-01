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
package io.spine.server.procman.given.pm

import io.spine.server.command.Assign
import io.spine.server.event.React
import io.spine.server.model.Nothing
import io.spine.server.procman.ProcessManager
import io.spine.server.procman.ProcessManagerRepository
import io.spine.server.tuple.EitherOf3
import io.spine.test.procman.quiz.PmQuestionId
import io.spine.test.procman.quiz.PmQuiz
import io.spine.test.procman.quiz.PmQuizId
import io.spine.test.procman.quiz.command.PmAnswerQuestion
import io.spine.test.procman.quiz.command.PmStartQuiz
import io.spine.test.procman.quiz.event.PmQuestionAnswered
import io.spine.test.procman.quiz.event.PmQuestionFailed
import io.spine.test.procman.quiz.event.PmQuestionSolved
import io.spine.test.procman.quiz.event.PmQuizStarted
import io.spine.test.procman.quiz.event.pmQuestionAnswered
import io.spine.test.procman.quiz.event.pmQuestionFailed
import io.spine.test.procman.quiz.event.pmQuestionSolved
import io.spine.test.procman.quiz.event.pmQuizStarted

/**
 * A quiz is started using [Start Quiz command][PmStartQuiz] which defines a question set, and
 * the question are answered using [Answer Question commands][PmAnswerQuestion].
 */
internal class QuizProcman(id: PmQuizId) : ProcessManager<PmQuizId, PmQuiz, PmQuiz.Builder>(id) {

    @Assign
    fun handle(command: PmStartQuiz): PmQuizStarted {
        builder().setId(command.quiz)
        return pmQuizStarted {
            quiz = command.quiz
            question.addAll(command.questionList)
        }
    }

    @Assign
    fun handle(command: PmAnswerQuestion): PmQuestionAnswered = pmQuestionAnswered {
        quiz = command.quiz
        answer = command.answer
    }

    @React
    fun on(event: PmQuizStarted): Nothing {
        builder().setId(event.quiz)
        return nothing()
    }

    @React
    fun on(event: PmQuestionAnswered): EitherOf3<PmQuestionSolved, PmQuestionFailed, Nothing> {
        val answer = event.answer
        val question = answer.question
        if (question.isClosed()) {
            return EitherOf3.withC(nothing())
        }
        return if (answer.correct) {
            EitherOf3.withA(pmQuestionSolved {
                quiz = event.quiz
                this.question = question
            })
        } else {
            EitherOf3.withB(pmQuestionFailed {
                quiz = event.quiz
                this.question = question
            })
        }
    }

    private fun PmQuestionId.isClosed(): Boolean {
        val openQuestions = builder().openQuestionList
        val isOpen = openQuestions.contains(this)
        return !isOpen
    }

    @React
    fun on(event: PmQuestionSolved): Nothing {
        val questionId = event.question
        removeOpenQuestion(questionId)
        builder().addSolvedQuestion(questionId)
        return nothing()
    }

    @React
    fun on(event: PmQuestionFailed): Nothing {
        val questionId = event.question
        removeOpenQuestion(questionId)
        builder()!!.addFailedQuestion(questionId)
        return nothing()
    }

    private fun removeOpenQuestion(question: PmQuestionId) {
        val openQuestions = builder().openQuestionList
        val index = openQuestions.indexOf(question)
        builder().removeOpenQuestion(index)
    }
}

internal class QuizProcmanRepository : ProcessManagerRepository<PmQuizId, QuizProcman, PmQuiz>()
