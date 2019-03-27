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

package io.spine.testing.server.blackbox;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.base.MessageContext;
import io.spine.core.Ack;
import io.spine.core.MessageId;
import io.spine.server.bus.BusFilter;
import io.spine.server.type.MessageEnvelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Abstract base for bus filters that remember passing messages.
 *
 * <p>Always accepts the posted messages.
 *
 * @param <I>
 *         the type of the message identifiers
 * @param <T>
 *         the type of the outer objects of messages
 * @param <C>
 *         the type of message contexts
 * @param <E>
 *         the type of the message envelopes
 */
abstract class MemoizingTap<I extends MessageId,
                            T extends Message,
                            C extends MessageContext,
                            E extends MessageEnvelope<I, T, C>> implements BusFilter<E> {

    private final Map<I, Message> messages = new HashMap<>();
    private final List<T> outerObjects = new ArrayList<>();

    /**
     * Looks up the command message by the command ID.
     */
    public final <M extends Message> Optional<M> find(I messageId, Class<M> messageClass) {
        Message commandMessage = messages.get(messageId);
        assertThat(commandMessage, instanceOf(messageClass));
        @SuppressWarnings("unchecked") // Checked with an assertion.
                M result = (M) commandMessage;
        return ofNullable(result);
    }

    /**
     * Remembers the passed message and accepts its, returning empty {@code Optional}.
     */
    @Override
    public final Optional<Ack> accept(E envelope) {
        messages.put(envelope.id(), envelope.message());
        outerObjects.add(envelope.outerObject());
        return Optional.empty();
    }

    /**
     * Obtains immutable list with outer objects by the tap so far.
     */
    final List<T> outerObjects() {
        return ImmutableList.copyOf(outerObjects);
    }
}
