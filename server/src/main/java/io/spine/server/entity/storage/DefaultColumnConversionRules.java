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

package io.spine.server.entity.storage;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.checkerframework.checker.nullness.qual.Nullable;

import static io.spine.server.entity.storage.ConversionRule.identity;

public final class DefaultColumnConversionRules extends AbstractColumnConversionRules<Object> {

    public static final DefaultColumnConversionRules INSTANCE = new DefaultColumnConversionRules();

    /**
     * Prevents external construction so the class is accessed only through {@link #INSTANCE}.
     */
    private DefaultColumnConversionRules() {
        super();
    }

    @Override
    protected ConversionRule<String, String> ofString() {
        return identity();
    }

    @Override
    protected ConversionRule<Integer, Integer> ofInteger() {
        return identity();
    }

    @Override
    protected ConversionRule<Long, Long> ofLong() {
        return identity();
    }

    @Override
    protected ConversionRule<Float, Float> ofFloat() {
        return identity();
    }

    @Override
    protected ConversionRule<Double, Double> ofDouble() {
        return identity();
    }

    @Override
    protected ConversionRule<Boolean, Boolean> ofBoolean() {
        return identity();
    }

    @Override
    protected ConversionRule<ByteString, ByteString> ofByteString() {
        return identity();
    }

    @Override
    protected ConversionRule<Enum<?>, Enum<?>> ofEnum() {
        return identity();
    }

    @Override
    protected ConversionRule<Message, Message> ofMessage() {
        return identity();
    }

    @SuppressWarnings("ReturnOfNull")
    @Override
    public ConversionRule<@Nullable ?, @Nullable ?> ofNull() {
        return o -> null;
    }
}
