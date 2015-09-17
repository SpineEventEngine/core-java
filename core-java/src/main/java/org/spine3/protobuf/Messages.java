/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
import org.spine3.ClassName;
import org.spine3.TypeName;

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
@SuppressWarnings("UtilityClass")
public class Messages {

    private static final String METHOD_PARSE_FROM = "parseFrom";
    private static final String METHOD_GET_DESCRIPTOR = "getDescriptor";

    private static final String MSG_NO_SUCH_METHOD = "Method %s() is not defined in the class %s.";
    private static final String MSG_UNABLE_TO_ACCESS = "Method %s() is not accessible in the class %s.";
    private static final String MSG_ERROR_INVOKING = "Error invoking %s() of the class %s: %s";

    private Messages() {}

    /**
     * Wraps {@link Message} object inside of {@link Any} instance.
     * The protobuf fully qualified name is used as typeUrl param,
     * {@link Message#toByteString()} is used to get the {@link ByteString} message representation.
     *
     * @param message message that should be put inside the {@link Any} instance.
     * @return the instance of {@link Any} object that wraps given message.
     */
    public static Any toAny(Message message) {
        checkNotNull(message);

        Any result = Any.pack(message);
//        Any result = Any.newBuilder()
//                .setTypeUrl(message.getDescriptorForType().getFullName())
//                .setValue(message.toByteString())
//                .build();

        return result;
    }

    /**
     * Creates a new instance of {@link Any} with the message represented by its byte
     * content and passed type.
     *
     * @param type the type of the message to be wrapped into {@code Any}
     * @param value the byte content of the message
     * @return new {@code Any} instance
     */
    public static Any ofType(TypeName type, ByteString value) {
        String typeUrl = type.toTypeUrl();
        final Any result = Any.newBuilder()
                .setValue(value)
                .setTypeUrl(typeUrl)
                .build();
        return result;
    }

    /**
     * Unwraps {@link Any} instance object to {@link Message}
     * that was put inside it and returns as the instance of object
     * with described by {@link Any#getTypeUrl()}.
     * <p>
     * NOTE: This is temporary solution and should be reworked in the future
     * when Protobuf provides means for working with {@link Any}.
     *
     * @param any instance of {@link Any} that should be unwrapped
     * @param <T> descendant of {@link Message} class that is used as the return type for this method
     * @return unwrapped instance of {@link Message} descendant that were put inside of given {@link Any} object
     */
    @SuppressWarnings("ProhibitedExceptionThrown")
    public static <T extends Message> T fromAny(Any any) {
        checkNotNull(any);

        T result = null;

        TypeName typeName = TypeName.of("");
//        String messageClassName = StringTypeValue.NULL;
        try {
            typeName = TypeName.ofEnclosed(any);
            Class<T> messageClass = toMessageClass(typeName);

            result = any.unpack(messageClass);

//            messageClassName = messageClass.getName();
//            Method method = messageClass.getMethod(METHOD_PARSE_FROM, ByteString.class);
//
//            //noinspection unchecked
//            T result = (T) method.invoke(null, any.getValue());
        } catch (ClassNotFoundException ignored) {
            throw new UnknownTypeInAnyException(typeName.toString());
//        } catch (NoSuchMethodException e) {
//            String msg = String.format(MSG_NO_SUCH_METHOD, METHOD_PARSE_FROM, messageClassName);
//            throw new Error(msg, e);
//        } catch (IllegalAccessException e) {
//            String msg = String.format(MSG_UNABLE_TO_ACCESS, METHOD_PARSE_FROM, messageClassName);
//            throw new Error(msg, e);
//        } catch (InvocationTargetException e) {
//            String msg = String.format(MSG_ERROR_INVOKING, METHOD_PARSE_FROM, messageClassName, e.getCause());
//            throw new Error(msg, e);
        } catch (InvalidProtocolBufferException e) {
            propagate(e);
        }
        return result;
    }

    /**
     * Returns message {@link Class} for the given Protobuf message type.
     * <p>
     * This method is temporary until full support of {@link Any} is provided.
     *
     * @param messageType full type name defined in the proto files
     * @return message class
     * @throws ClassNotFoundException in case there is no corresponding class for the given Protobuf message type
     * @see #fromAny(Any) that uses the same convention
     */
    public static <T extends Message> Class<T> toMessageClass(TypeName messageType) throws ClassNotFoundException {
        ClassName className = TypeToClassMap.get(messageType);
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

    private enum JsonPrinter {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final JsonFormat.Printer value = JsonFormat.printer().usingTypeRegistry(forKnownTypes());

        private static JsonFormat.TypeRegistry forKnownTypes() {
            final JsonFormat.TypeRegistry.Builder builder = JsonFormat.TypeRegistry.newBuilder();
            for (TypeName typeName : TypeToClassMap.knownTypes()) {
                try {
                    Class<? extends Message> clazz = toMessageClass(typeName);
                    Descriptors.GenericDescriptor descriptor = getClassDescriptor(clazz);
                    // Skip outer class descriptors.
                    if (descriptor instanceof Descriptors.Descriptor) {
                        Descriptors.Descriptor typeDescriptor = (Descriptors.Descriptor) descriptor;
                        builder.add(typeDescriptor);
                    }

                } catch (ClassNotFoundException e) {
                    propagate(e);
                }
            }
            return builder.build();
        }

        private static com.google.protobuf.util.JsonFormat.Printer instance() {
            return INSTANCE.value;
        }
    }

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
}
