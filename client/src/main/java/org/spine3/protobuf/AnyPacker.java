/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.protobuf;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.spine3.protobuf.error.UnknownTypeException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static org.spine3.protobuf.Messages.*;

/**
 * Utilities for working with {@link Any}.
 *
 * @author Alexander Yevsyukov
 */
public class AnyPacker {

    private AnyPacker() {}

    /**
     * Wraps {@link Message} object inside of {@link Any} instance.
     *
     * @param message message that should be put inside the {@link Any} instance
     * @return the instance of {@link Any} object that wraps given message
     */
    public static Any pack(Message message) {
        final TypeUrl typeUrl = TypeUrl.of(message.getDescriptorForType());
        final String typeUrlPrefix = typeUrl.getPrefix();
        final Any result = Any.pack(message, typeUrlPrefix);
        return result;
    }

    /**
     * Unwraps {@link Any} value into an instance of type specified by value
     * returned by {@link Any#getTypeUrl()}.
     *
     * <p>If there is no Java class for the type, {@link UnknownTypeException}
     * will be thrown.
     *
     * @param any instance of {@link Any} that should be unwrapped
     * @param <T> the type enclosed into {@code Any}
     * @return unwrapped message instance
     * @throws UnknownTypeException if there is no Java class in the classpath for the enclosed type
     */
    public static <T extends Message> T unpack(Any any) {
        checkNotNull(any);
        final TypeUrl typeUrl = TypeUrl.ofEnclosed(any);
        final Class<T> messageClass = toMessageClass(typeUrl);
        try {
            final T result = any.unpack(messageClass);
            return result;
        } catch (InvalidProtocolBufferException e) {
            throw propagate(e);
        }
    }
}
