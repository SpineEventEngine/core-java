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
import io.spine.server.tuple.EitherOf3;
import io.spine.server.tuple.Pair;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.CmdAssignTask;
import io.spine.test.command.CmdStartTask;
import io.spine.test.command.CsAssignTask;
import io.spine.test.command.CsCreateTask;
import io.spine.test.command.CsPauseTask;
import io.spine.test.command.CsRemoveTaskFromProject;
import io.spine.test.command.CsStartTask;
import io.spine.test.command.CsStopTask;
import io.spine.test.commandservice.command.CsAddTask;

import java.util.Optional;

/**
 * A test environment for {@link io.spine.server.command.model.CommandSubstituteSignatureTest
 * CommandSubstituteSignatureTest}.
 */
public class CommandSubstituteTestEnv {

    /**
     * A standalone commander which declares valid {@link io.spine.server.command.Command
     * Command} substitution methods.
     *
     * <p>This class declares the duplicate handlers for some commands, hence it cannot be
     * registered in any Bounded Context. This is done for simplicity of enumerating all possible
     * combinations of parameters.
     */
    public static final class ValidCommander extends AbstractCommander {

        @Command
        CsStartTask singleMsgSingleResult(CsAssignTask command) {
            return CsStartTask.getDefaultInstance();
        }

        @Command
        CsStartTask msgWithCtxSingleResult(CsAssignTask command, CommandContext ctx) {
            return CsStartTask.getDefaultInstance();
        }

        @Command
        Pair<CmdAssignTask, CmdStartTask> singleMsgPairResult(CmdAddTask command) {
            return Pair.of(CmdAssignTask.getDefaultInstance(), CmdStartTask.getDefaultInstance());
        }

        @Command
        Pair<CmdAssignTask, CmdStartTask>
        msgWithCtxPairResult(CmdAddTask command, CommandContext ctx) {
            return Pair.of(CmdAssignTask.getDefaultInstance(), CmdStartTask.getDefaultInstance());
        }

        @Command
        Pair<CsAddTask, Optional<CsStartTask>> singleMsgPairWithOptional(CsCreateTask command) {
            CsAddTask addTask = CsAddTask.getDefaultInstance();
            return Pair.withNullable(addTask, null);
        }

        @Command
        Pair<CsAddTask, Optional<CsStartTask>>
        msgWithCtxPairWithOptional(CsCreateTask command, CommandContext ctx) {
            CsAddTask addTask = CsAddTask.getDefaultInstance();
            return Pair.withNullable(addTask, null);
        }

        @Command
        EitherOf3<CsStopTask, CsPauseTask, Nothing>
        singleMsgEitherOf3(CsRemoveTaskFromProject cmd) {
            return EitherOf3.withC(nothing());
        }

        @Command
        EitherOf3<CsStopTask, CsPauseTask, Nothing>
        msgWithCtxEitherOf3(CsRemoveTaskFromProject cmd, CommandContext ctx) {
            return EitherOf3.withC(nothing());
        }

        @Command
        Iterable<CommandMessage> singleMsgIterableResult(CsAssignTask command) {
            return ImmutableList.of(CsStartTask.getDefaultInstance());
        }

        @Command
        Iterable<CommandMessage>
        msgWithCtxIterableResult(CsAssignTask command, CommandContext ctx) {
            return ImmutableList.of(CsStartTask.getDefaultInstance());
        }

        @SuppressWarnings("MethodMayBeStatic")              // testing the visibility level.
        @Command
        private CsStartTask privateHandler(CsAssignTask command) {
            return CsStartTask.getDefaultInstance();
        }

        @SuppressWarnings("ProtectedMemberInFinalClass")    // testing the visibility level.
        @Command
        protected CsStartTask protectedHandler(CsAssignTask command) {
            return CsStartTask.getDefaultInstance();
        }

        @Command
        public CsStartTask publicHandler(CsAssignTask command) {
            return CsStartTask.getDefaultInstance();
        }
    }

    /**
     * A standalone commander which declares invalid {@link Command
     * Command} substitution methods.
     *
     * <p>Being similar to {@link ValidCommander}, this class also declares duplicate handlers
     * for the same commands. Again, this seems to be the simplest way to test invalid signatures
     * by enumerating different invalid options.
     */
    public static final class InvalidCommander extends AbstractCommander {

        @Command
        CsStartTask noParams() {
            return CsStartTask.getDefaultInstance();
        }

        @Command
        CsStartTask nonCommandMessageParam(Nothing command) {
            return CsStartTask.getDefaultInstance();
        }

        @Command
        CsStartTask nonMessageParam(int command) {
            return CsStartTask.getDefaultInstance();
        }

        @Command
        Optional<CsStartTask> optionalResult(CsAssignTask command) {
            return Optional.empty();
        }

        @Command
        CsStartTask threeParams(CsAssignTask command, CommandContext ctx, CsAssignTask third) {
            return CsStartTask.getDefaultInstance();
        }

        @Command
        CsStartTask wrongSecondParam(CsAssignTask command, Nothing message) {
            return CsStartTask.getDefaultInstance();
        }

        @Command
        CsStartTask wrongContext(CsAssignTask command, MessageContext msg) {
            return CsStartTask.getDefaultInstance();
        }

        @Command
        void voidMethod(CsAssignTask command) {
            // do nothing.
        }

        @Command
        Nothing eventResult(CsAssignTask command) {
            return nothing();
        }

        @Command
        int nonMessageResult(CsAssignTask command) {
            return 42;
        }

        @Command
        CsStartTask justInterface(SubstitutionTestCommand command) {
            return CsStartTask.getDefaultInstance();
        }
    }
}
