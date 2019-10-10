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

package io.spine.server.event.model.given.subscriber;

import io.spine.base.MessageContext;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.core.UserId;
import io.spine.model.contexts.projects.command.SigCreateProject;
import io.spine.model.contexts.projects.command.SigStartTask;
import io.spine.model.contexts.projects.event.SigTaskAddedToProject;
import io.spine.model.contexts.projects.rejection.ProjectRejections;
import io.spine.model.contexts.projects.rejection.SigCannotCreateProject;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.model.given.SignatureTestEvent;

import java.io.IOException;

/**
 * A test environment for {@link io.spine.server.event.model.ReactorSignatureTest
 * ReactorSignatureTest}.
 */
public class EventSubscriberSignatureTestEnv {

    /** Prevents this test environment from direct initialization. */
    private EventSubscriberSignatureTestEnv() {
    }

    /**
     * A standalone event subscriber which declares valid {@link Subscribe event-subscribing
     * handlers}.
     *
     * <p>This class declares the duplicate handlers for some events, hence it cannot be
     * registered in any Bounded Context. This is done for simplicity of enumerating all possible
     * combinations of parameters.
     */
    public static final class ValidSubscriber extends AbstractEventSubscriber {

        @Subscribe
        void singleMsgSingleResult(SigTaskAddedToProject event) {
            // do nothing.
        }

        @Subscribe
        void msgWithCtxSingleResult(SigTaskAddedToProject event, EventContext ctx) {
            // do nothing.
        }

        @Subscribe
        void justRejection(ProjectRejections.SigCannotCreateProject rejection) {
            // do nothing.
        }

        @Subscribe
        void rejectionWithCtx(ProjectRejections.SigCannotCreateProject r, CommandContext ctx) {
            // do nothing.
        }

        @Subscribe
        void rejectionWithCommand(ProjectRejections.SigCannotCreateProject rejection,
                                  SigCreateProject command) {
            // do nothing.
        }

        @Subscribe
        void rejectionWithCommandAndCtx(ProjectRejections.SigCannotCreateProject rejection,
                                        SigCreateProject cmd,
                                        CommandContext ctx) {
            // do nothing.
        }

        @Subscribe
        private void privateHandler(SigTaskAddedToProject event) {
            // do nothing.
        }

        @Subscribe
        private void protectedHandler(SigTaskAddedToProject event) {
            // do nothing.
        }

        @Subscribe
        public void publicHandler(SigTaskAddedToProject event) {
            // do nothing.
        }
    }

    /**
     * A standalone commander which declares invalid {@link React event-reacting handlers}.
     *
     * <p>Being similar to {@link ValidSubscriber}, this class also declares duplicate handlers
     * for the same events. This is the way to avoid lots of small classes enumerating
     * all possible combinations or params.
     */
    public static final class InvalidSubscriber extends AbstractEventSubscriber {

        @Subscribe
        void noParams() {
            // do nothing.
        }

        @Subscribe
        void nonEventMessageParam(UserId user) {
            // do nothing.
        }

        @Subscribe
        void nonMessageParam(int event) {
            // do nothing.
        }

        @Subscribe
        void wrongThreeParams(SigTaskAddedToProject event, EventContext ctx, Nothing third) {
            // do nothing.
        }

        @React
        void rejectionAndThreeMoreParams(ProjectRejections.SigCannotCreateProject rejection,
                                         SigCreateProject cmd,
                                         CommandContext ctx,
                                         UserId user) {
            // do nothing.
        }

        @Subscribe
        void wrongSecondParam(SigTaskAddedToProject event, Nothing message) {
            // do nothing.
        }

        @Subscribe
        void wrongContext(SigTaskAddedToProject event, MessageContext msg) {
            // do nothing.
        }

        @Subscribe
        SigStartTask messageResult(SigTaskAddedToProject event) {
            return SigStartTask.getDefaultInstance();
        }

        @Subscribe
        int nonMessageResult(SigTaskAddedToProject event) {
            return 18;
        }

        @Subscribe
        void justInterface(SignatureTestEvent event) {
            // do nothing.
        }

        @Subscribe
        void interfaceAndContext(SignatureTestEvent event, EventContext context) {
            // do nothing.
        }

        @Subscribe
        void declaredThrowable(SigTaskAddedToProject event) throws IOException {
            throw new IOException("An invalid subscriber method has thrown an exception");
        }

        @Subscribe
        void declaredRejection(SigTaskAddedToProject e) throws SigCannotCreateProject {
            throw SigCannotCreateProject.newBuilder()
                                        .build();
        }
    }
}
