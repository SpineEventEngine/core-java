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
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;

/**
 * A command dispatcher that dispatches the same type of commands as
 * {@link io.spine.testing.server.blackbox.given.BbCommandDispatcher}.
 *
 * <p>Attempting to register them with the same bounded context should result in an exception.
 */
public final class BbDuplicateCommandDispatcher extends AbstractCommandDispatcher {

    private final CommandClass commandToIntercept;

    public BbDuplicateCommandDispatcher(CommandClass commandToIntercept) {
        super();
        this.commandToIntercept = commandToIntercept;
    }

    @Override
    public ImmutableSet<CommandClass> messageClasses() {
        return ImmutableSet.of(commandToIntercept);
    }

    @CanIgnoreReturnValue
    @Override
    public void dispatch(CommandEnvelope envelope) {
    }
}
