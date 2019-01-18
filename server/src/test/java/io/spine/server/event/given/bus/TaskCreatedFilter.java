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

package io.spine.server.event.given.bus;

import com.google.protobuf.Any;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.core.Ack;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.bus.BusFilter;
import io.spine.test.event.EBTaskAdded;
import io.spine.test.event.Task;

import java.util.Optional;

import static io.spine.server.bus.Buses.reject;
import static java.util.Optional.empty;

/**
 * Filters out the {@link EBTaskAdded} events which have their {@link Task#getDone()}
 * property set to {@code true}.
 */
public class TaskCreatedFilter implements BusFilter<EventEnvelope> {

    private static final EventClass TASK_ADDED_CLASS = EventClass.from(EBTaskAdded.class);

    @Override
    public Optional<Ack> accept(EventEnvelope envelope) {
        if (TASK_ADDED_CLASS.equals(envelope.getMessageClass())) {
            EBTaskAdded message = (EBTaskAdded) envelope.getMessage();
            Task task = message.getTask();
            if (task.getDone()) {
                Error error = error();
                Any packedId = Identifier.pack(envelope.getId());
                Ack result = reject(packedId, error);
                return Optional.of(result);
            }
        }
        return empty();
    }

    private static Error error() {
        return Error.newBuilder()
                    .setMessage("The task cannot be created in a 'completed' state.")
                    .build();
    }
}
