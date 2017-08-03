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

package io.spine.server.rejection.given;

import com.google.protobuf.Empty;
import io.spine.client.CommandFactory;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.React;
import io.spine.core.Rejection;
import io.spine.core.Rejections;
import io.spine.server.rejection.RejectionReactor;
import io.spine.test.rejection.ProjectRejections.InvalidProjectName;
import io.spine.test.rejection.command.UpdateProjectName;

public class InvalidProjectNameReactor extends RejectionReactor {

    private Rejection rejectionHandled;

    @React
    public Empty on(InvalidProjectName rejection,
                    UpdateProjectName commandMessage,
                    CommandContext context) {
        final CommandFactory commandFactory =
                TestActorRequestFactory.newInstance(InvalidProjectNameReactor.class)
                                       .command();
        final Command command = commandFactory.createWithContext(commandMessage, context);
        this.rejectionHandled = Rejections.createRejection(rejection, command);
        return Empty.getDefaultInstance();
    }

    public Rejection getRejectionHandled() {
        return rejectionHandled;
    }
}
