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

package io.spine.server.integration;

import com.google.common.collect.ImmutableList;
import io.spine.core.RejectionClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.integration.AcknowledgementsVerifier.acked;
import static io.spine.server.integration.AcknowledgementsVerifier.ackedWithErrors;
import static io.spine.server.integration.AcknowledgementsVerifier.ackedWithRejections;
import static io.spine.server.integration.ErrorCriteria.withType;
import static io.spine.server.integration.given.CommandAcksTestEnv.DUPLICATE_ERROR_TYPE;
import static io.spine.server.integration.given.CommandAcksTestEnv.DUPLICATE_TASK_TITLE;
import static io.spine.server.integration.given.CommandAcksTestEnv.MISSING_ERROR_TYPE;
import static io.spine.server.integration.given.CommandAcksTestEnv.MISSING_TASK_TITLE;
import static io.spine.server.integration.given.CommandAcksTestEnv.PRESENT_ERROR_TYPE;
import static io.spine.server.integration.given.CommandAcksTestEnv.PRESENT_TASK_TITLE;
import static io.spine.server.integration.given.CommandAcksTestEnv.newError;
import static io.spine.server.integration.given.CommandAcksTestEnv.newErrorAck;
import static io.spine.server.integration.given.CommandAcksTestEnv.newOkAck;
import static io.spine.server.integration.given.CommandAcksTestEnv.newRejectionAck;
import static io.spine.server.integration.given.CommandAcksTestEnv.newTask;
import static io.spine.server.integration.given.CommandAcksTestEnv.projectAlreadyStarted;
import static io.spine.server.integration.given.CommandAcksTestEnv.taskCreatedInCompletedProject;
import static io.spine.server.integration.given.CommandAcksTestEnv.taskLimitReached;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mykhailo Drachuk
 */
@DisplayName("Acknowledgements Verifier should")
class AcknowledgementsVerifierTest {

    private Acknowledgements acks;

    @BeforeEach
    void setUp() {
        acks = new Acknowledgements(asList(newOkAck(), newOkAck(), newOkAck()));
    }

    @Test
    @DisplayName("verifies acknowledgements count")
    void count() {
        verify(acked(3));
        assertThrows(AssertionError.class, () -> verify(acked(2)));
        assertThrows(AssertionError.class, () -> verify(acked(4)));
    }

    @Test
    @DisplayName("verifies acknowledgements count without rejections or errors")
    void countWithoutRejectionsOrErrors() {
        verify(acked(3).withoutErrorsOrRejections());

        assertThrows(AssertionError.class, () -> verify(acked(3).withRejections()));
        assertThrows(AssertionError.class, () -> verify(acked(3).withErrors()));
    }

    @Test
    @DisplayName("verify with rejection and with error rules")
    void containingRejectionsAndErrors() {
        Acknowledgements diverseAcks = new Acknowledgements(asList(
                newOkAck(),
                newRejectionAck(taskLimitReached()),
                newErrorAck()
        ));

        verify(acked(3).withRejections()
                       .and(ackedWithErrors()), diverseAcks);
        assertThrows(AssertionError.class, () -> verify(acked(3).withoutRejections(), diverseAcks));
        assertThrows(AssertionError.class, () -> verify(acked(3).withoutErrors(), diverseAcks));
    }
    
    @Test
    @DisplayName("verify errors total amount")
    void countErrors() {
        Acknowledgements errorAcks = new Acknowledgements(ImmutableList.of(
                newErrorAck(),
                newErrorAck(),
                newErrorAck()
        ));

        verify(acked(3).withErrors(3), errorAcks);
        assertThrows(AssertionError.class, () -> verify(ackedWithErrors(2), errorAcks));
    }


    @Test
    @DisplayName("verify error presence by a criteria")
    void containErrorByCriteria() {
        Acknowledgements errorAcks = new Acknowledgements(ImmutableList.of(
                newErrorAck(newError(PRESENT_ERROR_TYPE))
        ));

        verify(acked(1).withErrors(withType(PRESENT_ERROR_TYPE)), errorAcks);
        assertThrows(AssertionError.class, () ->
                verify(acked(1).withErrors(withType(MISSING_ERROR_TYPE)), errorAcks));
    }

