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
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.protobuf.AnyPacker;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static io.spine.protobuf.AnyPacker.pack;

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
            pack(Nothing.getDefaultInstance()),
            pack(Empty.getDefaultInstance())
    );

    private final ImmutableList<Any> messages;

    private MethodResult(ImmutableList<Any> messages) {
        this.messages = messages;
    }

    static MethodResult from(@Nullable Object rawMethodOutput) {
        ImmutableList<Any> packedMessages = toMessages(rawMethodOutput);
        ImmutableList<Any> filtered = filterIgnored(packedMessages);
        return new MethodResult(filtered);
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private static ImmutableList<Any> toMessages(@Nullable Object output) {
        ImmutableList<Any> emptyList = ImmutableList.of();
        if (output == null) {
            return emptyList;
        }

        // Allow reacting methods to return `Empty` instead of empty `List`. Do not store such
        // events. Command Handling methods except those of `ProcessManager`s will not be able to
        // use this trick because we check for non-empty result of such methods. `ProcessManager`
        // command handlers are allowed to return `Empty` but not empty event `List`.
        if (output instanceof Empty) {
            return emptyList;
        }

        // Allow `Optional` for event reactions and command generation in response to events.
        if (output instanceof Optional) {
            Optional optional = (Optional) output;
            if (optional.isPresent()) {
                Message message = (Message) optional.get();
                Any pack = pack(message);
                return ImmutableList.of(pack);
            } else {
                return emptyList;
            }
        }

        if (output instanceof Iterable) {
            @SuppressWarnings("unchecked")
            Iterable<Message> messages = (Iterable<Message>) output;
            ImmutableList<Any> packedMessages = stream(messages)
                    .map(AnyPacker::pack)
                    .collect(toImmutableList());
            // TODO:2019-06-28:dmytro.dashenkov: Performance considerations.
            return packedMessages;
        }

        // Another type of result is single event message (as Message).
        Message singleMessage = (Message) output;
        return ImmutableList.of(pack(singleMessage));
    }

    /**
     * Filters the list removing instances of the {@linkplain #IGNORED_MESSAGES ignored types}.
     */
    private static ImmutableList<Any> filterIgnored(ImmutableList<Any> messages) {
        ImmutableList<Any> result = messages.stream()
                                 .filter(message -> !IGNORED_MESSAGES.contains(message))
                                 .collect(toImmutableList());
        return result;
    }

    ImmutableList<Any> messages() {
        return messages;
    }
}
