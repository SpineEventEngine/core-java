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
import io.spine.model.contexts.projects.command.SigAddTaskToProject;
import io.spine.model.contexts.projects.command.SigAssignTask;
import io.spine.model.contexts.projects.command.SigCreateProject;
import io.spine.model.contexts.projects.command.SigCreateTask;
import io.spine.model.contexts.projects.command.SigPauseTask;
import io.spine.model.contexts.projects.command.SigRemoveTaskFromProject;
import io.spine.model.contexts.projects.command.SigSetProjectOwner;
import io.spine.model.contexts.projects.command.SigStartTask;
import io.spine.model.contexts.projects.command.SigStopTask;
import io.spine.model.contexts.projects.rejection.SigCannotCreateProject;
import io.spine.server.command.AbstractCommander;
import io.spine.server.command.Command;
import io.spine.server.model.DoNothing;
import io.spine.server.model.Nothing;
import io.spine.server.model.given.SignatureTestCommand;
import io.spine.server.tuple.EitherOf2;
import io.spine.server.tuple.EitherOf3;
import io.spine.server.tuple.Pair;

import java.util.Optional;

import static io.spine.util.Exceptions.newIllegalStateException;

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
        SigStartTask singleMsgSingleResult(SigAssignTask command) {
            return startTask();
        }

        @Command
        SigSetProjectOwner declaredRejection(SigCreateProject command)
                throws SigCannotCreateProject {
            throw SigCannotCreateProject.newBuilder()
                                        .build();
        }

        @Command
        SigStartTask msgWithCtxSingleResult(SigAssignTask command, CommandContext ctx) {
            return startTask();
        }

        @Command
        Pair<SigAssignTask, SigStartTask> singleMsgPairResult(SigAddTaskToProject command) {
            return Pair.of(assignTask(), startTask());
        }

        @Command
        Pair<SigAssignTask, SigStartTask>
        msgWithCtxPairResult(SigAddTaskToProject command, CommandContext ctx) {
            return Pair.of(assignTask(), startTask());
        }

        @Command
        Pair<SigAddTaskToProject, Optional<SigStartTask>>
        pairWithOptionalResult(SigCreateTask command) {
            return Pair.withNullable(addTask(), null);
        }

        @Command
        Pair<SigAddTaskToProject, Optional<SigStartTask>>
        msgWithCtxPairWithOptional(SigCreateTask command, CommandContext ctx) {
            return Pair.withNullable(addTask(), null);
        }

        @Command
        EitherOf2<SigStopTask, SigPauseTask> singleMsgEitherOf2(SigRemoveTaskFromProject cmd) {
            return EitherOf2.withB(pauseTask());
        }

        @Command
        EitherOf2<SigStopTask, SigPauseTask>
        msgWithCtxEitherOf2(SigRemoveTaskFromProject cmd, CommandContext ctx) {
            return EitherOf2.withB(pauseTask());
        }

        @Command
        Iterable<CommandMessage> singleMsgIterableResult(SigAssignTask command) {
            return ImmutableList.of(startTask());
        }

        @Command
        Iterable<CommandMessage>
        msgWithCtxIterableResult(SigAssignTask command, CommandContext ctx) {
            return ImmutableList.of(startTask());
        }

        @SuppressWarnings("MethodMayBeStatic")              // testing the visibility level.
        @Command
        private SigStartTask privateHandler(SigAssignTask command) {
            return startTask();
        }

        @SuppressWarnings("ProtectedMemberInFinalClass")    // testing the visibility level.
        @Command
        protected SigStartTask protectedHandler(SigAssignTask command) {
            return startTask();
        }

        @Command
        public SigStartTask publicHandler(SigAssignTask command) {
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
        SigStartTask noParams() {
            return startTask();
        }

        @Command
        SigStartTask nonCommandMessageParam(Nothing command) {
            return startTask();
        }

        @Command
        SigStartTask nonMessageParam(int command) {
            return startTask();
        }

        @Command
        Optional<SigStartTask> optionalResult(SigAssignTask command) {
            return Optional.empty();
        }

        @Command
        SigStartTask threeParams(SigAssignTask command, CommandContext ctx, SigAssignTask third) {
            return startTask();
        }

        @Command
        SigStartTask wrongSecondParam(SigAssignTask command, Nothing message) {
            return startTask();
        }

        @Command
        SigStartTask wrongContext(SigAssignTask command, MessageContext msg) {
            return startTask();
        }

        @Command
        void voidMethod(SigAssignTask command) {
            // do nothing.
        }

        @Command
        Nothing eventResult(SigAssignTask command) {
            return nothing();
        }

        @Command
        int nonMessageResult(SigAssignTask command) {
            return 42;
        }

        @Command
        SigStartTask justInterface(SignatureTestCommand command) {
            return startTask();
        }

        @Command
        SigStartTask interfaceAndContext(SignatureTestCommand command, CommandContext context) {
            return startTask();
        }

        @Command
        EitherOf3<SigAssignTask, SigStartTask, DoNothing>
        eitherWithNothing(SigAddTaskToProject command) {
            return EitherOf3.withC(doNothing());
        }

        @Command
        Iterable<Nothing> wrongIterable(SigAddTaskToProject command) {
            return ImmutableList.of();
        }

        @Command
        SigSetProjectOwner wrongThrowable(SigCreateProject command) throws RuntimeException {
            throw newIllegalStateException("Command substitution method has thrown " +
                                                   "an illegal exception.");
        }
    }

    private static SigAssignTask assignTask() {
        return SigAssignTask.getDefaultInstance();
    }

    private static SigAddTaskToProject addTask() {
        return SigAddTaskToProject.getDefaultInstance();
    }

    private static SigStartTask startTask() {
        return SigStartTask.getDefaultInstance();
    }

    private static SigPauseTask pauseTask() {
        return SigPauseTask.getDefaultInstance();
    }
}
