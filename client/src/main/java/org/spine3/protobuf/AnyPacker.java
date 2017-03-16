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

package org.spine3.protobuf;

import com.google.common.base.Function;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.spine3.type.TypeUrl;
import org.spine3.type.error.UnexpectedTypeException;

import javax.annotation.Nullable;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utilities for packing messages into {@link Any} and unpacking them.
 *
 * <p>When packing, the {@code AnyPacker} takes care of obtaining correct type URL prefix
 * for the passed messages.
 *
 * <p>When unpacking, the {@code AnyPacker} obtains Java class matching the type URL
 * from the passed {@code Any}.
 *
 * @author Alexander Yevsyukov
 * @see Any#pack(Message, String)
 * @see Any#unpack(Class)
 */
public class AnyPacker {

    private static final Function<Any, Message> ANY_UNPACKER = new Function<Any, Message>() {
        @Nullable
        @Override
        public Message apply(@Nullable Any input) {
            if (input == null) {
                return null;
            }
            return unpack(input);
        }
    };

    private AnyPacker() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Wraps {@link Message} object inside of {@link Any} instance.
     *
     * <p>If an instance of {@code Any} passed, this instance is returned.
     *
     * @param message the message to pack
     * @return the wrapping instance of {@link Any} or the message itself, if it is {@code Any}
     */
    public static Any pack(Message message) {
        if (message instanceof Any) {
            return (Any) message;
        }
        final TypeUrl typeUrl = TypeUrl.from(message.getDescriptorForType());
        final String typeUrlPrefix = typeUrl.getPrefix();
        final Any result = Any.pack(message, typeUrlPrefix);
        return result;
    }

    /**
     * Unwraps {@code Any} value into an instance of type specified by value
     * returned by {@link Any#getTypeUrl()}.
     *
     *
     * @param any instance of {@link Any} that should be unwrapped
     * @param <T> the type enclosed into {@code Any}
     * @return unwrapped message instance
     */
    public static <T extends Message> T unpack(Any any) {
        checkNotNull(any);
        final TypeUrl typeUrl = TypeUrl.ofEnclosed(any);
        final Class<T> messageClass = typeUrl.getJavaClass();
        return unpack(any, messageClass);
    }

    /**
     * Unwraps {@code Any} value into an instance of the passed class.
     *
     * <p>If there is no Java class for the type,
     * {@link UnexpectedTypeException UnexpectedTypeException}
     * will be thrown.
     *
     * @param any   instance of {@link Any} that should be unwrapped
     * @param cls the class implementing the type of the enclosed object
     * @param <T>   the type enclosed into {@code Any}
     * @return unwrapped message instance
     */
    public static <T extends Message> T unpack(Any any, Class<T> cls) {
        try {
            final T result = any.unpack(cls);
            return result;
        } catch (InvalidProtocolBufferException e) {
            throw new UnexpectedTypeException(e);
        }
    }

    /**
     * Creates an iterator that packs each incoming message into {@code Any}.
     *
     * @param iterator the iterator over messages to pack
     * @return the packing iterator
     */
    public static Iterator<Any> pack(Iterator<Message> iterator) {
        return new PackingIterator(iterator);
    }

    /**
     * Provides the function for unpacking messages from {@code Any}.
     *
     * <p>The function returns {@code null} for {@code null} input.
     */
    public static Function<Any, Message> unpackFunc() {
        return ANY_UNPACKER;
    }
}
