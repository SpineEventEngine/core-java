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
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.spine3.protobuf.AnyPacker.pack;

/**
 * Utility class for working with {@link Message} objects.
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
public class Messages {

    private Messages() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Prints the passed message into well formatted text.
     *
     * @param message the message object
     * @return text representation of the passed message
     */
    public static String toText(Message message) {
        checkNotNull(message);
        final String result = TextFormat.printToString(message);
        return result;
    }

    /**
     * Safely packs the given {@link Message} object as {@link Any}.
     *
     * <p>If the passed message object is already packed as {@code Any}, just
     * casts it to {@code Any} and returns the result.
     *
     * @param messageOrAny the message object
     * @return {@code Any}
     */
    public static Any toAny(Message messageOrAny) {
        return (messageOrAny instanceof Any)
                ? (Any) messageOrAny
                : pack(messageOrAny);
    }

    /**
     * Creates a new instance of a {@code Message} by its class.
     *
     * <p>This factory method obtains parameterless constructor {@code Message} via
     * Reflection and then invokes it.
     *
     * @return new instance
     */
    public static <M extends Message> M newInstance(Class<M> messageClass) {
        checkNotNull(messageClass);
        try {
            final Constructor<M> constructor = messageClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            final M state = constructor.newInstance();
            return state;
        } catch (NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the builder of the {@code Message}.
     *
     * @param clazz the message class
     * @param <B>   the builder type
     * @return the message builder
     */
    public static <B extends Message.Builder> B builderFor(Class<? extends Message> clazz) {
        try {
            final Method factoryMethod = clazz.getDeclaredMethod("newBuilder");
            @SuppressWarnings("unchecked")
            final B result = (B) factoryMethod.invoke(null);
            return result;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            final String errMsg = format("Class %s must be a generated proto message",
                                         clazz.getCanonicalName());
            throw new IllegalArgumentException(errMsg, e);
        }
    }
}
