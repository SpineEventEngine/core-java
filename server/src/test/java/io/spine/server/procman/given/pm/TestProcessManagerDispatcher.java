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

package io.spine.server.procman.given.pm;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmPlanIteration;
import io.spine.test.procman.command.PmReviewBacklog;
import io.spine.test.procman.command.PmScheduleRetrospective;
import io.spine.test.procman.command.PmStartIteration;

import java.util.Set;

/**
 * A simple NO-OP dispatcher for {@link TestProcessManager}.
 *
 * <p>Enables dispatch for all commands posted by the {@code TestProcessManager} commanding
 * methods.
 */
public class TestProcessManagerDispatcher implements CommandDispatcher<Message> {

    @Override
    public Set<CommandClass> messageClasses() {
        return CommandClass.setOf(PmAddTask.class,
                                  PmReviewBacklog.class,
                                  PmScheduleRetrospective.class,
                                  PmPlanIteration.class,
                                  PmStartIteration.class);
    }

    @Override
    public Message dispatch(CommandEnvelope envelope) {
        return Empty.getDefaultInstance();
    }

    @Override
    public void onError(CommandEnvelope envelope, RuntimeException exception) {
        // NoOp.
    }
}
