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
import io.spine.base.MessageContext;
import io.spine.core.CommandContext;
import io.spine.model.contexts.projects.command.SigAddTaskToProject;
import io.spine.model.contexts.projects.command.SigAssignTask;
import io.spine.model.contexts.projects.command.SigCreateProject;
import io.spine.model.contexts.projects.command.SigPauseTask;
import io.spine.model.contexts.projects.command.SigRemoveTaskFromProject;
import io.spine.model.contexts.projects.command.SigStartTask;
import io.spine.model.contexts.projects.event.SigProjectCreated;
import io.spine.model.contexts.projects.event.SigProjectStarted;
import io.spine.model.contexts.projects.event.SigTaskAssigned;
import io.spine.model.contexts.projects.event.SigTaskPaused;
import io.spine.model.contexts.projects.event.SigTaskStarted;
import io.spine.model.contexts.projects.event.SigTaskStopped;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.Assign;
import io.spine.server.model.DoNothing;
import io.spine.server.model.Nothing;
import io.spine.server.model.given.SignatureTestCommand;
import io.spine.server.model.given.SignatureTestEvent;
import io.spine.server.tuple.EitherOf2;
import io.spine.server.tuple.EitherOf3;
import io.spine.server.tuple.Pair;
import io.spine.test.reflect.command.RefCreateProject;

import java.util.Optional;

/**
 * A test environment for {@link io.spine.server.command.CommandHandlerSignatureTest
 * CommandHandlerSignatureTest}.
 */
@SuppressWarnings("MethodOnlyUsedFromInnerClass")
public final class CommandHandlerSignatureTestEnv {

    /** Prevents this test environment from direct initialization. */
    private CommandHandlerSignatureTestEnv() {
    }

    /**
     * A standalone commander which declares valid {@link Assign command handlers}.
     *
     * <p>This class declares the duplicate handlers for some commands, hence it cannot be
     * registered in any Bounded Context. This is done for simplicity of enumerating all possible
     * combinations of parameters.
     */
    public static final class ValidHandler extends AbstractCommandHandler {

        @Assign
        SigProjectCreated singleMsgSingleResult(SigCreateProject command) {
            return projectCreated();
        }

        @Assign
        SignatureTestEvent interfaceResult(SigCreateProject command) {
            return projectCreated();
        }

        @Assign
        SigProjectCreated msgWithCtxSingleResult(RefCreateProject command, CommandContext ctx) {
            return projectCreated();
        }

        @Assign
        Pair<SigProjectCreated, SigProjectStarted> singleMsgPairResult(RefCreateProject command) {
            return Pair.of(projectCreated(), projectStarted());
        }

        @Assign
        Pair<SigProjectCreated, SigProjectStarted>
        msgWithCtxPairResult(RefCreateProject command, CommandContext ctx) {
            return Pair.of(projectCreated(), projectStarted());
        }

        @Assign
        Pair<SigProjectCreated, Optional<SigProjectStarted>>
        pairWithOptionalResult(RefCreateProject command) {
            return Pair.withNullable(projectCreated(), null);
        }

        @Assign
        Pair<SigProjectCreated, Optional<SigProjectStarted>>
        msgWithCtxPairWithOptional(RefCreateProject command, CommandContext ctx) {
            return Pair.withNullable(projectCreated(), null);
        }

        @Assign
        EitherOf2<SigTaskStopped, SigTaskPaused> singleMsgEitherOf2(SigRemoveTaskFromProject cmd) {
            return EitherOf2.withB(taskPaused());
        }

        @Assign
        EitherOf2<SigTaskStopped, SigTaskPaused>
        msgWithCtxEitherOf2(SigRemoveTaskFromProject cmd, CommandContext ctx) {
            return EitherOf2.withB(taskPaused());
        }

        @Assign
        Iterable<EventMessage> singleMsgIterableResult(SigStartTask command) {
            return ImmutableList.of(taskStarted());
        }

        @Assign
        Iterable<EventMessage>
        msgWithCtxIterableResult(SigStartTask command, CommandContext ctx) {
            return ImmutableList.of(taskStarted());
        }

        @SuppressWarnings("MethodMayBeStatic")              // testing the visibility level.
        @Assign
        private SigTaskPaused privateHandler(SigPauseTask command) {
            return taskPaused();
        }

        @SuppressWarnings("ProtectedMemberInFinalClass")    // testing the visibility level.
        @Assign
        protected SigTaskPaused protectedHandler(SigPauseTask command) {
            return taskPaused();
        }

        @Assign
        public SigTaskPaused publicHandler(SigPauseTask command) {
            return taskPaused();
        }
    }

    /**
     * A standalone commander which declares invalid {@link Assign command handlers}.
     *
     * <p>Being similar to {@link ValidHandler}, this class also declares duplicate handlers
     * for the same commands. This is the way to avoid myriads of small classes which enumerate
     * all possible combinations or params.
     */
    public static final class InvalidHandler extends AbstractCommandHandler {

        @Assign
        SigTaskStarted noParams() {
            return taskStarted();
        }

        @Assign
        SigTaskStarted nonCommandMessageParam(Nothing command) {
            return taskStarted();
        }

        @Assign
        SigTaskStarted nonMessageParam(int command) {
            return taskStarted();
        }

        @Assign
        Optional<SigTaskStarted> optionalResult(SigAssignTask command) {
            return Optional.empty();
        }

        @Assign
        SigTaskStarted threeParams(SigAssignTask command, CommandContext ctx, SigAssignTask third) {
            return taskStarted();
        }

        @Assign
        SigTaskStarted wrongFirstParam(Nothing command, MessageContext msg) {
            return taskStarted();
        }

        @Assign
        SigTaskStarted wrongSecondParam(SigAssignTask command, Nothing message) {
            return taskStarted();
        }

        @Assign
        SigTaskStarted wrongContext(SigAssignTask command, MessageContext msg) {
            return taskStarted();
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
            return projectStarted();
        }

        @Assign
        SigTaskStarted interfaceAndContext(SignatureTestCommand command, CommandContext context) {
            return taskStarted();
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
    }

    private static SigTaskStarted taskStarted() {
        return SigTaskStarted.getDefaultInstance();
    }

    private static SigTaskPaused taskPaused() {
        return SigTaskPaused.getDefaultInstance();
    }

    private static SigProjectStarted projectStarted() {
        return SigProjectStarted.getDefaultInstance();
    }

    private static SigProjectCreated projectCreated() {
        return SigProjectCreated.getDefaultInstance();
    }
}
