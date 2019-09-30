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
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.Assign;
import io.spine.server.command.model.given.commander.CommanderTestCommand;
import io.spine.server.command.model.given.reaction.CommanderTestEvent;
import io.spine.server.model.DoNothing;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.server.tuple.EitherOf3;
import io.spine.server.tuple.Pair;
import io.spine.test.command.CdrAssignTask;
import io.spine.test.command.CdrCreateProject;
import io.spine.test.command.CdrPauseTask;
import io.spine.test.command.CdrRemoveTaskFromProject;
import io.spine.test.command.CdrStartTask;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.event.CdrProjectCreated;
import io.spine.test.command.event.CdrProjectStarted;
import io.spine.test.command.event.CdrTaskAssigned;
import io.spine.test.command.event.CdrTaskPaused;
import io.spine.test.command.event.CdrTaskStarted;
import io.spine.test.command.event.CdrTaskStopped;
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
        CdrProjectCreated singleMsgSingleResult(CdrCreateProject command) {
            return projectCreated();
        }

        @Assign
        CommanderTestEvent interfaceResult(CdrCreateProject command) {
            return projectCreated();
        }

        @Assign
        CdrProjectCreated msgWithCtxSingleResult(RefCreateProject command, CommandContext ctx) {
            return projectCreated();
        }

        @Assign
        Pair<CdrProjectCreated, CdrProjectStarted> singleMsgPairResult(RefCreateProject command) {
            return Pair.of(projectCreated(), projectStarted());
        }

        @Assign
        Pair<CdrProjectCreated, CdrProjectStarted>
        msgWithCtxPairResult(RefCreateProject command, CommandContext ctx) {
            return Pair.of(projectCreated(), projectStarted());
        }

        @Assign
        Pair<CdrProjectCreated, Optional<CdrProjectStarted>>
        pairWithOptionalResult(RefCreateProject command) {
            return Pair.withNullable(projectCreated(), null);
        }

        @Assign
        Pair<CdrProjectCreated, Optional<CdrProjectStarted>>
        msgWithCtxPairWithOptional(RefCreateProject command, CommandContext ctx) {
            return Pair.withNullable(projectCreated(), null);
        }

        @Assign
        EitherOf2<CdrTaskStopped, CdrTaskPaused> singleMsgEitherOf2(CdrRemoveTaskFromProject cmd) {
            return EitherOf2.withB(taskPaused());
        }

        @Assign
        EitherOf2<CdrTaskStopped, CdrTaskPaused>
        msgWithCtxEitherOf2(CdrRemoveTaskFromProject cmd, CommandContext ctx) {
            return EitherOf2.withB(taskPaused());
        }

        @Assign
        Iterable<EventMessage> singleMsgIterableResult(CdrStartTask command) {
            return ImmutableList.of(taskStarted());
        }

        @Assign
        Iterable<EventMessage>
        msgWithCtxIterableResult(CdrStartTask command, CommandContext ctx) {
            return ImmutableList.of(taskStarted());
        }

        @SuppressWarnings("MethodMayBeStatic")              // testing the visibility level.
        @Assign
        private CdrTaskPaused privateHandler(CdrPauseTask command) {
            return taskPaused();
        }

        @SuppressWarnings("ProtectedMemberInFinalClass")    // testing the visibility level.
        @Assign
        protected CdrTaskPaused protectedHandler(CdrPauseTask command) {
            return taskPaused();
        }

        @Assign
        public CdrTaskPaused publicHandler(CdrPauseTask command) {
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
        CdrTaskStarted noParams() {
            return taskStarted();
        }

        @Assign
        CdrTaskStarted nonCommandMessageParam(Nothing command) {
            return taskStarted();
        }

        @Assign
        CdrTaskStarted nonMessageParam(int command) {
            return taskStarted();
        }

        @Assign
        Optional<CdrTaskStarted> optionalResult(CdrAssignTask command) {
            return Optional.empty();
        }

        @Assign
        CdrTaskStarted threeParams(CdrAssignTask command, CommandContext ctx, CdrAssignTask third) {
            return taskStarted();
        }

        @Assign
        CdrTaskStarted wrongFirstParam(Nothing command, MessageContext msg) {
            return taskStarted();
        }

        @Assign
        CdrTaskStarted wrongSecondParam(CdrAssignTask command, Nothing message) {
            return taskStarted();
        }

        @Assign
        CdrTaskStarted wrongContext(CdrAssignTask command, MessageContext msg) {
            return taskStarted();
        }

        @Assign
        void voidMethod(CdrAssignTask command) {
            // do nothing.
        }

        @Assign
        DoNothing commandResult(CdrAssignTask command) {
            return DoNothing.getDefaultInstance();
        }

        @Assign
        int nonMessageResult(CdrAssignTask command) {
            return 42;
        }

        @Assign
        CdrProjectStarted justInterface(CommanderTestCommand command) {
            return projectStarted();
        }

        @Assign
        CdrTaskStarted interfaceAndContext(CommanderTestCommand command, CommandContext context) {
            return taskStarted();
        }

        @Assign
        EitherOf3<CdrTaskAssigned, CdrTaskStarted, Nothing> eitherWithNothing(CmdAddTask command) {
            return EitherOf3.withC(nothing());
        }

        @Assign
        Iterable<DoNothing> wrongIterable(CmdAddTask command) {
            return ImmutableList.of();
        }
    }

    private static CdrTaskStarted taskStarted() {
        return CdrTaskStarted.getDefaultInstance();
    }

    private static CdrTaskPaused taskPaused() {
        return CdrTaskPaused.getDefaultInstance();
    }

    private static CdrProjectStarted projectStarted() {
        return CdrProjectStarted.getDefaultInstance();
    }

    private static CdrProjectCreated projectCreated() {
        return CdrProjectCreated.getDefaultInstance();
    }
}
