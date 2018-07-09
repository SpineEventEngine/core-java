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
import com.google.common.collect.Streams;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.Ack;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.Any.pack;
import static io.spine.base.Identifier.newUuid;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mykhailo Drachuk
 */
@DisplayName("Command Acknowledgements should")
class CommandAcksTest {

    private static final String SPINE_TYPE_PREFIX = "type.spine.io";

    @Test
    @DisplayName("return proper total acknowledgements count")
    void count() {
        CommandAcks noAcks = new CommandAcks(newArrayList());
        assertEquals(0, noAcks.count());

        CommandAcks ack = new CommandAcks(acks(1, CommandAcksTest::newOkAck));
        assertEquals(1, ack.count());

        CommandAcks twoAcks = new CommandAcks(acks(2, CommandAcksTest::newOkAck));
        assertEquals(2, twoAcks.count());

        CommandAcks threeAcks = new CommandAcks(acks(3, CommandAcksTest::newOkAck));
        assertEquals(3, threeAcks.count());
    }

    private static Ack newOkAck() {
        return Ack.newBuilder()
                  .setMessageId(newMessageId())
                  .setStatus(newOkStatus())
                  .build();
    }

    private static Ack newRejectionAck(Message rejection) {
        return Ack.newBuilder()
                  .setMessageId(newMessageId())
                  .setStatus(newRejectedStatus(rejection))
                  .build();
    }

    private static Ack newRejectionAck() {
        return newRejectionAck(taskLimitReached());
    }

    private static Any newMessageId() {
        return pack(StringValue.of(newUuid()), SPINE_TYPE_PREFIX);
    }

    private static Status newOkStatus() {
        return Status.newBuilder()
                     .setOk(Empty.getDefaultInstance())
                     .build();
    }

    private static Status newRejectedStatus(Message domainRejection) {
        Rejection rejection =
                Rejection.newBuilder()
                         .setMessage(pack(domainRejection, SPINE_TYPE_PREFIX))
                         .build();
        return Status.newBuilder()
                     .setRejection(rejection)
                     .build();
    }

    private static List<Ack> acks(int count, Supplier<Ack> messageSupplier) {
        List<Ack> events = newArrayList();
        for (int i = 0; i < count; i++) {
            events.add(messageSupplier.get());
        }
        return events;
    }