    @Test
    @DisplayName("verify error count by a criteria")
    void countErrorByCriteria() {
        Acknowledgements errorAcks = new Acknowledgements(ImmutableList.of(
                newErrorAck(newError(DUPLICATE_ERROR_TYPE)),
                newErrorAck(newError(DUPLICATE_ERROR_TYPE))
        ));

        verify(acked(2).withErrors(2, withType(DUPLICATE_ERROR_TYPE)), errorAcks);
        assertThrows(AssertionError.class, () ->
                verify(ackedWithErrors(1, withType(DUPLICATE_ERROR_TYPE)), errorAcks));
    }

    @Test
    @DisplayName("verify rejection by type")
    void containRejectionByType() {
        Acknowledgements rejectionAcks = new Acknowledgements(ImmutableList.of(
                newRejectionAck(projectAlreadyStarted()),
                newRejectionAck(taskLimitReached()),
                newRejectionAck(taskLimitReached())
        ));

        RejectionClass taskLimitReached = RejectionClass.of(Rejections.IntTaskLimitReached.class);
        Class<Rejections.IntProjectAlreadyStarted> projectAlreadyStarted =
                Rejections.IntProjectAlreadyStarted.class;
        verify(acked(3).withRejections(projectAlreadyStarted), rejectionAcks);
        verify(acked(3).withRejections(taskLimitReached), rejectionAcks);

        assertThrows(AssertionError.class, () -> {
            RejectionClass taskInCompletedProject =
                    RejectionClass.of(Rejections.IntTaskCreatedInCompletedProject.class);
            verify(acked(3).withRejections(taskInCompletedProject), rejectionAcks);
        });
    }

    @Test
    @DisplayName("verify rejection by predicate")
    void containRejectionByPredicate() {
        Acknowledgements rejectionAcks = new Acknowledgements(ImmutableList.of(
                newRejectionAck(projectAlreadyStarted()),
                newRejectionAck(taskCreatedInCompletedProject(newTask(PRESENT_TASK_TITLE)))
        ));

        RejectionPredicate<Rejections.IntTaskCreatedInCompletedProject> whichIsPresent =
                rejection -> PRESENT_TASK_TITLE.equals(getTaskTitle(rejection));
        Class<Rejections.IntTaskCreatedInCompletedProject> taskInCompleteProject =
                Rejections.IntTaskCreatedInCompletedProject.class;
        verify(acked(2).withRejections(taskInCompleteProject, whichIsPresent), rejectionAcks);

        assertThrows(AssertionError.class, () -> {
            RejectionPredicate<Rejections.IntTaskCreatedInCompletedProject> whichIsMissing =
                    rejection -> MISSING_TASK_TITLE.equals(getTaskTitle(rejection));
            verify(acked(2).withRejections(taskInCompleteProject, whichIsMissing), rejectionAcks);
        });
    }

    private static String getTaskTitle(Rejections.IntTaskCreatedInCompletedProject rejection) {
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

        RejectionClass taskLimitReached = RejectionClass.of(Rejections.IntTaskLimitReached.class);
        Class<Rejections.IntProjectAlreadyStarted> projectAlreadyStarted
                = Rejections.IntProjectAlreadyStarted.class;

        verify(ackedWithRejections(5), rejectionAcks);
        verify(acked(5).withRejections(1, projectAlreadyStarted), rejectionAcks);
        verify(acked(5).withRejections(2, taskLimitReached), rejectionAcks);

        assertThrows(AssertionError.class, () -> verify(ackedWithRejections(4), rejectionAcks));
        assertThrows(AssertionError.class, () ->
                verify(ackedWithRejections(2, projectAlreadyStarted), rejectionAcks));


        RejectionPredicate<Rejections.IntTaskCreatedInCompletedProject> withDuplicateName =
                rejection -> DUPLICATE_TASK_TITLE.equals(getTaskTitle(rejection));
        Class<Rejections.IntTaskCreatedInCompletedProject> taskInCompleteProject =
                Rejections.IntTaskCreatedInCompletedProject.class;
        
        verify(acked(5).withRejections(2, taskInCompleteProject, withDuplicateName), rejectionAcks);
        
        assertThrows(AssertionError.class, () ->
                verify(ackedWithRejections(3, taskInCompleteProject, withDuplicateName),
                       rejectionAcks));
    }

    private void verify(AcknowledgementsVerifier verifier) {
        verify(verifier, acks);
    }

    private static void verify(AcknowledgementsVerifier verifier, Acknowledgements acks) {
        verifier.verify(acks);
    }
}
