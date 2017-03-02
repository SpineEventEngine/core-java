/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.bus;

import org.spine3.base.MessageClass;
import org.spine3.base.MessageEnvelope;

import java.util.Set;

/**
 * A dispatcher of a message.
 *
 * @param <C> the type of dispatched messages
 * @param <E> the type of envelopes for dispatched objects that contain messages
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 */
public interface MessageDispatcher<C extends MessageClass,
                                   E extends MessageEnvelope> {
    /**
     * Obtains a set of message classes that can be processed by this dispatcher.
     *
     * @return non-empty set of command classes
     */
    Set<C> getMessageClasses();

    /**
     * Dispatches the message contained in the passed envelope.
     *
     * @param envelope the envelope with the message
     */
    void dispatch(E envelope);
}
