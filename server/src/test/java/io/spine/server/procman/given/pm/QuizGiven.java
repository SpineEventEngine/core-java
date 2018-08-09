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

import io.spine.test.procman.quiz.PmAnswer;
import io.spine.test.procman.quiz.PmQuestionId;
import io.spine.test.procman.quiz.PmQuizId;
import io.spine.test.procman.quiz.command.PmAnswerQuestion;
import io.spine.test.procman.quiz.command.PmStartQuiz;

import static io.spine.base.Identifier.newUuid;

/**
 * Factory methods for creating test values.
 *
 * @author Alexander Yevsyukov
 */
public class QuizGiven {

    /** Prevents instantiation of this utility class. */
    private QuizGiven() {
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
}
