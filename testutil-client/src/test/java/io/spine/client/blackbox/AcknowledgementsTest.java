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

package io.spine.client.blackbox;

import com.google.common.collect.ImmutableList;
import io.spine.client.blackbox.given.CommandAcksTestEnv;
import io.spine.core.Ack;
import io.spine.core.RejectionClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.client.blackbox.ErrorCriterion.withType;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.DUPLICATE_ERROR_TYPE;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.DUPLICATE_TASK_TITLE;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.MISSING_ERROR_TYPE;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.MISSING_TASK_TITLE;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.PRESENT_ERROR_TYPE;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.PRESENT_TASK_TITLE;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.UNIQUE_ERROR_TYPE;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.UNIQUE_TASK_TITLEE;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.acks;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.concat;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.newError;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.newErrorAck;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.newRejectionAck;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.newTask;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.projectAlreadyStarted;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.taskCreatedInCompletedProject;
import static io.spine.client.blackbox.given.CommandAcksTestEnv.taskLimitReached;
import static io.spine.test.client.blackbox.Rejections.IntProjectAlreadyStarted;
import static io.spine.test.client.blackbox.Rejections.IntTaskCreatedInCompletedProject;
import static io.spine.test.client.blackbox.Rejections.IntTaskLimitReached;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mykhailo Drachuk
 */
@DisplayName("Acknowledgements should")
class AcknowledgementsTest {

    @Test
    @DisplayName("return proper total acknowledgements count")
    void count() {
        Acknowledgements noAcks = new Acknowledgements(newArrayList());
        assertEquals(0, noAcks.count());

        Acknowledgements ack = new Acknowledgements(acks(1, CommandAcksTestEnv::newOkAck));
        assertEquals(1, ack.count());

        Acknowledgements twoAcks = new Acknowledgements(acks(2, CommandAcksTestEnv::newOkAck));
        assertEquals(2, twoAcks.count());

        Acknowledgements threeAcks = new Acknowledgements(acks(3, CommandAcksTestEnv::newOkAck));
        assertEquals(3, threeAcks.count());
    }

    /*
     * Rejections
     ******************************************************************************/

    @Test
    @DisplayName("return true if contain any rejections")
    void containRejections() {
        List<Ack> items = ImmutableList.of(newRejectionAck(projectAlreadyStarted()));

        Acknowledgements acks = new Acknowledgements(items);
        assertTrue(acks.containRejections());

        Acknowledgements emptyAcks = new Acknowledgements(emptyList());
        assertFalse(emptyAcks.containRejections());
    }

    @Test
    @DisplayName("return proper total rejection count")
    void countRejection() {
        Acknowledgements noAcks = new Acknowledgements(newArrayList());
        assertEquals(0, noAcks.countRejections());

        Acknowledgements ack = new Acknowledgements(acks(1, CommandAcksTestEnv::newRejectionAck));
        assertEquals(1, ack.countRejections());

        Acknowledgements fiveAcksTwoRejections = new Acknowledgements(concat(
                acks(3, CommandAcksTestEnv::newOkAck),
                acks(2, CommandAcksTestEnv::newRejectionAck)));
        assertEquals(2, fiveAcksTwoRejections.countRejections());

        Acknowledgements sixAcksThreeRejections = new Acknowledgements(concat(
                acks(3, CommandAcksTestEnv::newRejectionAck),
                acks(3, CommandAcksTestEnv::newOkAck)));
        assertEquals(3, sixAcksThreeRejections.countRejections());
    }

    @Test
    @DisplayName("return proper total count for rejection class")
    void countRejectionClass() {
        Acknowledgements acks = new Acknowledgements(asList(
                newRejectionAck(projectAlreadyStarted()),
                newRejectionAck(taskLimitReached()),
                newRejectionAck(taskLimitReached())
        ));

        RejectionClass taskInCompletedProject =
                RejectionClass.of(IntTaskCreatedInCompletedProject.class);
        assertEquals(0, acks.countRejections(taskInCompletedProject));

        RejectionClass projectAlreadyStarted = RejectionClass.of(IntProjectAlreadyStarted.class);
        assertEquals(1, acks.countRejections(projectAlreadyStarted));

        RejectionClass taskLimitReached = RejectionClass.of(IntTaskLimitReached.class);
        assertEquals(2, acks.countRejections(taskLimitReached));
    }

    @Test
    @DisplayName("return true if contain the provided rejection class")
    void containRejectionClass() {
        List<Ack> items = asList(
                newRejectionAck(projectAlreadyStarted()),
                newRejectionAck(taskLimitReached()),
                newRejectionAck(taskLimitReached())
        );
        Acknowledgements acks = new Acknowledgements(items);

        RejectionClass completedProject = RejectionClass.of(IntTaskCreatedInCompletedProject.class);
        assertFalse(acks.containRejections(completedProject));

        RejectionClass projectAlreadyStarted = RejectionClass.of(IntProjectAlreadyStarted.class);
        assertTrue(acks.containRejections(projectAlreadyStarted));

        RejectionClass taskLimitReached = RejectionClass.of(IntTaskLimitReached.class);
        assertTrue(acks.containRejections(taskLimitReached));
    }

