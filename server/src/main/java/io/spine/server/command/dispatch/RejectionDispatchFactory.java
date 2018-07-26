/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.command.dispatch;

import com.google.protobuf.Message;
import io.spine.core.RejectionEnvelope;
import io.spine.server.model.ReactorMethodResult;
import io.spine.server.rejection.model.RejectionReactorMethod;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link MessageDispatchFactory message dispatch factory} that deals 
 * with {@link RejectionEnvelope rejection envelopes}. 
 * 
 * @author Mykhailo Drachuk
 */
public final class RejectionDispatchFactory extends MessageDispatchFactory<RejectionEnvelope, 
                                                                           RejectionReactorMethod> {
    RejectionDispatchFactory(RejectionEnvelope rejection) {
        super(rejection);
    }

    /** {@inheritDoc} */
    @Override
    public Dispatch<RejectionEnvelope> to(Object context, RejectionReactorMethod method) {
        checkNotNull(context);
        checkNotNull(method);
        return new RejectionMethodDispatch(envelope(), method, context);
    }

    /**
     * A dispatch of a {@link RejectionEnvelope rejection envelope} 
     * to a {@link RejectionReactorMethod rejection reactor method}.
     */
    private static class RejectionMethodDispatch extends Dispatch<RejectionEnvelope> {
        private final RejectionReactorMethod method;
        private final Object context;

        private RejectionMethodDispatch(RejectionEnvelope envelope,
                                        RejectionReactorMethod method,
                                        Object context) {
            super(envelope);
            this.method = method;
            this.context = context;
        }

        @Override
        protected List<? extends Message> dispatch() {
            RejectionEnvelope rejection = envelope();
            ReactorMethodResult result =
                    method.invoke(context, rejection.getMessage(), rejection.getRejectionContext());
            return result.asMessages();
        }
    }
}
