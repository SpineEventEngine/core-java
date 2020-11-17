/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.event.given;

import com.google.common.collect.ImmutableList;
import io.spine.base.EventMessage;
import io.spine.base.MessageContext;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.core.UserId;
import io.spine.model.contexts.projects.command.SigCreateProject;
import io.spine.model.contexts.projects.command.SigStartTask;
import io.spine.model.contexts.projects.event.SigProjectCreated;
import io.spine.model.contexts.projects.event.SigTaskAddedToProject;
import io.spine.model.contexts.projects.event.SigTaskPaused;
import io.spine.model.contexts.projects.event.SigTaskRemovedFromProject;
import io.spine.model.contexts.projects.event.SigTaskStarted;
import io.spine.model.contexts.projects.event.SigTaskStopped;
import io.spine.model.contexts.projects.rejection.ProjectRejections;
import io.spine.model.contexts.projects.rejection.SigCannotCreateProject;
import io.spine.server.event.AbstractEventReactor;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.model.given.SignatureTestEvent;
import io.spine.server.tuple.EitherOf3;
import io.spine.server.tuple.Pair;

import java.io.IOException;
import java.util.Optional;

/**
 * A test environment for {@link io.spine.server.event.model.EventReactorSignatureTest
 * EventReactorSignatureTest}.
 */
@SuppressWarnings(
        {"MethodOnlyUsedFromInnerClass", "Unused", "MethodMayBeStatic"})  // reflective access.
public class EventReactorSignatureTestEnv {

    /**
     * Prevents this test environment from direct initialization.
     */
    private EventReactorSignatureTestEnv() {
    }

    /**
     * A standalone event reactor which declares valid {@link React event-reacting handlers}.
     *
     * <p>This class declares the duplicate handlers for some events, hence it cannot be
     * registered in any Bounded Context. This is done for simplicity of enumerating all possible
     * combinations of parameters.
     */
    public static final class ValidReactor extends AbstractEventReactor {

        @React
        SigTaskStarted singleMsgSingleResult(SigTaskAddedToProject event) {
            return taskStarted();
        }

        @React
        Optional<SigTaskStarted> singleMsgOptionalResult(SigTaskAddedToProject event) {
            return Optional.empty();
        }

        @React
        SigTaskStarted msgWithCtxSingleResult(SigTaskAddedToProject event, EventContext ctx) {
            return taskStarted();
        }

        @React
        SigProjectCreated justRejection(ProjectRejections.SigCannotCreateProject rejection) {
            return projectCreated();
        }

        @React
        SigProjectCreated
        rejectionWithCtx(ProjectRejections.SigCannotCreateProject rejection, CommandContext ctx) {
            return projectCreated();
        }

        @React
        SigProjectCreated rejectionWithCommand(ProjectRejections.SigCannotCreateProject rejection,
                                               SigCreateProject command) {
            return projectCreated();
        }

        @React
        SigProjectCreated rejectionWithCommandAndCtx(ProjectRejections.SigCannotCreateProject r,
                                                     SigCreateProject cmd,
                                                     CommandContext ctx) {
            return projectCreated();
        }

        @React
        Pair<SigTaskAddedToProject, SigTaskStarted>
        singleMsgPairResult(SigTaskAddedToProject event) {
            return Pair.of(taskAddedToProject(), taskStarted());
        }

        @React
        Pair<SigTaskAddedToProject, Optional<SigTaskStarted>>
        pairWithOptionalResult(SigTaskAddedToProject event) {
            return Pair.withNullable(taskAddedToProject(), null);
        }

        @React
        EitherOf3<SigTaskPaused, SigTaskStopped, Nothing>
        eitherOf3Result(SigTaskRemovedFromProject event) {
            return EitherOf3.withC(nothing());
        }

        @React
        Iterable<EventMessage> iterableResult(SigTaskAddedToProject event) {
            return ImmutableList.of(taskStarted());
        }

        @React
        private SigTaskStarted privateHandler(SigTaskAddedToProject event) {
            return taskStarted();
        }

        @React
        // It is a use-case-under-test.
        @SuppressWarnings({"ProtectedMembersInFinalClass", "ProtectedMemberInFinalClass"})
        protected SigTaskStarted protectedHandler(SigTaskAddedToProject event) {
            return taskStarted();
        }

        @React
        public SigTaskStarted publicHandler(SigTaskAddedToProject event) {
            return taskStarted();
        }
    }

    /**
     * A standalone reactor which declares invalid {@link React event-reacting handlers}.
     *
     * <p>Being similar to {@link ValidReactor}, this class also declares duplicate handlers
     * for the same events. This is the way to avoid lots of small classes enumerating
     * all possible combinations or params.
     */
    public static final class InvalidReactor extends AbstractEventReactor {

        @React
        SigTaskStarted noParams() {
            return taskStarted();
        }

        @React
        SigTaskStarted nonEventMessageParam(UserId user) {
            return taskStarted();
        }

        @React
        SigTaskStarted nonMessageParam(int event) {
            return taskStarted();
        }

        @React
        SigTaskStarted wrongThreeParams(SigTaskAddedToProject e, EventContext ctx, Nothing third) {
            return taskStarted();
        }

        @React
        SigProjectCreated rejectionAndThreeMoreParams(ProjectRejections.SigCannotCreateProject r,
                                                      SigCreateProject cmd,
                                                      CommandContext ctx,
                                                      UserId user) {
            return projectCreated();
        }

        @React
        SigTaskStarted wrongSecondParam(SigTaskAddedToProject event, Nothing message) {
            return taskStarted();
        }

        @React
        SigTaskStarted wrongContext(SigTaskAddedToProject event, MessageContext msg) {
            return taskStarted();
        }

        @React
        void voidMethod(SigTaskAddedToProject event) {
            // do nothing.
        }

        @React
        SigStartTask commandResult(SigTaskAddedToProject event) {
            return startTask();
        }

        @React
        int nonMessageResult(SigTaskAddedToProject event) {
            return 18;
        }

        @React
        SigTaskStarted justInterface(SignatureTestEvent event) {
            return taskStarted();
        }

        @React
        SigTaskStarted interfaceAndContext(SignatureTestEvent event, EventContext context) {
            return taskStarted();
        }

        @React
        Iterable<UserId> wrongIterable(SigTaskAddedToProject event) {
            return ImmutableList.of();
        }

        @React
        SigTaskStarted declaredThrowable(SigTaskAddedToProject event) throws IOException {
            throw new IOException("An invalid reactor method has thrown an exception");
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

    private static SigStartTask startTask() {
        return SigStartTask.getDefaultInstance();
    }

    private static SigProjectCreated projectCreated() {
        return SigProjectCreated.getDefaultInstance();
    }

    private static SigTaskAddedToProject taskAddedToProject() {
        return SigTaskAddedToProject.getDefaultInstance();
    }

    private static SigTaskStarted taskStarted() {
        return SigTaskStarted.getDefaultInstance();
    }
}
