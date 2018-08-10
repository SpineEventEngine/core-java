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

package io.spine.server.procman.given.pm;

import com.google.common.collect.Lists;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.test.procman.command.PmAddTask;

import java.util.List;
import java.util.Set;

/**
 * Helper dispatcher class to verify that the Process Manager routes
 * the {@link PmAddTask} command.
 */
public class AddTaskDispatcher implements CommandDispatcher<Message> {

    private final List<CommandEnvelope> commands = Lists.newLinkedList();

    @Override
    public Set<CommandClass> getMessageClasses() {
        return CommandClass.setOf(PmAddTask.class);
    }

    @Override
    public Message dispatch(CommandEnvelope envelope) {
        commands.add(envelope);
        return Empty.getDefaultInstance();
    }

    @Override
    public void onError(CommandEnvelope envelope, RuntimeException exception) {
        // Do nothing.
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK for tests.
    public List<CommandEnvelope> getCommands() {
        return commands;
    }
}