    private static ProjectId newProjectId() {
        return ProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    private static Rejections.IntProjectAlreadyStarted projectAlreadyStarted() {
        return Rejections.IntProjectAlreadyStarted.newBuilder()
                                                  .setProjectId(newProjectId())
                                                  .build();
    }

    private static Rejections.IntTaskLimitReached taskLimitReached() {
        return Rejections.IntTaskLimitReached.newBuilder()
                                             .setProjectId(newProjectId())
                                             .build();

    }

    private static Task newTask(String title) {
        return Task.newBuilder()
                   .setTaskId(newTaskId())
                   .setTitle(title)
                   .build();
    }

    private static TaskId newTaskId() {
        return TaskId.newBuilder()
                     .setId(newUuid())
                     .build();
    }

    private static Rejections.IntTaskCreatedInCompletedProject
    taskCreatedInCompletedProject(Task task) {
        return Rejections.IntTaskCreatedInCompletedProject
                .newBuilder()
                .setProjectId(newProjectId())
                .setTask(task)
                .build();

    }

    @Test
    @DisplayName("return true if contain any rejections")
    void containRejections() {
        List<Ack> items = ImmutableList.of(newRejectionAck(projectAlreadyStarted()));

        CommandAcks acks = new CommandAcks(items);
        assertTrue(acks.containRejections());

        CommandAcks emptyAcks = new CommandAcks(emptyList());
        assertFalse(emptyAcks.containRejections());
    }

    @Test
    @DisplayName("return proper total rejection count")
    void countRejection() {
        CommandAcks noAcks = new CommandAcks(newArrayList());
        assertEquals(0, noAcks.countRejections());

        CommandAcks ack = new CommandAcks(acks(1, CommandAcksTest::newRejectionAck));
        assertEquals(1, ack.countRejections());

        CommandAcks fiveAcksTwoRejections = new CommandAcks(concat(
                acks(3, CommandAcksTest::newOkAck),
                acks(2, CommandAcksTest::newRejectionAck)));
        assertEquals(2, fiveAcksTwoRejections.countRejections());
        
        CommandAcks sixAcksThreeRejections = new CommandAcks(concat(
                acks(3, CommandAcksTest::newRejectionAck), 
                acks(3, CommandAcksTest::newOkAck)));
        assertEquals(3, sixAcksThreeRejections.countRejections());
    }

    private static List<Ack> concat(Collection<Ack> itemsA, Collection<Ack> itemsB) {
        return Streams.concat(itemsA.stream(), itemsB.stream())
                      .collect(toList());
    }

    @Test
    @DisplayName("return proper total count for rejection class")
    void countRejectionClass() {
        List<Ack> items = asList(
                newRejectionAck(projectAlreadyStarted()),
                newRejectionAck(taskLimitReached()),
                newRejectionAck(taskLimitReached())
        );
        CommandAcks acks = new CommandAcks(items);

        RejectionClass taskInCompletedProject =
                RejectionClass.of(Rejections.IntTaskCreatedInCompletedProject.class);
        assertEquals(0, acks.countRejections(taskInCompletedProject));

        RejectionClass projectAlreadyStarted =
                RejectionClass.of(Rejections.IntProjectAlreadyStarted.class);
        assertEquals(1, acks.countRejections(projectAlreadyStarted));

        RejectionClass taskLimitReached =
                RejectionClass.of(Rejections.IntTaskLimitReached.class);
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
        CommandAcks acks = new CommandAcks(items);

        RejectionClass completedProject =
                RejectionClass.of(Rejections.IntTaskCreatedInCompletedProject.class);
        assertFalse(acks.containRejections(completedProject));

        RejectionClass projectAlreadyStarted =
                RejectionClass.of(Rejections.IntProjectAlreadyStarted.class);
        assertTrue(acks.containRejections(projectAlreadyStarted));

        RejectionClass taskLimitReached =
                RejectionClass.of(Rejections.IntTaskLimitReached.class);
        assertTrue(acks.containRejections(taskLimitReached));
    }

    @Test
    @DisplayName("return true if contain a rejection specified by predicate")
    void containRejectionUsingPredicate() {
        String presentTitle = "present-title";
        String missingTitle = "missing-title";
        ImmutableList<Ack> items = ImmutableList.of(
                newRejectionAck(taskCreatedInCompletedProject(newTask(presentTitle)))
        );
        CommandAcks acks = new CommandAcks(items);
        Class<Rejections.IntTaskCreatedInCompletedProject> taskInCompletedProjectClass =
                Rejections.IntTaskCreatedInCompletedProject.class;

        RejectionPredicate<Rejections.IntTaskCreatedInCompletedProject> withPresentTitle =
                rejection -> presentTitle.equals(rejection.getTask().getTitle());
        assertTrue(acks.containRejection(taskInCompletedProjectClass, withPresentTitle));

        RejectionPredicate<Rejections.IntTaskCreatedInCompletedProject> withMissingTitle =
                rejection -> missingTitle.equals(rejection.getTask().getTitle());
        assertFalse(acks.containRejection(taskInCompletedProjectClass, withMissingTitle));
    }

    @Test
    @DisplayName("return proper count if contain a rejection specified by predicate")
    void countRejectionUsingPredicate() {
        String uniqueTitle = "single-title";
        String duplicatedTitle = "duplicate-title";
        String missingTitle = "missing-title";
        ImmutableList<Ack> items = ImmutableList.of(
                newRejectionAck(taskLimitReached()),
                newRejectionAck(taskCreatedInCompletedProject(newTask(uniqueTitle))),
                newRejectionAck(taskCreatedInCompletedProject(newTask(duplicatedTitle))),
                newRejectionAck(taskCreatedInCompletedProject(newTask(duplicatedTitle)))
        );
        CommandAcks acks = new CommandAcks(items);

        Class<Rejections.IntTaskCreatedInCompletedProject> taskInCompletedProject =
                Rejections.IntTaskCreatedInCompletedProject.class;

        RejectionPredicate<Rejections.IntTaskCreatedInCompletedProject> withMissingTitle =
                rejection -> missingTitle.equals(rejection.getTask().getTitle());
        assertEquals(0, acks.countRejections(taskInCompletedProject, withMissingTitle));

        RejectionPredicate<Rejections.IntTaskCreatedInCompletedProject> withUniqueTitle =
                rejection -> uniqueTitle.equals(rejection.getTask().getTitle());
        assertEquals(1, acks.countRejections(taskInCompletedProject, withUniqueTitle));

        RejectionPredicate<Rejections.IntTaskCreatedInCompletedProject> withDuplicatedTitle =
                rejection -> duplicatedTitle.equals(rejection.getTask().getTitle());
        assertEquals(2, acks.countRejections(taskInCompletedProject, withDuplicatedTitle));
    }
}
