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

package io.spine.server.entity;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.util.FieldMaskUtil;

import javax.annotation.Nonnull;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A utility class for creating instances of {@code FieldMask} and processing them
 * against instances of {@link Message}.
 */
public final class FieldMasks {

    /** Prevent instantiation of this utility class. */
    private FieldMasks() {
    }

    /**
     * Applies the given {@code FieldMask} to given collection of {@link Message}s.
     * Does not change the {@link Collection} itself.
     *
     * <p>In case the {@code FieldMask} instance contains invalid field declarations, they are
     * ignored and do not affect the execution result.
     *
     * @param mask     {@code FieldMask} to apply to each item of the input {@link Collection}.
     * @param messages {@link Message}s to filter.
     * @return messages with the {@code FieldMask} applied
     */
    @Nonnull
    public static <M extends Message>
    Collection<M> applyMask(FieldMask mask, Collection<M> messages) {
        checkNotNull(mask);
        checkNotNull(messages);
        return doApplyMany(mask, messages);
    }

    /**
     * Applies the {@code FieldMask} to the given {@link Message}
     * if the {@code mask} parameter is valid.
     *
     * <p>In case the {@code FieldMask} instance contains invalid field declarations,
     * they are ignored and do not affect the execution result.
     *
     * @param mask    the {@code FieldMask} to apply.
     * @param message the {@link Message} to apply given mask to.
     * @return the message of the same type as the given one with only selected fields if
     * the {@code mask} is valid, original message otherwise.
     */
    public static <M extends Message> M applyMask(FieldMask mask, M message) {
        checkNotNull(mask);
        checkNotNull(message);
        if (mask.getPathsList()
                .isEmpty()) {
            return message;
        }
        M result = distill(message, mask);
        return result;
    }

    private static <M extends Message> M distill(M wholeMessage, FieldMask mask) {
        Message.Builder builder = wholeMessage.newBuilderForType();
        FieldMaskUtil.merge(mask, wholeMessage, builder);
        @SuppressWarnings("unchecked") // safe as we got builder of `M`.
                M result = (M) builder.build();
        return result;
    }

    private static <M extends Message>
    ImmutableList<M> doApplyMany(FieldMask mask, Collection<M> messages) {
        ImmutableList<M> input = ImmutableList.copyOf(messages);
        if (input.isEmpty()) {
            return input;
        }

        ProtocolStringList filter = mask.getPathsList();
        if (filter.isEmpty()) {
            return input;
        }

        ImmutableList.Builder<M> filtered = ImmutableList.builder();
        for (M wholeMessage : messages) {
            M distilled = distill(wholeMessage, mask);
            filtered.add(distilled);
        }
        return filtered.build();
    }
}
