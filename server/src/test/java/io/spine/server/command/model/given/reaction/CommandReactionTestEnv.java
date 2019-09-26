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

package io.spine.server.command.model.given.reaction;

import com.google.common.collect.ImmutableList;
import io.spine.base.CommandMessage;
import io.spine.base.MessageContext;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.core.UserId;
import io.spine.server.command.AbstractCommander;
import io.spine.server.command.Command;
import io.spine.server.model.DoNothing;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf3;
import io.spine.server.tuple.Pair;
import io.spine.test.command.CdrAddTaskToProject;
import io.spine.test.command.CdrCreateProject;
import io.spine.test.command.CdrPauseTask;
import io.spine.test.command.CdrStartTask;
import io.spine.test.command.CdrStopTask;
import io.spine.test.command.event.CdrTaskAddedToProject;
import io.spine.test.command.event.CdrTaskStarted;
import io.spine.test.command.rejection.CommandRejections;

import java.util.Optional;

/**
 * A test environment for {@link io.spine.server.command.model.CommandReactionSignatureTest
 * CommandReactionSignatureTest}.
 */
@SuppressWarnings("MethodOnlyUsedFromInnerClass")
public final class CommandReactionTestEnv {

    /** Prevents instantiation of this test environment utility. */
    private CommandReactionTestEnv() {
    }

    /**
     * A standalone commander which declares valid {@link Command} reaction methods.
     *
     * <p>This class declares the duplicate handlers for some events, hence it cannot be
     * registered in any Bounded Context. This is done for simplicity of enumerating all possible
     * combinations of parameters.
     */
    @SuppressWarnings("MethodMayBeStatic")
    public static final class ValidCommander extends AbstractCommander {

        @Command
        CdrStartTask singleMsgSingleResult(CdrTaskAddedToProject event) {
            return startTask();
        }

        @Command
        Optional<CdrStartTask> singleMsgOptionalResult(CdrTaskAddedToProject event) {
            return Optional.empty();
        }

        @Command
        CdrStartTask msgWithCtxSingleResult(CdrTaskAddedToProject event, EventContext ctx) {
            return startTask();
        }

        @Command
        CdrCreateProject justRejection(CommandRejections.CdrCannotCreateProject rejection) {
            return createProject();
        }

        @Command
        CdrCreateProject rejectionWithCtx(CommandRejections.CdrCannotCreateProject rejection,
                                          CommandContext context) {
            return createProject();
        }

        @Command
        Pair<CdrAddTaskToProject, CdrStartTask> singleMsgPairResult(CdrTaskAddedToProject event) {
            return Pair.of(addTaskToProject(), startTask());
        }

        @Command
        Pair<CdrAddTaskToProject, Optional<CdrStartTask>>
        pairWithOptionalResult(CdrTaskAddedToProject event) {
            return Pair.withNullable(addTaskToProject(), null);
        }

        @Command
        EitherOf3<CdrPauseTask, CdrStopTask, DoNothing>
        eitherOf3Result(CdrTaskAddedToProject event) {
            return EitherOf3.withC(doNothing());
        }

        @Command
        Iterable<CommandMessage> iterableResult(CdrTaskAddedToProject event) {
            return ImmutableList.of(startTask());
        }

        @Command
        private CdrStartTask privateHandler(CdrTaskAddedToProject event) {
            return startTask();
        }

        @Command
        private CdrStartTask protectedHandler(CdrTaskAddedToProject event) {
            return startTask();
        }

        @Command
        public CdrStartTask publicHandler(CdrTaskAddedToProject event) {
            return startTask();
        }
    }

    /**
     * A standalone commander which declares invalid {@link Command} reaction methods.
     *
     * <p>Being similar to {@link ValidCommander}, this class also declares duplicate handlers
     * for the same events. Again, this seems to be the simplest way to test invalid signatures
     * by enumerating different invalid options.
     */
    public static final class InvalidCommander extends AbstractCommander {

        @Command
        CdrStartTask noParams() {
            return startTask();
        }

        @Command
        CdrStartTask nonEventMessageParam(UserId user) {
            return startTask();
        }

        @Command
        CdrStartTask nonMessageParam(int event) {
            return startTask();
        }

        @Command
        CdrStartTask threeParams(CdrTaskAddedToProject event, EventContext ctx, Nothing third) {
            return startTask();
        }

        @Command
        CdrStartTask wrongSecondParam(CdrTaskAddedToProject event, Nothing message) {
            return startTask();
        }

        @Command
        CdrStartTask wrongContext(CdrTaskAddedToProject event, MessageContext msg) {
            return startTask();
        }

        @Command
        void voidMethod(CdrTaskAddedToProject event) {
            // do nothing.
        }

        @Command
        CdrTaskStarted eventResult(CdrTaskAddedToProject event) {
            return taskStarted();
        }

        @Command
        int nonMessageResult(CdrTaskAddedToProject event) {
            return 18;
        }

        @Command
        CdrStartTask justInterface(CommanderTestEvent event) {
            return startTask();
        }

        @Command
        CdrStartTask interfaceAndContext(CommanderTestEvent event, EventContext context) {
            return startTask();
        }

        @Command
        Iterable<UserId> wrongIterable(CdrTaskAddedToProject event) {
            return ImmutableList.of();
        }
    }

    private static CdrTaskStarted taskStarted() {
        return CdrTaskStarted.getDefaultInstance();
    }

    private static CdrCreateProject createProject() {
        return CdrCreateProject.getDefaultInstance();
    }

    private static CdrAddTaskToProject addTaskToProject() {
        return CdrAddTaskToProject.getDefaultInstance();
    }

    private static CdrStartTask startTask() {
        return CdrStartTask.getDefaultInstance();
    }
}
