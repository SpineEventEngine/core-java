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

package io.spine.server.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * Result of a handler method processing a signal.
 *
 * <p>A handler method may return several types of objects. A {@code MessageResult} converts
 * a generic returned value into a required format.
 */
final class MethodResult {

    /**
     * The ignored message types.
     *
     * <p>Messages of these types should not be posted to the system.
     */
    private static final ImmutableSet<? extends Message> IGNORED_MESSAGES = ImmutableSet.of(
            Nothing.getDefaultInstance(),
            Empty.getDefaultInstance()
    );

    private final ImmutableList<Message> messages;

    private MethodResult(ImmutableList<Message> messages) {
        this.messages = messages;
    }

    static MethodResult from(@Nullable Object rawMethodOutput) {
        List<Message> messages = toMessages(rawMethodOutput);
        ImmutableList<Message> filtered = filterIgnored(messages);
        return new MethodResult(filtered);
    }

    private static List<Message> toMessages(@Nullable Object output) {
        if (output == null) {
            return ImmutableList.of();
        } else if (output instanceof Optional) {
            // Allow `Optional` for event reactions and command generation in response to events.
            Optional<?> optional = (Optional<?>) output;
            return optional.map(MethodResult::singleton)
                           .orElse(ImmutableList.of());
        } else if (output instanceof Iterable) {
            @SuppressWarnings("unchecked")
            Iterable<Message> messages = (Iterable<Message>) output;
            return ImmutableList.copyOf(messages);
        } else {
            // Another type of result is single event message (as Message).
            return singleton(output);
        }
    }

    private static List<Message> singleton(Object messageDisguised) {
        Message message = (Message) messageDisguised;
        return ImmutableList.of(message);
    }

    /**
     * Filters the list removing instances of the {@linkplain #IGNORED_MESSAGES ignored types}.
     */
    private static ImmutableList<Message> filterIgnored(List<Message> messages) {
        ImmutableList<Message> result = messages
                .stream()
                .filter(message -> !IGNORED_MESSAGES.contains(message))
                .collect(toImmutableList());
        return result;
    }

    /**
     * Obtains the method result as a list of messages.
     *
     * @param messageType
     *         class of messages used; not used by the method, required only to satisfy the compiler
     * @param <T>
     *         the type of the messages
     * @return list of messages
     */
    <T extends Message> ImmutableList<T> messages(
            @SuppressWarnings("unused") Class<T> messageType) {
        @SuppressWarnings("unchecked")
        ImmutableList<T> castMessages = (ImmutableList<T>) this.messages;
        return castMessages;
    }
}
