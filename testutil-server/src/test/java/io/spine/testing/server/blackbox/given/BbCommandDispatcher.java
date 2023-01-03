/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;

import static io.spine.server.dispatch.DispatchOutcomes.successfulOutcome;

/**
 * Increments a counter on receiving a command of the specified type.
 *
 * @see io.spine.testing.server.blackbox.BlackBoxBoundedContextTest#registerCommandDispatchers
 */
public final class BbCommandDispatcher extends AbstractCommandDispatcher {

    private int commandsReceived = 0;
    private final CommandClass commandToDispatch;

    public BbCommandDispatcher(CommandClass commandToDispatch) {
        super();
        this.commandToDispatch = commandToDispatch;
    }

    @Override
    public ImmutableSet<CommandClass> messageClasses() {
        return ImmutableSet.of(commandToDispatch);
    }

    @CanIgnoreReturnValue
    @Override
    public DispatchOutcome dispatch(CommandEnvelope envelope) {
        commandsReceived++;
        return successfulOutcome(envelope);
    }

    public int commandsDispatched() {
        return commandsReceived;
    }
}
