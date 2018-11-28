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

package io.spine.server.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Streams.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * A result of a {@link HandlerMethod} execution.
 *
 * @param <V> the type of the messages produced by the method
 * @author Mykhailo Drachuk
 * @author Alexander Yevsyukov
 */
public abstract class MethodResult<V extends Message> {

    /**
     * The ignored message types.
     *
     * <p>Messages of these types should not be posted to the system.
     */
    private static final ImmutableSet<? extends Message> IGNORED_MESSAGES = ImmutableSet.of(
            Nothing.getDefaultInstance(),
            Empty.getDefaultInstance()
    );

    private final @Nullable Object rawMethodOutput;
    private @MonotonicNonNull ImmutableList<V> messages;

    protected MethodResult(@Nullable Object output) {
        rawMethodOutput = output;
    }

    /**
     * Assigns messages to a method result object.
     *
     * @apiNote This method is meant to be called from withing a constructor of derived classes,
     *          and called only once.
     *
     * @throws IllegalStateException if messages are already assigned
     */
    protected final void setMessages(List<V> messages) {
        checkState(this.messages == null, "Method result messages are already assigned");
        checkNotNull(messages);
        this.messages = copyOf(messages);
    }

    protected @Nullable Object getRawMethodOutput() {
        return rawMethodOutput;
    }

    /**
     * Filters the list removing instances of the {@linkplain #IGNORED_MESSAGES ignored types}.
     */
    protected static <M extends Message> List<M> filterIgnored(List<M> messages) {
        List<M> result = messages.stream()
                                 .filter(message -> !IGNORED_MESSAGES.contains(message))
                                 .collect(toList());
        return result;
    }

    /**
     * Obtains messages returned by the method call.
     */
    public List<V> asMessages() {
        checkNotNull(messages, "Messages are not set");
        return messages;
    }

    /**
     * Returns result of a method which returns nothing.
     *
     * <p>Such a result could be obtained if a handling method returns {@code void},
     * or {@code Empty}, if the contract requires returning a {@code Message}.
     */
    public static MethodResult<Empty> empty() {
        return EmptyResult.INSTANCE;
    }

    /**
     * Casts a handling result to a list of event messages.
     *
     * @param output the command handler method return value.
     *               Could be a {@link Message}, a list of messages, or {@code null}.
     * @return the list of event messages or an empty list if {@code null} is passed
     */
    @SuppressWarnings({"unchecked", "ChainOfInstanceofChecks"})
    protected static <V extends Message> List<V> toMessages(@Nullable Object output) {
        if (output == null) {
            return emptyList();
        }

        // Allow reacting methods to return `Empty` instead of empty `List`. Do not store such
        // events. Command Handling methods except those of `ProcessManager`s will not be able to
        // use this trick because we check for non-empty result of such methods. `ProcessManager`
        // command handlers are allowed to return `Empty` but not empty event `List`.
        if (output instanceof Empty) {
            return emptyList();
        }

        // Allow `Optional` for event reactions and command generation in response to events.
        if (output instanceof Optional) {
            Optional optional = (Optional) output;
            if (optional.isPresent()) {
                V message = (V) optional.get();
                return ImmutableList.of(message);
            } else {
                return emptyList();
            }
        }

        if (output instanceof List) {
            // Cast to the list of messages as it is the one of the return types
            // we expect by methods we call.
            List<V> result = (List<V>) output;
            return result;
        }

        // If it's not a list it could be another `Iterable`.
        if (output instanceof Iterable) {
            Iterable<V> iterable = (Iterable<V>) output;
            return copyOf(iterable);
        }

        // Another type of result is single event message (as Message).
        V singleMessage = (V) output;
        List<V> result = singletonList(singleMessage);
        return result;
    }

    /**
     * An event applier does not return values.
     */
    private static final class EmptyResult extends MethodResult<Empty> {

        private static final EmptyResult INSTANCE = new EmptyResult();

        private EmptyResult() {
            super(null);
            setMessages(ImmutableList.of());
        }
    }
}