    @Test
    @DisplayName("return true if contain a rejection specified by predicate")
    void containRejectionUsingPredicate() {
        ImmutableList<Ack> items = ImmutableList.of(
                newRejectionAck(taskCreatedInCompletedProject(newTask(PRESENT_TASK_TITLE)))
        );
        Acknowledgements acks = new Acknowledgements(items);
        Class<IntTaskCreatedInCompletedProject> taskInCompletedProject =
                IntTaskCreatedInCompletedProject.class;

        RejectionCriterion<IntTaskCreatedInCompletedProject> withPresentTitle =
                rejection -> PRESENT_TASK_TITLE.equals(rejection.getTask().getTitle());
        assertTrue(acks.containRejection(taskInCompletedProject, withPresentTitle));

        RejectionCriterion<IntTaskCreatedInCompletedProject> withMissingTitle =
                rejection -> MISSING_TASK_TITLE.equals(rejection.getTask().getTitle());
        assertFalse(acks.containRejection(taskInCompletedProject, withMissingTitle));
    }

    @Test
    @DisplayName("return proper count if contain a rejection specified by predicate")
    void countRejectionUsingPredicate() {
        ImmutableList<Ack> items = ImmutableList.of(
                newRejectionAck(taskLimitReached()),
                newRejectionAck(taskCreatedInCompletedProject(newTask(UNIQUE_TASK_TITLEE))), 
                newRejectionAck(taskCreatedInCompletedProject(newTask(DUPLICATE_TASK_TITLE))),
                newRejectionAck(taskCreatedInCompletedProject(newTask(DUPLICATE_TASK_TITLE)))
        );
        Acknowledgements acks = new Acknowledgements(items);

        Class<IntTaskCreatedInCompletedProject> taskInCompletedProject =
                IntTaskCreatedInCompletedProject.class;

        RejectionCriterion<IntTaskCreatedInCompletedProject> withMissingTitle =
                rejection -> MISSING_TASK_TITLE.equals(rejection.getTask().getTitle());
        assertEquals(0, acks.countRejections(taskInCompletedProject, withMissingTitle));

        RejectionCriterion<IntTaskCreatedInCompletedProject> withUniqueTitle =
                rejection -> UNIQUE_TASK_TITLEE.equals(rejection.getTask().getTitle());
        assertEquals(1, acks.countRejections(taskInCompletedProject, withUniqueTitle));

        RejectionCriterion<IntTaskCreatedInCompletedProject> withDuplicatedTitle =
                rejection -> DUPLICATE_TASK_TITLE.equals(rejection.getTask().getTitle());
        assertEquals(2, acks.countRejections(taskInCompletedProject, withDuplicatedTitle));
    }

    /*
     * Errors
     ******************************************************************************/

    @Test
    @DisplayName("return true if contain any errors")
    void containErrors() {
        List<Ack> items = ImmutableList.of(newErrorAck());

        Acknowledgements acks = new Acknowledgements(items);
        assertTrue(acks.containErrors());

        Acknowledgements emptyAcks = new Acknowledgements(emptyList());
        assertFalse(emptyAcks.containErrors());
    }

    @Test
    @DisplayName("return proper total error count")
    void countErrors() {
        Acknowledgements noAcks = new Acknowledgements(newArrayList());
        assertEquals(0, noAcks.countErrors());

        Acknowledgements ack = new Acknowledgements(acks(1, CommandAcksTestEnv::newErrorAck));
        assertEquals(1, ack.countErrors());

        Acknowledgements fiveAcksTwoRejections = new Acknowledgements(concat(
                acks(3, CommandAcksTestEnv::newOkAck),
                acks(2, CommandAcksTestEnv::newErrorAck)));
        assertEquals(2, fiveAcksTwoRejections.countErrors());

        Acknowledgements sixAcksThreeRejections = new Acknowledgements(concat(
                acks(3, CommandAcksTestEnv::newErrorAck),
                acks(3, CommandAcksTestEnv::newOkAck)));
        assertEquals(3, sixAcksThreeRejections.countErrors());
    }

    @Test
    @DisplayName("return true if contain errors matched by criterion")
    void containErrorsUsingQualifier() {
        List<Ack> items = ImmutableList.of(newErrorAck(newError(PRESENT_ERROR_TYPE)));
        Acknowledgements acks = new Acknowledgements(items);

        assertTrue(acks.containErrors(withType(PRESENT_ERROR_TYPE)));
        assertFalse(acks.containErrors(withType(MISSING_ERROR_TYPE)));
    }

    @Test
    @DisplayName("return proper total error count matched by criterion")
    void countErrorsUsingQualifier() {
        Acknowledgements acks = new Acknowledgements(asList(
                newErrorAck(newError(UNIQUE_ERROR_TYPE)),
                newErrorAck(newError(DUPLICATE_ERROR_TYPE)),
                newErrorAck(newError(DUPLICATE_ERROR_TYPE))
        ));
        
        assertEquals(0, acks.countErrors(withType(MISSING_ERROR_TYPE)));
        assertEquals(1, acks.countErrors(withType(UNIQUE_ERROR_TYPE)));
        assertEquals(2, acks.countErrors(withType(DUPLICATE_ERROR_TYPE)));
    }
}
