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

import com.google.common.base.Optional;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;
import org.spine3.type.KnownTypes;
import org.spine3.type.TypeUrl;
import org.spine3.type.error.UnknownTypeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.Descriptors.Descriptor;

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
     * Converts passed message into Json representation.
     *
     * @param message the message object
     * @return Json string
     */
    public static String toJson(Message message) {
        checkNotNull(message);
        String result;
        try {
            result = JsonPrinter.instance().print(message);
        } catch (InvalidProtocolBufferException e) {
            throw new UnknownTypeException(e);
        }
        checkState(result != null);
        return result;
    }

    /**
     * Builds and returns the registry of types known in the application.
     *
     * @return {@code JsonFormat.TypeRegistry} instance
     */
    static JsonFormat.TypeRegistry forKnownTypes() {
        final JsonFormat.TypeRegistry.Builder builder = JsonFormat.TypeRegistry.newBuilder();
        for (TypeUrl typeUrl : KnownTypes.getAllUrls()) {
            final Optional<Descriptor> typeDescriptor = typeUrl.getDescriptor();
            // Skip outer class descriptors.
            if (typeDescriptor.isPresent()) {
                builder.add(typeDescriptor.get());
            }
        }
        return builder.build();
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

    private enum JsonPrinter {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final JsonFormat.Printer value = JsonFormat.printer()
                                                           .usingTypeRegistry(forKnownTypes());

        private static JsonFormat.Printer instance() {
            return INSTANCE.value;
        }
    }
}
