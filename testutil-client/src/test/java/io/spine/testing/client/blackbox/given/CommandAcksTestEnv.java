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

package io.spine.testing.client.blackbox.given;

import com.google.common.collect.Streams;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Event;
import io.spine.core.Status;
import io.spine.testing.client.blackbox.ProjectId;
import io.spine.testing.client.blackbox.Rejections.BbProjectAlreadyStarted;
import io.spine.testing.client.blackbox.Rejections.BbTaskCreatedInCompletedProject;
import io.spine.testing.client.blackbox.Rejections.BbTaskLimitReached;
import io.spine.testing.client.blackbox.Task;
import io.spine.testing.client.blackbox.TaskId;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Identifier.newUuid;
import static io.spine.protobuf.AnyPacker.pack;
import static java.util.stream.Collectors.toList;

public class CommandAcksTestEnv {

    public static final String MISSING_ERROR_TYPE = "missing-error";
    public static final String UNIQUE_ERROR_TYPE = "unique-error";
    public static final String DUPLICATE_ERROR_TYPE = "duplicate-error";
    public static final String PRESENT_ERROR_TYPE = "present-error";
    public static final String UNIQUE_TASK_TITLE = "single-title";
    public static final String DUPLICATE_TASK_TITLE = "duplicate-title";
    public static final String MISSING_TASK_TITLE = "missing-title";
    public static final String PRESENT_TASK_TITLE = "present-title";

    /** Prevents instantiation of this utility class. */
    private CommandAcksTestEnv() {
    }

    public static Ack newOkAck() {
        return Ack.newBuilder()
                  .setMessageId(newMessageId())
                  .setStatus(newOkStatus())
                  .build();
    }

    public static Ack newRejectionAck(Message rejection) {
        return Ack.newBuilder()
                  .setMessageId(newMessageId())
                  .setStatus(newRejectedStatus(rejection))
                  .build();
    }

    public static Ack newErrorAck() {
        return newErrorAck(newError());
    }

    public static Error newError() {
        return newError(newUuid());
    }

    public static Error newError(String type) {
        return Error.newBuilder()
                    .setType(type)
                    .setCode(0)
                    .build();
    }

    public static Ack newErrorAck(Error error) {
        return Ack.newBuilder()
                  .setMessageId(newMessageId())
                  .setStatus(newErrorStatus(error))
                  .build();
    }

    public static Ack newRejectionAck() {
        return newRejectionAck(taskLimitReached());
    }

    private static Any newMessageId() {
        return pack(StringValue.of(newUuid()));
    }

    private static Status newOkStatus() {
        return Status.newBuilder()
                     .setOk(Empty.getDefaultInstance())
                     .build();
    }

    private static Status newRejectedStatus(Message domainRejection) {
        Event rejectionEvent = Event
                .newBuilder()
                .setMessage(pack(domainRejection))
                .build();
        return Status.newBuilder()
                     .setRejection(rejectionEvent)
                     .build();
    }

    private static Status newErrorStatus(Error error) {
        return Status.newBuilder()
                     .setError(error)
                     .build();
    }

    public static List<Ack> acks(int count, Supplier<Ack> messageSupplier) {
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

    public static BbProjectAlreadyStarted projectAlreadyStarted() {
        return BbProjectAlreadyStarted.newBuilder()
                                      .setProjectId(newProjectId())
                                      .build();
    }

    public static BbTaskLimitReached taskLimitReached() {
        return BbTaskLimitReached.newBuilder()
                                 .setProjectId(newProjectId())
                                 .build();

    }

    public static Task newTask(String title) {
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

    public static BbTaskCreatedInCompletedProject taskCreatedInCompletedProject(Task task) {
        return BbTaskCreatedInCompletedProject
                .newBuilder()
                .setProjectId(newProjectId())
                .setTask(task)
                .build();

    }

    public static List<Ack> concat(Collection<Ack> itemsA, Collection<Ack> itemsB) {
        return Streams.concat(itemsA.stream(), itemsB.stream())
                      .collect(toList());
    }
}
