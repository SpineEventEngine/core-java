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

package io.spine.system.server.given.entity;

import io.spine.server.command.Assign;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.procman.ProcessManager;
import io.spine.system.server.CompletePersonCreation;
import io.spine.system.server.PersonCreation;
import io.spine.system.server.PersonCreationCompleted;
import io.spine.system.server.PersonCreationStarted;
import io.spine.system.server.PersonId;
import io.spine.system.server.PersonNameCreated;
import io.spine.system.server.StartPersonCreation;
import io.spine.type.TypeUrl;

/**
 * A process manager which handles person creation.
 */
public final class PersonProcman
        extends ProcessManager<PersonId, PersonCreation, PersonCreation.Builder> {

    public static final TypeUrl TYPE = TypeUrl.of(PersonCreation.class);

    PersonProcman(PersonId id) {
        super(id);
    }

    @Assign
    PersonCreationStarted handle(StartPersonCreation command) {
        builder().setId(command.getId());
        return PersonCreationStarted
                .newBuilder()
                .setId(command.getId())
                .build();
    }

    @Assign
    PersonCreationCompleted handle(CompletePersonCreation command) {
        builder().setId(command.getId())
                 .setCreated(true);
        return PersonCreationCompleted
                .newBuilder()
                .setId(command.getId())
                .build();
    }

    @React
    Nothing reactOn(PersonNameCreated event) {
        builder().setId(event.getId())
                 .setCreated(true);
        return nothing();
    }
}
