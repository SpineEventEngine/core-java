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
import io.spine.core.EventEnvelope;
import io.spine.server.event.model.EventReactorMethod;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link MessageDispatchFactory message dispatch factory} that deals
 * with {@link EventEnvelope event envelopes}.
 *
 * @author Mykhailo Drachuk
 */
public final class EventDispatchFactory extends MessageDispatchFactory<EventEnvelope,
                                                                       EventReactorMethod> {
    EventDispatchFactory(EventEnvelope event) {
        super(event);
    }

    /** {@inheritDoc} */
    @Override
    public Dispatch<EventEnvelope> to(Object context, EventReactorMethod method) {
        checkNotNull(context);
        checkNotNull(method);
        return new EventMethodDispatch(envelope(), method, context);
    }

    /**
     * A dispatch of a {@link EventEnvelope event envelope}
     * to a {@link EventReactorMethod event reactor method}.
     */
    private static class EventMethodDispatch extends Dispatch<EventEnvelope> {
        private final EventReactorMethod method;
        private final Object context;

        private EventMethodDispatch(EventEnvelope envelope,
                                    EventReactorMethod method,
                                    Object context) {
            super(envelope);
            this.method = method;
            this.context = context;
        }

        @Override
        protected DispatchResult dispatch() {
            EventEnvelope event = envelope();
            List<? extends Message> events = method.invoke(context, event.getMessage(),
                                                           event.getEventContext());
            DispatchResult result = DispatchResult.ofEvents(events, envelope());
            return result;
        }
    }
}
