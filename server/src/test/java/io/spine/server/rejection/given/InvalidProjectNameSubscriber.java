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

package io.spine.server.rejection.given;

import io.spine.client.CommandFactory;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.Rejection;
import io.spine.core.Rejections;
import io.spine.core.Subscribe;
import io.spine.server.rejection.RejectionSubscriber;
import io.spine.test.rejection.ProjectRejections.InvalidProjectName;
import io.spine.test.rejection.command.RjUpdateProjectName;
import io.spine.testing.client.TestActorRequestFactory;

/**
 * @author Alexander Yevsyukov
 */
public class InvalidProjectNameSubscriber extends RejectionSubscriber {

    private Rejection rejectionHandled;

    @Subscribe
    public void on(InvalidProjectName rejection,
                   RjUpdateProjectName commandMessage,
                   CommandContext context) {
        CommandFactory commandFactory =
                TestActorRequestFactory.newInstance(InvalidProjectNameSubscriber.class)
                                       .command();
        Command command = commandFactory.createWithContext(commandMessage, context);
        this.rejectionHandled = Rejections.createRejection(rejection, command);
    }

    public Rejection getRejectionHandled() {
        return rejectionHandled;
    }
}
