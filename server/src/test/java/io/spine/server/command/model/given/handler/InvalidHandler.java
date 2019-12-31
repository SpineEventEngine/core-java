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

package io.spine.server.command.model.given.handler;

import com.google.common.collect.ImmutableList;
import io.spine.base.MessageContext;
import io.spine.core.CommandContext;
import io.spine.model.contexts.projects.command.SigAddTaskToProject;
import io.spine.model.contexts.projects.command.SigAssignTask;
import io.spine.model.contexts.projects.command.SigCreateProject;
import io.spine.model.contexts.projects.command.SigSetProjectOwner;
import io.spine.model.contexts.projects.event.SigProjectStarted;
import io.spine.model.contexts.projects.event.SigTaskAssigned;
import io.spine.model.contexts.projects.event.SigTaskStarted;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.Assign;
import io.spine.server.command.Command;
import io.spine.server.model.DoNothing;
import io.spine.server.model.Nothing;
import io.spine.server.model.given.SignatureTestCommand;
import io.spine.server.tuple.EitherOf3;

import java.util.Optional;

import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A standalone commander which declares invalid {@link Assign command handlers}.
 *
 * <p>Being similar to {@link ValidHandler}, this class also declares duplicate handlers
 * for the same commands. This is the way to avoid myriads of small classes which enumerate
 * all possible combinations or params.
 */
public final class InvalidHandler extends AbstractCommandHandler {

    @Assign
    SigTaskStarted noParams() {
        return EventMessages.taskStarted();
    }

    @Assign
    SigTaskStarted nonCommandMessageParam(Nothing command) {
        return EventMessages.taskStarted();
    }

    @Assign
    SigTaskStarted nonMessageParam(int command) {
        return EventMessages.taskStarted();
    }

    @Assign
    Optional<SigTaskStarted> optionalResult(SigAssignTask command) {
        return Optional.empty();
    }

    @Assign
    SigTaskStarted threeParams(SigAssignTask command, CommandContext ctx, SigAssignTask third) {
        return EventMessages.taskStarted();
    }

    @Assign
    SigTaskStarted wrongFirstParam(Nothing command, MessageContext msg) {
        return EventMessages.taskStarted();
    }

    @Assign
    SigTaskStarted wrongSecondParam(SigAssignTask command, Nothing message) {
        return EventMessages.taskStarted();
    }

    @Assign
    SigTaskStarted wrongContext(SigAssignTask command, MessageContext msg) {
        return EventMessages.taskStarted();
    }

    @Assign
    void voidMethod(SigAssignTask command) {
        // do nothing.
    }

    @Assign
    DoNothing commandResult(SigAssignTask command) {
        return DoNothing.getDefaultInstance();
    }

    @Assign
    int nonMessageResult(SigAssignTask command) {
        return 42;
    }

    @Assign
    SigProjectStarted justInterface(SignatureTestCommand command) {
        return EventMessages.projectStarted();
    }

    @Assign
    SigTaskStarted interfaceAndContext(SignatureTestCommand command, CommandContext context) {
        return EventMessages.taskStarted();
    }

    @Assign
    EitherOf3<SigTaskAssigned, SigTaskStarted, Nothing>
    eitherWithNothing(SigAddTaskToProject command) {
        return EitherOf3.withC(nothing());
    }

    @Assign
    Iterable<DoNothing> wrongIterable(SigAddTaskToProject command) {
        return ImmutableList.of();
    }

    @Command
    SigSetProjectOwner wrongThrowable(SigCreateProject command) throws RuntimeException {
        throw newIllegalStateException("Command handler method " +
                                               "has declared an illegal exception.");
    }
}
