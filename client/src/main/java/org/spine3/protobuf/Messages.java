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

import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import org.spine3.protobuf.error.MissingDescriptorException;
import org.spine3.protobuf.error.UnknownTypeInAnyException;
import org.spine3.type.ClassName;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;

/**
 * Utility class for working with {@link Message} objects.
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
public class Messages {

    @SuppressWarnings("DuplicateStringLiteralInspection") // This constant is used in generated classes.
    private static final String METHOD_GET_DESCRIPTOR = "getDescriptor";

    private Messages() {}

    /**
     * Wraps {@link Message} object inside of {@link Any} instance.
     *
     * @param message message that should be put inside the {@link Any} instance.
     * @return the instance of {@link Any} object that wraps given message.
     */
    public static Any toAny(Message message) {
        return Any.pack(message);
    }

    /**
     * Creates a new instance of {@link Any} with the message represented by its byte
     * content and the passed type.
     *
     * @param type the type of the message to be wrapped into {@code Any}
     * @param value the byte content of the message
     * @return new {@code Any} instance
     */
    public static Any ofType(TypeName type, ByteString value) {
        final String typeUrl = type.toTypeUrl();
        final Any result = Any.newBuilder()
                .setValue(value)
                .setTypeUrl(typeUrl)
                .build();
        return result;
    }

    /**
     * Unwraps {@link Any} value into an instance of type specified by value
     * returned by {@link Any#getTypeUrl()}.
     *
     * <p>If there is no Java class for the type, {@link UnknownTypeInAnyException}
     * will be thrown.
     *
     * @param any instance of {@link Any} that should be unwrapped
     * @param <T> the type enclosed into {@code Any}
     * @return unwrapped message instance
     * @throws UnknownTypeInAnyException if there is no Java class in the classpath for the enclosed type
     */
    public static <T extends Message> T fromAny(Any any) {
        checkNotNull(any);

        T result = null;
        String typeStr = "";
        try {
            final TypeName typeName = TypeName.ofEnclosed(any);
            typeStr = typeName.value();

            final Class<T> messageClass = toMessageClass(typeName);
            result = any.unpack(messageClass);

        } catch (ClassNotFoundException ignored) {
            throw new UnknownTypeInAnyException(typeStr);
        } catch (InvalidProtocolBufferException e) {
            propagate(e);
        }

        return result;
    }

    /**
     * Returns message {@link Class} for the given Protobuf message type.
     *
     * <p>This method is temporary until full support of {@link Any} is provided.
     *
     * @param messageType full type name defined in the proto files
     * @return message class
     * @throws ClassNotFoundException in case there is no corresponding class for the given Protobuf message type
     * @see #fromAny(Any) that uses the same convention
     */
    public static <T extends Message> Class<T> toMessageClass(TypeName messageType) throws ClassNotFoundException {
        final ClassName className = TypeToClassMap.get(messageType);
        @SuppressWarnings("unchecked")
        final Class<T> result = (Class<T>) Class.forName(className.value());
        return result;
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
        String result = null;
        try {
            result = JsonPrinter.instance().print(message);
        } catch (InvalidProtocolBufferException e) {
            propagate(e);
        }
        checkState(result != null);
        return result;
    }

    /**
     * Builds and returns the registry of types known in the application.
     *
     * @return {@code JsonFormat.TypeRegistry} instance
     * @see TypeToClassMap#knownTypes()
     */
    public static JsonFormat.TypeRegistry forKnownTypes() {
        final JsonFormat.TypeRegistry.Builder builder = JsonFormat.TypeRegistry.newBuilder();
        for (TypeName typeName : TypeToClassMap.knownTypes()) {
            try {
                final Class<? extends Message> clazz = toMessageClass(typeName);
                final Descriptors.GenericDescriptor descriptor = getClassDescriptor(clazz);
                // Skip outer class descriptors.
                if (descriptor instanceof Descriptors.Descriptor) {
                    final Descriptors.Descriptor typeDescriptor = (Descriptors.Descriptor) descriptor;
                    builder.add(typeDescriptor);
                }

            } catch (ClassNotFoundException e) {
                propagate(e);
            }
        }
        return builder.build();
    }

    private enum JsonPrinter {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final JsonFormat.Printer value = JsonFormat.printer().usingTypeRegistry(forKnownTypes());

        private static com.google.protobuf.util.JsonFormat.Printer instance() {
            return INSTANCE.value;
        }
    }

    /**
     * Returns descriptor for the passed message class.
     */
    public static Descriptors.GenericDescriptor getClassDescriptor(Class<? extends Message> clazz) {
        try {
            final Method method = clazz.getMethod(METHOD_GET_DESCRIPTOR);
            final Descriptors.GenericDescriptor result = (Descriptors.GenericDescriptor) method.invoke(null);
            return result;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new MissingDescriptorException(clazz, e.getCause());
        }
    }

    /**
     * Verifies if the passed message object is its default state.
     * @param object the message to inspect
     *
     * @return true if the message is in the default state, false otherwise
     */
    public static boolean isDefault(Message object) {
        return object.getDefaultInstanceForType().equals(object);
    }

    /**
     * Verifies if the passed message object is not its default state.
     * @param object the message to inspect
     *
     * @return true if the message is not in the default state, false otherwise
     */
    public static boolean isNotDefault(Message object) {
        return !isDefault(object);
    }

    /**
     * Ensures that the passed object is not in its default state.
     *
     * @param object the {@code Message} instance to check
     * @param errorMessage the message for the exception to be thrown;
     *                     will be converted to a string using {@link String#valueOf(Object)}
     * @throws IllegalStateException if the object is in its default state
     */
    public static void checkNotDefault(Message object, @Nullable Object errorMessage) {
        checkState(isNotDefault(object), errorMessage);
    }

    /**
     * Ensures that the passed object is not in its default state.
     *
     * @param object the {@code Message} instance to check
     * @param errorMessageTemplate a template for the exception message should the check fail
     * @param errorMessageArgs the arguments to be substituted into the message template
     * @throws IllegalStateException if the object is in its default state
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    public static void checkNotDefault(Message object, String errorMessageTemplate, Object... errorMessageArgs) {
        checkState(isNotDefault(object), errorMessageTemplate, errorMessageArgs);
    }

    /**
     * Ensures that the passed object is not in its default state.
     *
     * @param object the {@code Message} instance to check
     * @throws IllegalStateException if the object is in its default state
     */
    public static void checkNotDefault(Message object) {
        checkNotDefault(object, "The message is in the default state: %s", object);
    }

    /**
     * Ensures that the passed object is in its default state.
     *
     * @param object the {@code Message} instance to check
     * @param errorMessage the message for the exception to be thrown;
     *                     will be converted to a string using {@link String#valueOf(Object)}
     * @throws IllegalStateException if the object is not in its default state
     */
    public static <M extends Message> M checkDefault(M object, @Nullable Object errorMessage) {
        checkState(isDefault(object), errorMessage);
        return object;
    }

    /**
     * Ensures that the passed object is in its default state.
     *
     * @param object the {@code Message} instance to check
     * @param errorMessageTemplate a template for the exception message should the check fail
     * @param errorMessageArgs the arguments to be substituted into the message template
     * @throws IllegalStateException if the object is not in its default state
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    public static <M extends Message> M checkDefault(M object, String errorMessageTemplate,
                                                     Object... errorMessageArgs) {
        checkState(isDefault(object), errorMessageTemplate, errorMessageArgs);
        return object;
    }

    /**
     * Ensures that the passed object is in its default state.
     *
     * @param object the {@code Message} instance to check
     * @throws IllegalStateException if the object is not in its default state
     */
    public static <M extends Message> M checkDefault(M object) {
        checkDefault(object, "The message is not in the default state: %s", object);
        return object;
    }
}
