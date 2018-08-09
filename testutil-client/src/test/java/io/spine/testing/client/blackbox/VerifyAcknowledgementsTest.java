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

package io.spine.testing.client.blackbox;

import com.google.common.collect.ImmutableList;
import io.spine.core.RejectionClass;
import io.spine.testing.client.blackbox.Rejections.BbProjectAlreadyStarted;
import io.spine.testing.client.blackbox.Rejections.BbTaskCreatedInCompletedProject;
import io.spine.testing.client.blackbox.Rejections.BbTaskLimitReached;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.client.blackbox.VerifyAcknowledgements.acked;
import static io.spine.testing.client.blackbox.VerifyAcknowledgements.ackedWithErrors;
import static io.spine.testing.client.blackbox.VerifyAcknowledgements.ackedWithRejections;
import static io.spine.testing.client.blackbox.Count.count;
import static io.spine.testing.client.blackbox.Count.once;
import static io.spine.testing.client.blackbox.Count.thrice;
import static io.spine.testing.client.blackbox.Count.twice;
import static io.spine.testing.client.blackbox.ErrorCriterion.withType;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.DUPLICATE_ERROR_TYPE;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.DUPLICATE_TASK_TITLE;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.MISSING_ERROR_TYPE;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.MISSING_TASK_TITLE;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.PRESENT_ERROR_TYPE;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.PRESENT_TASK_TITLE;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.newError;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.newErrorAck;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.newOkAck;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.newRejectionAck;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.newTask;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.projectAlreadyStarted;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.taskCreatedInCompletedProject;
import static io.spine.testing.client.blackbox.given.CommandAcksTestEnv.taskLimitReached;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mykhailo Drachuk
 */
@DisplayName("Acknowledgements Verifier should")
class VerifyAcknowledgementsTest {

    private Acknowledgements acks;

    @BeforeEach
    void setUp() {
        acks = new Acknowledgements(asList(newOkAck(), newOkAck(), newOkAck()));
    }

    @Test
    @DisplayName("verifies acknowledgements count")
    void countAny() {
        verify(acked(thrice()));
        assertThrows(AssertionError.class, () -> verify(acked(twice())));
        assertThrows(AssertionError.class, () -> verify(acked(count(4))));
    }

    @Test
    @DisplayName("verifies acknowledgements count without rejections or errors")
    void countWithoutRejectionsOrErrors() {
        verify(acked(thrice()).withoutErrorsOrRejections());

        assertThrows(AssertionError.class, () -> verify(acked(thrice()).withRejections()));
        assertThrows(AssertionError.class, () -> verify(acked(thrice()).withErrors()));
    }

    @Test
    @DisplayName("verify with rejection and with error rules")
    void containingRejectionsAndErrors() {
        Acknowledgements diverseAcks = new Acknowledgements(asList(
                newOkAck(),
                newRejectionAck(taskLimitReached()),
                newErrorAck()
        ));

        verify(acked(thrice()).withRejections().and(ackedWithErrors()), diverseAcks);
        assertThrows(AssertionError.class, () -> 
                verify(acked(thrice()).withoutRejections(), diverseAcks));
        assertThrows(AssertionError.class, () -> 
                verify(acked(thrice()).withoutErrors(), diverseAcks));
    }

    @Test
    @DisplayName("verify errors total amount")
    void countErrors() {
        Acknowledgements errorAcks = new Acknowledgements(ImmutableList.of(
                newErrorAck(),
                newErrorAck(),
                newErrorAck()
        ));

        verify(acked(thrice()).withErrors(thrice()), errorAcks);
        assertThrows(AssertionError.class, () -> verify(ackedWithErrors(twice()), errorAcks));
    }

    @Test
    @DisplayName("verify error presence by a criterion")
    void containErrorBycriterion() {
        Acknowledgements errorAcks = new Acknowledgements(ImmutableList.of(
                newErrorAck(newError(PRESENT_ERROR_TYPE))
        ));

        verify(acked(once()).withErrors(withType(PRESENT_ERROR_TYPE)), errorAcks);
        assertThrows(AssertionError.class, () ->
                verify(acked(once()).withErrors(withType(MISSING_ERROR_TYPE)), errorAcks));
    }

    @Test
    @DisplayName("verify error count by a criterion")
    void countErrorBycriterion() {
        Acknowledgements errorAcks = new Acknowledgements(ImmutableList.of(
                newErrorAck(newError(DUPLICATE_ERROR_TYPE)),
                newErrorAck(newError(DUPLICATE_ERROR_TYPE))
        ));

        verify(acked(twice()).withErrors(withType(DUPLICATE_ERROR_TYPE), twice()), errorAcks);
        assertThrows(AssertionError.class, () ->
                verify(ackedWithErrors(withType(DUPLICATE_ERROR_TYPE), once()), errorAcks));
    }

