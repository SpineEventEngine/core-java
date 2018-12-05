/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import io.spine.type.MessageClass;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;

/**
 * Abstract base for classes providing information on messages emitted
 * in the {@link BlackBoxBoundedContext Bounded Context}.
 *
 * @param <C>
 *         the type of the message class
 * @param <W>
 *         the type of the wrapper object containing messages
 * @param <M>
 *         the type of the emitted message
 */
public abstract class EmittedMessages<C extends MessageClass<M>,
                                      W extends Message,
                                      M extends Message> {

    private final ImmutableList<W> messages;
    private final MessageTypeCounter<C, W, M> countByType;
    private final Class<W> wrapperClass;

    EmittedMessages(List<W> messages, MessageTypeCounter<C, W, M> counter, Class<W> wrapperClass) {
        checkNotNull(messages);
        checkNotNull(counter);
        this.messages = copyOf(messages);
        this.countByType = counter;
        this.wrapperClass = wrapperClass;
    }

    /** Obtains the total number of messages. */
    public int count() {
        return messages.size();
    }

    /** Obtains the total number of messages of the passed type. */
    public int count(Class<? extends M> messageClass) {
        checkNotNull(messageClass);
        return countByType.get(messageClass);
    }

    /** Obtains the number of messages with the passed class. */
    public int count(C messageClass) {
        checkNotNull(messageClass);
        return countByType.get(messageClass);
    }

    /** Obtains the number of messages with the passed class. */
    public boolean contain(Class<? extends M> messageClass) {
        checkNotNull(messageClass);
        return countByType.contains(messageClass);
    }

    /**
     * Obtains the number of messages with the passed class.
     */
    public boolean contain(C messageClass) {
        checkNotNull(messageClass);
        return countByType.contains(messageClass);
    }

    /**
     * Obtains a singular word for the name of the emitted object.
     *
     * @apiNote Is used in assertion messages displayed to the programmer.
     * @implNote Calculated as the simple lowercase name of the passed wrapper class.
     */
    public String singular() {
        return wrapperClass.getSimpleName()
                           .toLowerCase();
    }

    /**
     * Obtains a plural word for naming emitted objects.
     *
     * @see #singular()
     */
    public String plural() {
        return singular() + 's';
    }

    protected ImmutableList<W> messages() {
        return messages;
    }
}
