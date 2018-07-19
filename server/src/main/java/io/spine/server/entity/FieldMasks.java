/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.entity;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * A utility class for creating instances of {@code FieldMask} and processing them
 * against instances of {@link Message}.
 *
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("UtilityClass")
public class FieldMasks {

    private static final String CONSTRUCTOR_INVOCATION_ERROR_LOGGING_PATTERN =
            "Constructor for type %s could not be found or called: ";

    private static final String TYPE_CAST_ERROR_LOGGING_PATTERN =
            "Class %s must be assignable from com.google.protobuf.Message. " +
            "Try to rebuild the project. Make sure type URL is valid.";

    private FieldMasks() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates a new instance of {@code FieldMask} basing on the target type
     * {@link Descriptor descriptor} and field tags defined in the Protobuf message.
     *
     * @param typeDescriptor {@link Descriptor descriptor} of the type to create a mask for.
     * @param fieldTags      field tags to include into the mask.
     * @return an instance of {@code FieldMask} for the target type with the fields specified.
     */
    public static FieldMask maskOf(Descriptor typeDescriptor, int... fieldTags) {
        if (fieldTags.length == 0) {
            return FieldMask.getDefaultInstance();
        }

        FieldMask.Builder result = FieldMask.newBuilder();
        for (int fieldNumber : fieldTags) {
            Descriptors.FieldDescriptor field = typeDescriptor.findFieldByNumber(fieldNumber);
            String fieldPath = field.getFullName();
            result.addPaths(fieldPath);
        }

        return result.build();
    }

    /**
     * Applies the given {@code FieldMask} to given collection of {@link Message}s.
     * Does not change the {@link Collection} itself.
     *
     * <p>In case the {@code FieldMask} instance contains invalid field declarations, they are
     * ignored and do not affect the execution result.
     *
     * @param mask     {@code FieldMask} to apply to each item of the input {@link Collection}.
     * @param messages {@link Message}s to filter.
     * @param type     type of the {@link Message}s.
     * @return messages with the {@code FieldMask} applied
     */
    @Nonnull
    public static <M extends Message, B extends Message.Builder>
    Collection<M> applyMask(FieldMask mask,
                            Collection<M> messages,
                            TypeUrl type) {
        checkNotNull(mask);
        checkNotNull(messages);
        checkNotNull(type);

        List<M> filtered = new LinkedList<>();
        ProtocolStringList filter = mask.getPathsList();
        Class<B> builderClass = getBuilderForType(type);

        if (filter.isEmpty() || builderClass == null) {
            return Collections.unmodifiableCollection(messages);
        }

        try {
            Constructor<B> builderConstructor = builderClass.getDeclaredConstructor();
            builderConstructor.setAccessible(true);

            for (Message wholeMessage : messages) {
                M message = messageForFilter(filter, builderConstructor, wholeMessage);
                filtered.add(message);
            }
        } catch (NoSuchMethodException |
                InvocationTargetException |
                IllegalAccessException |
                InstantiationException e) {
            // If any reflection failure happens, return all the data without any mask applied.
            log().warn(format(CONSTRUCTOR_INVOCATION_ERROR_LOGGING_PATTERN,
                              builderClass.getCanonicalName()),
                       e);
            return Collections.unmodifiableCollection(messages);
        }
        return Collections.unmodifiableList(filtered);
    }

    /**
     * Applies the {@code FieldMask} to the given {@link Message}
     * if the {@code mask} parameter is valid.
     *
     * <p>In case the {@code FieldMask} instance contains invalid field declarations,
     * they are ignored and do not affect the execution result.
     *
     * @param mask    the {@code FieldMask} to apply.
     * @param message the {@link Message} to apply given mask to.
     * @param typeUrl type of given {@link Message}.
     * @return the message of the same type as the given one with only selected fields if
     * the {@code mask} is valid, original message otherwise.
     */
    public static <M extends Message> M applyMask(FieldMask mask, M message, TypeUrl typeUrl) {
        if (!mask.getPathsList()
                 .isEmpty()) {
            return doApply(mask, message, typeUrl);
        }
        return message;
    }

    private static <M extends Message, B extends Message.Builder> M doApply(FieldMask mask,
                                                                            M message,
                                                                            TypeUrl type) {
        checkNotNull(mask);
        checkNotNull(message);
        checkNotNull(type);

        ProtocolStringList filter = mask.getPathsList();
        Class<B> builderClass = getBuilderForType(type);

        if (builderClass == null) {
            return message;
        }

        try {
            Constructor<B> builderConstructor = builderClass.getDeclaredConstructor();
            builderConstructor.setAccessible(true);

            M result = messageForFilter(filter, builderConstructor, message);
            return result;
        } catch (NoSuchMethodException |
                InvocationTargetException |
                IllegalAccessException |
                InstantiationException e) {
            log().warn(format(CONSTRUCTOR_INVOCATION_ERROR_LOGGING_PATTERN,
                              builderClass.getCanonicalName()),
                       e);
            return message;
        }
    }

    private static <M extends Message, B extends Message.Builder> M messageForFilter(
            ProtocolStringList filter,
            Constructor<B> builderConstructor, Message wholeMessage)
            throws InstantiationException,
                   IllegalAccessException,
                   InvocationTargetException {
        B builder = builderConstructor.newInstance();

        List<Descriptors.FieldDescriptor> fields = wholeMessage.getDescriptorForType()
                                                               .getFields();
        for (Descriptors.FieldDescriptor field : fields) {
            if (filter.contains(field.getFullName())) {
                builder.setField(field, wholeMessage.getField(field));
            }
        }
        @SuppressWarnings("unchecked")
        // It's fine as the constructor is of {@code MessageCls.Builder} type.
        M result = (M) builder.build();
        return result;
    }

    @SuppressWarnings("unchecked") // We assume that TypeUrl.getMessageClass() works properly.
    private static @Nullable <B extends Message.Builder> Class<B>
    getBuilderForType(TypeUrl typeUrl) {
        Class<? extends Message> msgClass = typeUrl.getMessageClass();
        Class<B> builderClass;
        try {
            builderClass = (Class<B>) msgClass.getClasses()[0];
        } catch (ClassCastException e) {
            String message = format(TYPE_CAST_ERROR_LOGGING_PATTERN, msgClass.getCanonicalName());
            log().warn(message, e);
            builderClass = null;
        }

        return builderClass;
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FieldMasks.class);
    }
}
