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
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.core.MessageEnvelope;
import io.spine.core.RejectionEnvelope;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An abstract {@link MessageEnvelope message envelope} dispatch.
 *
 * <p>Target and method of dispatch are specified by the {@code Dispatch} inheritors.
 *
 * @author Mykhailo Drachuk
 */
public abstract class Dispatch<E extends MessageEnvelope> {

    private final E envelope;

    protected Dispatch(E envelope) {
        this.envelope = envelope;
    }

    /**
     * Dispatches an envelope to the target in a manner specified by inheritor.
     */
    protected abstract List<? extends Message> dispatch();

    /**
     * @return a {@link MessageEnvelope message envelope} which is handled by
     *         the current dispatch
     */
    protected E envelope() {
        return envelope;
    }

    /**
     * Performs the dispatch of a message to the target and processes its results.
     *
     * @return the events emitted after dispatching a message
     */
    public DispatchResult perform() {
        List<? extends Message> messages = dispatch();
        List<? extends Message> filtered = Filtering.of(messages)
                                                    .perform();
        return new DispatchResult(filtered, envelope);
    }

    /**
     * @param command an envelope which is dispatched to some target
     * @return a {@link MessageDispatchFactory dispatch factory} for the provided
     *         {@link CommandEnvelope command envelope}
     */
    public static CommandDispatchFactory of(CommandEnvelope command) {
        checkNotNull(command);
        return new CommandDispatchFactory(command);
    }

    /**
     * @param event an envelope which is dispatched to some target
     * @return a {@link MessageDispatchFactory dispatch factory} for the provided
     *         {@link EventEnvelope event envelope}
     */
    public static EventDispatchFactory of(EventEnvelope event) {
        checkNotNull(event);
        return new EventDispatchFactory(event);
    }

    /**
     * @param rejection an envelope which is dispatched to some target
     * @return a {@link MessageDispatchFactory dispatch factory} for the provided
     *         {@link RejectionEnvelope rejection envelope}
     */
    public static RejectionDispatchFactory of(RejectionEnvelope rejection) {
        checkNotNull(rejection);
        return new RejectionDispatchFactory(rejection);
    }
}
