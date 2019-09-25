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

package io.spine.server.command.model.given.commander;

import com.google.common.collect.ImmutableList;
import io.spine.base.CommandMessage;
import io.spine.base.MessageContext;
import io.spine.core.CommandContext;
import io.spine.server.command.AbstractCommander;
import io.spine.server.command.Command;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.server.tuple.EitherOf3;
import io.spine.server.tuple.Pair;
import io.spine.test.command.CdrAddTaskToProject;
import io.spine.test.command.CdrAssignTask;
import io.spine.test.command.CdrCreateTask;
import io.spine.test.command.CdrPauseTask;
import io.spine.test.command.CdrRemoveTaskFromProject;
import io.spine.test.command.CdrStartTask;
import io.spine.test.command.CdrStopTask;
import io.spine.test.command.CmdAddTask;

import java.util.Optional;

/**
 * A test environment for {@link io.spine.server.command.model.CommandSubstituteSignatureTest
 * CommandSubstituteSignatureTest}.
 */
@SuppressWarnings("MethodOnlyUsedFromInnerClass")
public final class CommandSubstituteTestEnv {

    /** Prevents instantiation of this test environment utility. */
    private CommandSubstituteTestEnv() {
    }

    /**
     * A standalone commander which declares valid {@link Command} substitution methods.
     *
     * <p>This class declares the duplicate handlers for some commands, hence it cannot be
     * registered in any Bounded Context. This is done for simplicity of enumerating all possible
     * combinations of parameters.
     */
    public static final class ValidCommander extends AbstractCommander {

        @Command
        CdrStartTask singleMsgSingleResult(CdrAssignTask command) {
            return startTask();
        }

        @Command
        CdrStartTask msgWithCtxSingleResult(CdrAssignTask command, CommandContext ctx) {
            return startTask();
        }

        @Command
        Pair<CdrAssignTask, CdrStartTask> singleMsgPairResult(CmdAddTask command) {
            return Pair.of(assignTask(), startTask());
        }

        @Command
        Pair<CdrAssignTask, CdrStartTask>
        msgWithCtxPairResult(CmdAddTask command, CommandContext ctx) {
            return Pair.of(assignTask(), startTask());
        }

        @Command
        Pair<CdrAddTaskToProject, Optional<CdrStartTask>>
        pairWithOptionalResult(CdrCreateTask command) {
            return Pair.withNullable(addTask(), null);
        }

        @Command
        Pair<CdrAddTaskToProject, Optional<CdrStartTask>>
        msgWithCtxPairWithOptional(CdrCreateTask command, CommandContext ctx) {
            return Pair.withNullable(addTask(), null);
        }

        @Command
        EitherOf2<CdrStopTask, CdrPauseTask> singleMsgEitherOf2(CdrRemoveTaskFromProject cmd) {
            return EitherOf2.withB(pauseTask());
        }

        @Command
        EitherOf2<CdrStopTask, CdrPauseTask>
        msgWithCtxEitherOf2(CdrRemoveTaskFromProject cmd, CommandContext ctx) {
            return EitherOf2.withB(pauseTask());
        }

        @Command
        Iterable<CommandMessage> singleMsgIterableResult(CdrAssignTask command) {
            return ImmutableList.of(startTask());
        }

        @Command
        Iterable<CommandMessage>
        msgWithCtxIterableResult(CdrAssignTask command, CommandContext ctx) {
            return ImmutableList.of(startTask());
        }

        @SuppressWarnings("MethodMayBeStatic")              // testing the visibility level.
        @Command
        private CdrStartTask privateHandler(CdrAssignTask command) {
            return startTask();
        }

        @SuppressWarnings("ProtectedMemberInFinalClass")    // testing the visibility level.
        @Command
        protected CdrStartTask protectedHandler(CdrAssignTask command) {
            return startTask();
        }

        @Command
        public CdrStartTask publicHandler(CdrAssignTask command) {
            return startTask();
        }
    }

    /**
     * A standalone commander which declares invalid {@link Command} substitution methods.
     *
     * <p>Being similar to {@link ValidCommander}, this class also declares duplicate handlers
     * for the same commands. Again, this seems to be the simplest way to test invalid signatures
     * by enumerating different invalid options.
     */
    public static final class InvalidCommander extends AbstractCommander {

        @Command
        CdrStartTask noParams() {
            return startTask();
        }

        @Command
        CdrStartTask nonCommandMessageParam(Nothing command) {
            return startTask();
        }

        @Command
        CdrStartTask nonMessageParam(int command) {
            return startTask();
        }

        @Command
        Optional<CdrStartTask> optionalResult(CdrAssignTask command) {
            return Optional.empty();
        }

        @Command
        CdrStartTask threeParams(CdrAssignTask command, CommandContext ctx, CdrAssignTask third) {
            return startTask();
        }

        @Command
        CdrStartTask wrongSecondParam(CdrAssignTask command, Nothing message) {
            return startTask();
        }

        @Command
        CdrStartTask wrongContext(CdrAssignTask command, MessageContext msg) {
            return startTask();
        }

        @Command
        void voidMethod(CdrAssignTask command) {
            // do nothing.
        }

        @Command
        Nothing eventResult(CdrAssignTask command) {
            return nothing();
        }

        @Command
        int nonMessageResult(CdrAssignTask command) {
            return 42;
        }

        @Command
        CdrStartTask justInterface(CommanderTestCommand command) {
            return startTask();
        }

        @Command
        CdrStartTask interfaceAndContext(CommanderTestCommand command, CommandContext context) {
            return startTask();
        }

        @Command
        EitherOf3<CdrAssignTask, CdrStartTask, Nothing> eitherWithNothing(CmdAddTask command) {
            return EitherOf3.withC(nothing());
        }

        @Command
        Iterable<Nothing> wrongIterable(CmdAddTask command) {
            return ImmutableList.of();
        }
    }

    private static CdrAssignTask assignTask() {
        return CdrAssignTask.getDefaultInstance();
    }

    private static CdrAddTaskToProject addTask() {
        return CdrAddTaskToProject.getDefaultInstance();
    }

    private static CdrStartTask startTask() {
        return CdrStartTask.getDefaultInstance();
    }

    private static CdrPauseTask pauseTask() {
        return CdrPauseTask.getDefaultInstance();
    }
}
