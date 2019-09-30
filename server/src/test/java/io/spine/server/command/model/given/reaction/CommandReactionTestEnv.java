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
import io.spine.model.contexts.projects.command.SigAddTaskToProject;
import io.spine.model.contexts.projects.command.SigCreateProject;
import io.spine.model.contexts.projects.command.SigPauseTask;
import io.spine.model.contexts.projects.command.SigStartTask;
import io.spine.model.contexts.projects.command.SigStopTask;
import io.spine.model.contexts.projects.event.SigTaskAddedToProject;
import io.spine.model.contexts.projects.event.SigTaskStarted;
import io.spine.model.contexts.projects.rejection.ProjectRejections;
import io.spine.model.contexts.projects.rejection.SigCannotCreateProject;
import io.spine.server.command.AbstractCommander;
import io.spine.server.command.Command;
import io.spine.server.event.React;
import io.spine.server.model.DoNothing;
import io.spine.server.model.Nothing;
import io.spine.server.model.given.SignatureTestEvent;
import io.spine.server.tuple.EitherOf3;
import io.spine.server.tuple.Pair;

import java.io.IOException;
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
        SigStartTask singleMsgSingleResult(SigTaskAddedToProject event) {
            return startTask();
        }

        @Command
        Optional<SigStartTask> singleMsgOptionalResult(SigTaskAddedToProject event) {
            return Optional.empty();
        }

        @Command
        SigStartTask msgWithCtxSingleResult(SigTaskAddedToProject event, EventContext ctx) {
            return startTask();
        }

        @Command
        SigCreateProject justRejection(ProjectRejections.SigCannotCreateProject rejection) {
            return createProject();
        }

        @Command
        SigCreateProject rejectionWithCtx(ProjectRejections.SigCannotCreateProject rejection,
                                          CommandContext context) {
            return createProject();
        }

        @Command
        Pair<SigAddTaskToProject, SigStartTask> singleMsgPairResult(SigTaskAddedToProject event) {
            return Pair.of(addTaskToProject(), startTask());
        }

        @Command
        Pair<SigAddTaskToProject, Optional<SigStartTask>>
        pairWithOptionalResult(SigTaskAddedToProject event) {
            return Pair.withNullable(addTaskToProject(), null);
        }

        @Command
        EitherOf3<SigPauseTask, SigStopTask, DoNothing>
        eitherOf3Result(SigTaskAddedToProject event) {
            return EitherOf3.withC(doNothing());
        }

        @Command
        Iterable<CommandMessage> iterableResult(SigTaskAddedToProject event) {
            return ImmutableList.of(startTask());
        }

        @Command
        private SigStartTask privateHandler(SigTaskAddedToProject event) {
            return startTask();
        }

        @Command
        private SigStartTask protectedHandler(SigTaskAddedToProject event) {
            return startTask();
        }

        @Command
        public SigStartTask publicHandler(SigTaskAddedToProject event) {
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
        SigStartTask noParams() {
            return startTask();
        }

        @Command
        SigStartTask nonEventMessageParam(UserId user) {
            return startTask();
        }

        @Command
        SigStartTask nonMessageParam(int event) {
            return startTask();
        }

        @Command
        SigStartTask threeParams(SigTaskAddedToProject event, EventContext ctx, Nothing third) {
            return startTask();
        }

        @Command
        SigStartTask wrongSecondParam(SigTaskAddedToProject event, Nothing message) {
            return startTask();
        }

        @Command
        SigStartTask wrongContext(SigTaskAddedToProject event, MessageContext msg) {
            return startTask();
        }

        @Command
        void voidMethod(SigTaskAddedToProject event) {
            // do nothing.
        }

        @Command
        SigTaskStarted eventResult(SigTaskAddedToProject event) {
            return taskStarted();
        }

        @Command
        int nonMessageResult(SigTaskAddedToProject event) {
            return 18;
        }

        @Command
        SigStartTask justInterface(SignatureTestEvent event) {
            return startTask();
        }

        @Command
        SigStartTask interfaceAndContext(SignatureTestEvent event, EventContext context) {
            return startTask();
        }

        @Command
        Iterable<UserId> wrongIterable(SigTaskAddedToProject event) {
            return ImmutableList.of();
        }

        @React
        SigTaskStarted declaredThrowable(SigTaskAddedToProject event) throws IOException {
            throw new IOException("An invalid commander method has thrown an exception");
        }

        @React
        SigTaskStarted declaredRejection(SigTaskAddedToProject e) throws SigCannotCreateProject {
            throw cannotCreateProject();
        }
    }

    private static SigCannotCreateProject cannotCreateProject() {
        return SigCannotCreateProject.newBuilder()
                                     .build();
    }

    private static SigTaskStarted taskStarted() {
        return SigTaskStarted.getDefaultInstance();
    }

    private static SigCreateProject createProject() {
        return SigCreateProject.getDefaultInstance();
    }

    private static SigAddTaskToProject addTaskToProject() {
        return SigAddTaskToProject.getDefaultInstance();
    }

    private static SigStartTask startTask() {
        return SigStartTask.getDefaultInstance();
    }
}
