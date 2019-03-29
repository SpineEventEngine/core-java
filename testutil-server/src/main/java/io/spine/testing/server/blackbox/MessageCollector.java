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
import io.spine.core.MessageId;
import io.spine.core.TenantId;
import io.spine.server.type.MessageEnvelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Abstract base for message listeners that collect messages posted to a bus.
 *
 * @param <I>
 *         the type of the message identifiers
 * @param <T>
 *         the type of the outer objects of messages
 * @param <E>
 *         the type of the message envelopes
 */
abstract class MessageCollector<I extends MessageId,
                                T extends Message,
                                E extends MessageEnvelope<I, T, ?>>
        implements Consumer<E> {

    private final List<T> outerObjects = new ArrayList<>();
    private final Map<I, Message> messages = new HashMap<>();

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
    public final void accept(E envelope) {
        messages.put(envelope.id(), envelope.message());
        outerObjects.add(envelope.outerObject());
    }

    /**
     * Obtains immutable list with outer objects of messages collected so far.
     */
    public final List<T> all() {
        return ImmutableList.copyOf(outerObjects);
    }

    /**
     * Obtains ID of the tenant to which the passed message belongs.
     */
    protected abstract TenantId tenantOf(T message);

    /**
     * Obtains immutable list with outer objects of messages belonging to the passed tenant.
     */
    public final List<T> ofTenant(TenantId tenantId) {
        checkNotNull(tenantId);
        ImmutableList<T> result =
                outerObjects.stream()
                            .filter(m -> tenantId.equals(tenantOf(m)))
                            .collect(toImmutableList());
        return result;
    }
}
