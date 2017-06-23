/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.string;

import com.google.protobuf.Message;
import io.spine.json.Json;

/**
 * The default {@code Stringifier} for {@code Message} classes.
 *
 * <p>The sample of the usage:
 * <pre>  {@code
 * // The `TaskId` Protobuf message definition.
 * message TaskId {
 *     string value = 1;
 * }}</pre>
 *
 * <pre>  {@code
 * // The `TaskName` Protobuf message definition.
 * message TaskName {
 *     string value = 1;
 * }}</pre>
 *
 * <pre>  {@code
 * // The `Task` Protobuf message definition.
 * message Task {
 *     TaskId id = 1;
 *     TaskName name = 2;
 * }}</pre>
 *
 * <pre>  {@code
 * // Construct the message.
 * final TaskId taskId = TaskId.newBuilder().setValue("task-id").build();
 * final TaskName taskName = TaskName.newBuilder().setValue("task-name").build();
 * final Task task = Task.newBuilder().setId(taskId).setTaskName(taskName).build();
 *
 * // Obtain the default Stringifier.
 * final Stringifier<Task> taskStringifier = StringifierRegistry.getStringifier(Task.class);
 *
 * // The result is {"id":{"value":"task-id"},"name":{"value":"task-name"}}.
 * final String json = taskStringifier.reverse().convert(task);
 *
 * // task.equals(taskFromJson) == true.
 * final Task taskFromJson = taskStringifier.convert(json);
 * }</pre>
 *
 * @param <T> the message type
 *
 * @author Illia Shepilov
 */
final class DefaultMessageStringifier<T extends Message> extends Stringifier<T> {

    private final Class<T> messageClass;

    DefaultMessageStringifier(Class<T> messageType) {
        super();
        this.messageClass = messageType;
    }

    @Override
    protected String toString(T obj) {
        return Json.toCompactJson(obj);
    }

    @Override
    protected T fromString(String s) {
        return Json.fromJson(s, messageClass);
    }
}
