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

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.spine3.protobuf.error.UnexpectedTypeException;
import org.spine3.protobuf.error.UnknownTypeException;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.Messages.toMessageClass;

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
        return unpack(any, messageClass);
    }

    /**
     * Unwraps {@code Any} value into an instance of the passed class.
     *
     * @param any   instance of {@link Any} that should be unwrapped
     * @param clazz the class implementing the type of the enclosed object
     * @param <T>   the type enclosed into {@code Any}
     * @return unwrapped message instance
     */
    public static <T extends Message> T unpack(Any any, Class<T> clazz) {
        try {
            final T result = any.unpack(clazz);
            return result;
        } catch (InvalidProtocolBufferException e) {
            throw new UnexpectedTypeException(e);
        }
    }

    /**
     * Creates an iterator that packs each incoming messages into {@code Any}.
     *
     * @param iterator the iterator over messages to pack
     * @return the packing iterator
     */
    public static Iterator<Any> pack(Iterator<Message> iterator) {
        return new PackingIterator(iterator);
    }

    /**
     * An iterator that packs messages from the associated iterator.
     */
    private static class PackingIterator implements Iterator<Any> {

        private final Iterator<Message> source;

        private PackingIterator(Iterator<Message> source) {
            this.source = source;
        }

        @Override
        public boolean hasNext() {
            return source.hasNext();
        }

        @Override
        public Any next() {
            final Message next = source.next();
            final Any result = pack(next);
            return result;
        }

        @SuppressWarnings("NewExceptionWithoutArguments")
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
