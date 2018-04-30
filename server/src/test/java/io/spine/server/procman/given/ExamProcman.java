/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Empty;
import io.spine.core.React;
import io.spine.server.command.Assign;
import io.spine.server.procman.ProcessManager;
import io.spine.server.tuple.EitherOfThree;
import io.spine.test.procman.exam.PmExam;
import io.spine.test.procman.exam.PmExamId;
import io.spine.test.procman.exam.PmExamVBuilder;
import io.spine.test.procman.exam.PmProblemAnswer;
import io.spine.test.procman.exam.PmProblemId;
import io.spine.test.procman.exam.command.PmAnswerProblem;
import io.spine.test.procman.exam.command.PmStartExam;
import io.spine.test.procman.exam.event.PmExamStarted;
import io.spine.test.procman.exam.event.PmProblemAnswered;
import io.spine.test.procman.exam.event.PmProblemFailed;
import io.spine.test.procman.exam.event.PmProblemSolved;

import java.util.List;

class ExamProcman extends ProcessManager<PmExamId, PmExam, PmExamVBuilder> {

    protected ExamProcman(PmExamId id) {
        super(id);
    }

    @Assign
    PmExamStarted handle(PmStartExam command) {
        return PmExamStarted.newBuilder()
                            .setExamId(command.getExamId())
                            .addAllProblem(command.getProblemList())
                            .build();
    }

    @Assign
    PmProblemAnswered handle(PmAnswerProblem command) {
        final PmProblemAnswered event =
                PmProblemAnswered.newBuilder()
                                 .setExamId(command.getExamId())
                                 .setAnswer(command.getAnswer())
                                 .build();
        return event;
    }

    @React
    void on(PmExamStarted event) {
        getBuilder().setId(event.getExamId());
    }

    @React
    EitherOfThree<PmProblemSolved, PmProblemFailed, Empty> on(PmProblemAnswered event) {
        final PmProblemAnswer answer = event.getAnswer();
        final PmExamId examId = event.getExamId();
        final PmProblemId problemId = answer.getProblemId();

        if (problemIsClosed(problemId)) {
            return EitherOfThree.withC(Empty.getDefaultInstance());
        }

        final boolean answerIsCorrect = answer.getCorrect();
        if (answerIsCorrect) {
            final PmProblemSolved reaction =
                    PmProblemSolved.newBuilder()
                                   .setExamId(examId)
                                   .setProblemId(problemId)
                                   .build();
            return EitherOfThree.withA(reaction);
        } else {
            final PmProblemFailed reaction =
                    PmProblemFailed.newBuilder()
                                   .setExamId(examId)
                                   .setProblemId(problemId)
                                   .build();
            return EitherOfThree.withB(reaction);
        }
    }

    private boolean problemIsClosed(final PmProblemId problemId) {
        final List<PmProblemId> openProblems = getBuilder().getOpenProblem();
        final boolean containedInOpenProblems = openProblems.contains(problemId);
        return !containedInOpenProblems;
    }

    @React
    void on(PmProblemSolved event) {
        final PmProblemId problemId = event.getProblemId();
        removeOpenProblem(problemId);
        getBuilder().addSolvedProblem(problemId);
    }

    @React
    void on(PmProblemFailed event) {
        final PmProblemId problemId = event.getProblemId();
        removeOpenProblem(problemId);
        getBuilder().addFailedProblem(problemId);
    }

    private void removeOpenProblem(PmProblemId problemId) {
        final List<PmProblemId> openProblems = getBuilder().getOpenProblem();
        final int index = openProblems.indexOf(problemId);
        getBuilder().removeOpenProblem(index);
    }
}
