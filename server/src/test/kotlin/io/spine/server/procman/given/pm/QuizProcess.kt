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

import io.spine.core.Subscribe
import io.spine.server.command.Assign
import io.spine.server.entity.alter
import io.spine.server.event.React
import io.spine.server.model.Nothing
import io.spine.server.procman.ProcessManager
import io.spine.server.projection.Projection
import io.spine.server.tuple.EitherOf2
import io.spine.server.tuple.EitherOf3
import io.spine.test.procman.quiz.PmQuestionId
import io.spine.test.procman.quiz.PmQuiz
import io.spine.test.procman.quiz.PmQuizId
import io.spine.test.procman.quiz.PmQuizStats
import io.spine.test.procman.quiz.command.PmAnswerQuestion
import io.spine.test.procman.quiz.command.PmStartQuiz
import io.spine.test.procman.quiz.event.PmQuestionAnswered
import io.spine.test.procman.quiz.event.PmQuestionFailed
import io.spine.test.procman.quiz.event.PmQuestionSolved
import io.spine.test.procman.quiz.event.PmQuizFinished
import io.spine.test.procman.quiz.event.PmQuizStarted
import io.spine.test.procman.quiz.event.pmQuestionAnswered
import io.spine.test.procman.quiz.event.pmQuestionFailed
import io.spine.test.procman.quiz.event.pmQuestionSolved
import io.spine.test.procman.quiz.event.pmQuizFinished
import io.spine.test.procman.quiz.event.pmQuizStarted

/**
 * A quiz is started using [Start Quiz command][PmStartQuiz] which defines a question set, and
 * the question are answered using [Answer Question commands][PmAnswerQuestion].
 */
internal class QuizProcess(id: PmQuizId) : ProcessManager<PmQuizId, PmQuiz, PmQuiz.Builder>(id) {

    @Assign
    fun handle(command: PmStartQuiz): PmQuizStarted {
        alter {
            id = command.quiz
            addAllOpenQuestion(command.questionList)
        }
        return pmQuizStarted {
            quiz = command.quiz
            question.addAll(command.questionList)
        }
    }

    @Assign
    fun handle(command: PmAnswerQuestion): PmQuestionAnswered =
        pmQuestionAnswered {
            quiz = command.quiz
            answer = command.answer
        }

    @React
    fun on(event: PmQuizStarted): Nothing {
        alter { id = event.quiz }
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
    fun on(event: PmQuestionSolved): EitherOf2<Nothing, PmQuizFinished> {
        val question = event.question
        alter {
            removeOpenQuestion(question)
            addSolvedQuestion(question)
        }
        return onAnsweredQuestion()
    }

    @React
    fun on(event: PmQuestionFailed): EitherOf2<Nothing, PmQuizFinished> {
        val question = event.question
        alter {
            removeOpenQuestion(question)
            addFailedQuestion(question)
        }
        return onAnsweredQuestion()
    }

    private fun PmQuiz.Builder.removeOpenQuestion(question: PmQuestionId) {
        val openQuestions = openQuestionList
        val index = openQuestions.indexOf(question)
        removeOpenQuestion(index)
    }

    private fun onAnsweredQuestion(): EitherOf2<Nothing, PmQuizFinished> {
        return if (builder().openQuestionList.isEmpty()) {
            val loaded = select(PmQuizStats::class.java).findById(id())
            EitherOf2.withB(pmQuizFinished {
                quiz = id()
                loaded?.let {
                    stats = it
                }
            })
        } else {
            EitherOf2.withA(nothing())
        }
    }
}

internal class QuizStatsView: Projection<PmQuizId, PmQuizStats, PmQuizStats.Builder>() {

    @Subscribe
    fun on(event: PmQuizStarted) = alter {
        id = event.quiz
    }

    @Subscribe
    @Suppress("UNUSED_PARAMETER")
    fun on(event: PmQuestionAnswered) = alter {
        solvedQuestions += 1
    }

    @Subscribe
    @Suppress("UNUSED_PARAMETER")
    fun on(event: PmQuestionFailed) = alter {
        failedQuestions += 1
    }
}
