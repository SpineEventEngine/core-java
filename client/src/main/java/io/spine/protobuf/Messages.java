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
package io.spine.protobuf;

import com.google.protobuf.Message;
import io.spine.annotation.Internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Utility class for working with {@link Message} objects.
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
public final class Messages {

    /** The name of a message builder factory method. */
    public static final String METHOD_NEW_BUILDER = "newBuilder";

    private Messages() {
        // Prevent instantiation of this utility class.
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
        checkNotNull(clazz);
        try {
            final Method factoryMethod = clazz.getDeclaredMethod(METHOD_NEW_BUILDER);
            @SuppressWarnings("unchecked")
            final B result = (B) factoryMethod.invoke(null);
            return result;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            final String errMsg = format("Class %s must be a generated proto message",
                                         clazz.getCanonicalName());
            throw new IllegalArgumentException(errMsg, e);
        }
    }

    /**
     * Checks that the {@code Type} is a {@code Class} of the {@code Message}.
     *
     * @param typeToCheck the type to check
     * @return {@code true} if the type is message class, {@code false} otherwise
     */
    @Internal
    public static boolean isMessage(Type typeToCheck) {
        checkNotNull(typeToCheck);
        if (typeToCheck instanceof Class) {
            final Class<?> aClass = (Class) typeToCheck;
            final boolean isMessage = Message.class.isAssignableFrom(aClass);
            return isMessage;
        }
        return false;
    }
}
