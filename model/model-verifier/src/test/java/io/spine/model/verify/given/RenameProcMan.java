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

package io.spine.model.verify.given;

import io.spine.core.EventContext;
import io.spine.core.Events;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.procman.ProcessManager;
import io.spine.test.model.verify.command.ChangeTitle;
import io.spine.test.model.verify.event.TitleChanged;
import io.spine.test.model.verify.given.RenameState;
import io.spine.test.model.verify.given.RenameStateVBuilder;

public class RenameProcMan extends ProcessManager<String, RenameState, RenameStateVBuilder> {

    protected RenameProcMan(String id) {
        super(id);
    }

    @Assign
    TitleChanged handle(ChangeTitle command) {
        return TitleChanged
                .newBuilder()
                .setNewTitle(command.getNewTitle())
                .build();
    }

    @Apply
    private void on(TitleChanged event, EventContext context) {
        getBuilder().setEditor(Events.getActor(context)
                                     .getValue());
    }
}
