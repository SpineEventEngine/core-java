/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.util;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import org.spine3.lang.UnknownTypeInAnyException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with {@link Message} objects.
 *
 * @author Mikhail Melnik
 */
@SuppressWarnings("UtilityClass")
public class Messages {

    public static final String PARSE_FROM_METHOD_NAME = "parseFrom";

    public static final String UNEXISTING_METHOD_CALL = "There is no such method for the given object: ";
    public static final String INACCESSIBLE_METHOD = "Method became inaccessible: ";

    private static final String COM_PREFIX = "com.";

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

        Any result = Any.newBuilder()
                .setTypeUrl(message.getDescriptorForType().getFullName())
                .setValue(message.toByteString())
                .build();

        return result;
    }

    /**
     * Unwraps {@link Any} instance object to {@link Message}
     * that was put inside it and returns as the instance of object
     * with described by {@link Any#getTypeUrl()}.
     * <p/>
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

        try {
            Class<T> messageType = toMessageClass(any.getTypeUrl());
            Method method = messageType.getMethod(PARSE_FROM_METHOD_NAME, ByteString.class);

            //noinspection unchecked
            T result = (T) method.invoke(null, any.getValue());
            return result;
        } catch (ClassNotFoundException ignored) {
            throw new UnknownTypeInAnyException(any.getTypeUrl());
        } catch (NoSuchMethodException e) {
            throw new Error(UNEXISTING_METHOD_CALL + PARSE_FROM_METHOD_NAME, e);
        } catch (IllegalAccessException e) {
            throw new Error(INACCESSIBLE_METHOD + PARSE_FROM_METHOD_NAME, e);
        } catch (InvocationTargetException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new Error(e.getCause());
        }
    }

    //TODO:2015-06-10:alexander.yevsyukov: This is fragile. Fix it to have some kind of translator between
    // type name and Java class name. Do not store a map String->Java class, as it would initialize all the
    // required classes. Store a map string->string.

    /**
     * Returns message {@link Class} for the given Protobuf message type.
     * <p/>
     * NOTE: By convention classname is the Protobuf message type name prefixed with '.com'.
     * This method is temporary until full support of {@link Any} is provided.
     *
     * @param messageType full type name defined in the proto files
     * @return message class
     * @throws ClassNotFoundException in case there is no corresponding class for the given Protobuf message type
     * @see #fromAny(Any) that uses the same convention
     */
    public static <T extends Message> Class<T> toMessageClass(String messageType) throws ClassNotFoundException {
        String className = COM_PREFIX + messageType;
        //noinspection unchecked
        return (Class<T>) Class.forName(className);
    }

    /**
     * Prints the passed message into to string.
     *
     * @param message the message object
     * @return string representation of the passed message
     */
    public static String toString(Message message) {
        checkNotNull(message);

        //TODO:2015-06-22:mikhail.melnik: implement
        return JsonFormat.printToString(message);
    }

    /**
     * Prints the passed message into well formatted text.
     *
     * @param message the message object
     * @return text representation of the passed message
     */
    public static String toText(Message message) {
        checkNotNull(message);

        return TextFormat.printToString(message);
    }

    /**
     * Converts passed message into Json representation.
     *
     * @param message the message object
     * @return Json string
     */
    public static String toJson(Message message) {
        checkNotNull(message);

        return JsonFormat.printToString(message);
    }

    private Messages() {
    }

}
