/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.examples.importado;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.examples.importado.commands.ImportEvents;
import org.spine3.examples.importado.events.TaskCreated;
import org.spine3.examples.importado.events.TaskDone;
import org.spine3.examples.importado.events.WorkStarted;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;

import java.util.List;

/** A sample aggregate that can import events passed in the special command. */
public class TaskAggregate extends Aggregate<TaskId, Task, Task.Builder> {

    public TaskAggregate(TaskId id) {
        super(id);
    }

    @Assign
    public List<Event> handle(ImportEvents command, CommandContext ctx) {
        return command.getEventList();
    }

    @Apply
    private void on(TaskCreated event) {
        getBuilder().mergeFrom(event.getTask());
        print(event);
    }

    @Apply
    private void on(WorkStarted event) {
        getBuilder().setStatus(Task.Status.IN_PROGRESS);
        print(event);
    }

    @Apply
    private void on(TaskDone event) {
        getBuilder().setStatus(Task.Status.DONE);
        print(event);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr") // OK for this example.
    private void print(Message event) {
        final String aggregateClass = getClass().getSimpleName();
        final String eventClass = event.getClass().getSimpleName();
        final String eventData = TextFormat.shortDebugString(event);
        System.out.printf("%s applied %s: %s%n", aggregateClass, eventClass, eventData);
    }
}
