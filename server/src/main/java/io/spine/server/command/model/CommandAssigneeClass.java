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

package io.spine.server.command.model;

import io.spine.server.command.AbstractCommandAssignee;
import io.spine.server.type.EventClass;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides message-handling information on a {@code CommandAssignee} class.
 *
 * @param <C>
 *         the type of command assignees
 */
public final class CommandAssigneeClass<C extends AbstractCommandAssignee>
        extends AbstractCommandHandlingClass<C, EventClass, AssigneeReceptor> {

    private static final long serialVersionUID = 0L;

    private CommandAssigneeClass(Class<C> cls) {
        super(cls, new CommandAssigneeSignature());
    }

    /**
     * Obtains command assignee class for the passed raw class.
     */
    public static <C extends AbstractCommandAssignee>
    CommandAssigneeClass<C> asCommandAssigneeClass(Class<C> cls) {
        checkNotNull(cls);
        @SuppressWarnings("unchecked")
        var result = (CommandAssigneeClass<C>)
                get(cls, CommandAssigneeClass.class, () -> new CommandAssigneeClass<>(cls));
        return result;
    }
}