    @Test
    @DisplayName("verify rejection by type")
    void containRejectionByType() {
        Acknowledgements rejectionAcks = new Acknowledgements(ImmutableList.of(
                newRejectionAck(projectAlreadyStarted()),
                newRejectionAck(taskLimitReached()),
                newRejectionAck(taskLimitReached())
        ));

        RejectionClass taskLimitReached = RejectionClass.of(BbTaskLimitReached.class);
        Class<BbProjectAlreadyStarted> projectAlreadyStarted = BbProjectAlreadyStarted.class;
        verify(acked(thrice()).withRejections(projectAlreadyStarted), rejectionAcks);
        verify(acked(thrice()).withRejections(taskLimitReached), rejectionAcks);

        assertThrows(AssertionError.class, () -> {
            RejectionClass taskInCompletedProject =
                    RejectionClass.of(BbTaskCreatedInCompletedProject.class);
            verify(acked(thrice()).withRejections(taskInCompletedProject), rejectionAcks);
        });
    }

    @Test
    @DisplayName("verify rejection by predicate")
    void containRejectionByPredicate() {
        Acknowledgements rejectionAcks = new Acknowledgements(ImmutableList.of(
                newRejectionAck(projectAlreadyStarted()),
                newRejectionAck(taskCreatedInCompletedProject(newTask(PRESENT_TASK_TITLE)))
        ));

        RejectionCriterion<BbTaskCreatedInCompletedProject> whichIsPresent =
                rejection -> PRESENT_TASK_TITLE.equals(getTaskTitle(rejection));
        Class<BbTaskCreatedInCompletedProject> taskInCompleteProject =
                BbTaskCreatedInCompletedProject.class;
        verify(acked(twice()).withRejections(taskInCompleteProject, whichIsPresent), 
               rejectionAcks);

        assertThrows(AssertionError.class, () -> {
            RejectionCriterion<BbTaskCreatedInCompletedProject> whichIsMissing =
                    rejection -> MISSING_TASK_TITLE.equals(getTaskTitle(rejection));
            verify(acked(twice()).withRejections(taskInCompleteProject, whichIsMissing),
                   rejectionAcks);
        });
    }

    private static String getTaskTitle(BbTaskCreatedInCompletedProject rejection) {
        return rejection.getTask()
                        .getTitle();
    }

    @Test
    @DisplayName("verify rejection count")
    void containRejectionCount() {
        Acknowledgements rejectionAcks = new Acknowledgements(ImmutableList.of(
                newRejectionAck(projectAlreadyStarted()),
                newRejectionAck(taskLimitReached()),
                newRejectionAck(taskLimitReached()),
                newRejectionAck(taskCreatedInCompletedProject(newTask(DUPLICATE_TASK_TITLE))),
                newRejectionAck(taskCreatedInCompletedProject(newTask(DUPLICATE_TASK_TITLE)))
        ));

        RejectionClass taskLimitReached = RejectionClass.of(BbTaskLimitReached.class);
        Class<BbProjectAlreadyStarted> projectAlreadyStarted = BbProjectAlreadyStarted.class;

        verify(ackedWithRejections(count(5)), rejectionAcks);
        verify(acked(count(5)).withRejections(projectAlreadyStarted, once()), rejectionAcks);
        verify(acked(count(5)).withRejections(taskLimitReached, twice()), rejectionAcks);

        assertThrows(AssertionError.class, () ->
                verify(ackedWithRejections(count(4)), rejectionAcks));
        assertThrows(AssertionError.class, () ->
                verify(ackedWithRejections(projectAlreadyStarted, twice()), rejectionAcks));

        RejectionCriterion<BbTaskCreatedInCompletedProject> withDuplicateName =
                rejection -> DUPLICATE_TASK_TITLE.equals(getTaskTitle(rejection));
        Class<BbTaskCreatedInCompletedProject> taskInCompleteProject =
                BbTaskCreatedInCompletedProject.class;

        verify(acked(count(5)).withRejections(taskInCompleteProject, twice(), withDuplicateName),
               rejectionAcks);

        assertThrows(AssertionError.class, () ->
                verify(ackedWithRejections(taskInCompleteProject, thrice(), withDuplicateName),
                       rejectionAcks));
    }

    private void verify(VerifyAcknowledgements verifier) {
        verify(verifier, acks);
    }

    private static void verify(VerifyAcknowledgements verifier, Acknowledgements acks) {
        verifier.verify(acks);
    }
}
