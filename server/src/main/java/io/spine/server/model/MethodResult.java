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
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
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

    private final @Nullable Object rawMethodOutput;
    private @MonotonicNonNull ImmutableList<V> messages;

    protected MethodResult(@Nullable Object output) {
        rawMethodOutput = output;
    }

    protected final void setMessages(List<V> messages) {
        checkNotNull(messages);
        this.messages = ImmutableList.copyOf(messages);
    }

    protected @Nullable Object getRawMethodOutput() {
        return rawMethodOutput;
    }

    /**
     * Filters the list removing instances of {@link Empty}.
     */
    protected static <M extends Message> List<M> filterEmpty(List<M> messages) {
        List<M> result =
                messages.stream()
                        .filter((m) -> !Empty.getDefaultInstance()
                                             .equals(m))
                        .collect(toList());
        return result;
    }

    /**
     * Obtains messages returned by the method call.
     */
    public List<? extends Message> asMessages() {
        checkNotNull(messages, "Messages are not set");
        return messages;
    }

    /**
     * Obtains a method results with represents no returned value.
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
    protected static List<Message> toMessages(@Nullable Object output) {
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

        if (output instanceof List) {
            // Cast to the list of messages as it is the one of the return types
            // we expect by methods we call.
            List<Message> result = (List<Message>) output;
            return result;
        }

        // If it's not a list it could be another `Iterable`.
        if (output instanceof Iterable) {
            return ImmutableList.copyOf((Iterable<? extends Message>) output);
        }

        // Another type of result is single event message (as Message).
        List<Message> result = singletonList((Message) output);
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
