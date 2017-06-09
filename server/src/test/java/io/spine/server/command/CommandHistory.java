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

package io.spine.server.command;

import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import io.spine.base.Command;
import io.spine.base.CommandContext;
import io.spine.base.Commands;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Test utility for keeping the history of command messages and their contexts
 * handled by an aggregate.
 *
 * @author Alexander Yevsyukov
 */
public class CommandHistory {

    private final List<Message> messages = Lists.newArrayList();
    private final List<CommandContext> contexts = Lists.newArrayList();

    public void add(Message message, CommandContext context) {
        messages.add(message);
        contexts.add(context);
    }

    public boolean contains(Command command) {
        final Message message = Commands.getMessage(command);

        if (messages.contains(message)) {
            final int messageIndex = messages.indexOf(message);
            final CommandContext actualContext = command.getContext();
            final CommandContext storedContext = contexts.get(messageIndex);
            return actualContext.equals(storedContext);
        }

        return false;
    }

    public void clear() {
        messages.clear();
        contexts.clear();
    }

    public void assertHandled(Command expected) {
        final String cmdName = Commands.getMessage(expected)
                                       .getClass()
                                       .getName();
        assertTrue("Expected but wasn't handled, command: " + cmdName, contains(expected));
    }
}
