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

package io.spine.system.server.given.entity;

import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.system.server.CreatePersonName;
import io.spine.system.server.PersonFirstName;
import io.spine.system.server.PersonFirstNameVBuilder;
import io.spine.system.server.PersonId;
import io.spine.system.server.PersonNameCreated;
import io.spine.system.server.PersonRenamed;
import io.spine.type.TypeUrl;

/**
 * An aggregate part which handles a person first name.
 */
public class PersonNamePart
        extends AggregatePart<PersonId, PersonFirstName, PersonFirstNameVBuilder, PersonRoot> {

    public static final TypeUrl TYPE = TypeUrl.of(PersonFirstName.class);

    private PersonNamePart(PersonRoot root) {
        super(root);
    }

    @React
    Nothing reactOn(PersonRenamed event) {
        return nothing();
    }

    @Assign
    PersonNameCreated handle(CreatePersonName command) {
        return PersonNameCreated.newBuilder()
                                .setId(command.getId())
                                .setFirstName(command.getFirstName())
                                .build();
    }

    @Apply
    private void on(PersonNameCreated event) {
        builder().setId(event.getId())
                 .setFirstName(event.getFirstName());
    }
}
