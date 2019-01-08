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

package io.spine.server.bus;

import com.google.common.collect.ImmutableSet;
import io.spine.core.MessageEnvelope;
import io.spine.type.MessageClass;

import java.util.Set;

/**
 * Dispatches a message to several entities of the same type.
 *
 * @param <C> the type of dispatched messages
 * @param <E> the type of envelopes for dispatched objects that contain messages
 * @param <I> the type of IDs of entities to which messages are dispatched
 * @author Alexander Yevsyukov
 */
public interface MulticastDispatcher <C extends MessageClass, E extends MessageEnvelope, I>
        extends MessageDispatcher<C, E, Set<I>> {

    /**
     * Returns immutable set with one element with the identity of the multicast dispatcher
     * that dispatches messages to itself.
     *
     * @implNote The identity obtained as the result of {@link Object#toString()
     * MulticastDispatcher.toString()}.
     *
     * @return immutable set with the dispatcher identity
     */
    default Set<String> identity() {
        return ImmutableSet.of(this.toString());
    }
}
