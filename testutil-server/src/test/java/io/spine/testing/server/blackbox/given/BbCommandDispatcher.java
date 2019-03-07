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

package io.spine.testing.server.blackbox.given;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.server.command.AbstractCommandDispatcher;
import io.spine.server.event.EventBus;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;

import java.util.Set;

/**
 * Increments a counter on receiving a command of the specified type.
 *
 * @see io.spine.testing.server.blackbox.BlackBoxBoundedContextTest#registerCommandDispatchers
 */
public class BbCommandDispatcher extends AbstractCommandDispatcher {

    private int commandsReceived = 0;
    private final CommandClass commandToIntercept;

    public BbCommandDispatcher(EventBus eventBus, CommandClass commandToIntercept) {
        super(eventBus);
        this.commandToIntercept = commandToIntercept;
    }

    @Override
    public Set<CommandClass> messageClasses() {
        return ImmutableSet.of(commandToIntercept);
    }

    @CanIgnoreReturnValue
    @Override
    public String dispatch(CommandEnvelope envelope) {
        commandsReceived++;
        return getId();
    }

    public int commandsIntercepted() {
        return commandsReceived;
    }
}
