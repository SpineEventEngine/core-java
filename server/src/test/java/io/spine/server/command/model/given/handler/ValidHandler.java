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
import io.spine.base.EventMessage;
import io.spine.core.CommandContext;
import io.spine.model.contexts.projects.command.SigCreateProject;
import io.spine.model.contexts.projects.command.SigPauseTask;
import io.spine.model.contexts.projects.command.SigRemoveTaskFromProject;
import io.spine.model.contexts.projects.command.SigStartTask;
import io.spine.model.contexts.projects.event.SigProjectCreated;
import io.spine.model.contexts.projects.event.SigProjectStarted;
import io.spine.model.contexts.projects.event.SigTaskPaused;
import io.spine.model.contexts.projects.event.SigTaskStopped;
import io.spine.model.contexts.projects.rejection.SigCannotCreateProject;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.Assign;
import io.spine.server.command.Command;
import io.spine.server.model.given.SignatureTestEvent;
import io.spine.server.tuple.EitherOf2;
import io.spine.server.tuple.Pair;
import io.spine.test.reflect.command.RefCreateProject;

import java.util.Optional;

/**
 * A standalone commander which declares valid {@link Assign command handlers}.
 *
 * <p>This class declares the duplicate handlers for some commands, hence it cannot be
 * registered in any Bounded Context. This is done for simplicity of enumerating all possible
 * combinations of parameters.
 */
public final class ValidHandler extends AbstractCommandHandler {

    @Assign
    SigProjectCreated singleMsgSingleResult(SigCreateProject command) {
        return EventMessages.projectCreated();
    }

    @Command
    SigProjectCreated declaredRejection(SigCreateProject cmd) throws SigCannotCreateProject {
        throw SigCannotCreateProject.newBuilder()
                                    .build();
    }

    @Assign
    SignatureTestEvent interfaceResult(SigCreateProject command) {
        return EventMessages.projectCreated();
    }

    @Assign
    SigProjectCreated msgWithCtxSingleResult(RefCreateProject command, CommandContext ctx) {
        return EventMessages.projectCreated();
    }

    @Assign
    Pair<SigProjectCreated, SigProjectStarted> singleMsgPairResult(RefCreateProject command) {
        return Pair.of(EventMessages.projectCreated(), EventMessages.projectStarted());
    }

    @Assign
    Pair<SigProjectCreated, SigProjectStarted>
    msgWithCtxPairResult(RefCreateProject command, CommandContext ctx) {
        return Pair.of(EventMessages.projectCreated(), EventMessages.projectStarted());
    }

    @Assign
    Pair<SigProjectCreated, Optional<SigProjectStarted>>
    pairWithOptionalResult(RefCreateProject command) {
        return Pair.withNullable(EventMessages.projectCreated(), null);
    }

    @Assign
    Pair<SigProjectCreated, Optional<SigProjectStarted>>
    msgWithCtxPairWithOptional(RefCreateProject command, CommandContext ctx) {
        return Pair.withNullable(EventMessages.projectCreated(), null);
    }

    @Assign
    EitherOf2<SigTaskStopped, SigTaskPaused> singleMsgEitherOf2(SigRemoveTaskFromProject cmd) {
        return EitherOf2.withB(EventMessages.taskPaused());
    }

    @Assign
    EitherOf2<SigTaskStopped, SigTaskPaused>
    msgWithCtxEitherOf2(SigRemoveTaskFromProject cmd, CommandContext ctx) {
        return EitherOf2.withB(EventMessages.taskPaused());
    }

    @Assign
    Iterable<EventMessage> singleMsgIterableResult(SigStartTask command) {
        return ImmutableList.of(EventMessages.taskStarted());
    }

    @Assign
    Iterable<EventMessage>
    msgWithCtxIterableResult(SigStartTask command, CommandContext ctx) {
        return ImmutableList.of(EventMessages.taskStarted());
    }

    @SuppressWarnings("MethodMayBeStatic")              // testing the visibility level.
    @Assign
    private SigTaskPaused privateHandler(SigPauseTask command) {
        return EventMessages.taskPaused();
    }

    @SuppressWarnings("ProtectedMemberInFinalClass")    // testing the visibility level.
    @Assign
    protected SigTaskPaused protectedHandler(SigPauseTask command) {
        return EventMessages.taskPaused();
    }

    @Assign
    public SigTaskPaused publicHandler(SigPauseTask command) {
        return EventMessages.taskPaused();
    }
}
