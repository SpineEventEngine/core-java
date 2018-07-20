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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.MessageEnvelope;
import io.spine.core.Version;
import io.spine.server.model.HandlerMethod;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * The events emitted as a result of message dispatch.
 *
 * <p>Dispatch result can be treated in different forms.
 * (e.g. {@link #asMessages() as messages} or {@link #asEvents(Any, Version) as events}).
 *
 * @author Mykhailo Drachuk
 */
public final class DispatchResult {

    private final MessageEnvelope origin;
    private final List<? extends Message> messages;

    /**
     * @param messages messages which were emitted by the dispatch
     * @param origin   a message that was dispatched
     * @param <E>      {@link MessageEnvelope} dispatched message type
     */
    <E extends MessageEnvelope> DispatchResult(List<? extends Message> messages, E origin) {
        this.messages = ImmutableList.copyOf(messages);
        this.origin = origin;
    }

    /**
     * @return dispatch result representation as a list of domain event messages
     */
    public List<? extends Message> asMessages() {
        return ImmutableList.copyOf(this.messages);
    }

    /**
     * @return dispatch result representation as a list of events
     */
    public List<Event> asEvents(Any producerId, @Nullable Version version) {
        return HandlerMethod.toEvents(producerId, version, messages, origin);
    }
}
