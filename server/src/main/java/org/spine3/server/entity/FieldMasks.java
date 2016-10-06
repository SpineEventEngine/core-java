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

package org.spine3.server.entity;

import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;
import org.spine3.protobuf.KnownTypes;
import org.spine3.protobuf.TypeUrl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A utility class for {@code FieldMask} processing against instances of {@link Message}.
 *
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("UtilityClass")
public class FieldMasks {

    private FieldMasks() {
    }

    /**
     * Applies the given {@code FieldMask} to given collection of {@link Message}s.
     * Does not change the {@link Collection} itself.
     *
     * <p>In case the {@code FieldMask} instance contains invalid field declarations, they are ignored and
     * do not affect the execution result.
     *
     * @param mask     {@code FieldMask} to apply to each item of the input {@link Collection}.
     * @param messages {@link Message}s to filter.
     * @param type     Type of the {@link Message}s.
     * @return messages with the {@code FieldMask} applied
     */
    @Nonnull
    public static <M extends Message, B extends Message.Builder> Collection<M> applyMask(
            FieldMask mask,
            Collection<M> messages,
            TypeUrl type) {
        final List<M> filtered = new LinkedList<>();
        final ProtocolStringList filter = mask.getPathsList();
        final Class<B> builderClass = getBuilderForType(type);

        if (filter.isEmpty() || builderClass == null) {
            return Collections.unmodifiableCollection(messages);
        }

        try {
            final Constructor<B> builderConstructor = builderClass.getDeclaredConstructor();
            builderConstructor.setAccessible(true);

            for (Message wholeMessage : messages) {
                final M message = messageForFilter(filter, builderConstructor, wholeMessage);
                filtered.add(message);
            }
        } catch (NoSuchMethodException |
                InvocationTargetException |
                IllegalAccessException |
                InstantiationException ignored) {
            // If any reflection failure happens, return all the data without any mask applied.
            return Collections.unmodifiableCollection(messages);
        }
        return Collections.unmodifiableList(filtered);
    }

    /**
     * Applies the given {@code FieldMask} to a single {@link Message}.
     *
     * <p>In case the {@code FieldMask} instance contains invalid field declarations, they are ignored and
     * do not affect the execution result.
     *
     * @param mask    {@code FieldMask} instance to apply.
     * @param message The {@link Message} to apply given {@code FieldMask} to.
     * @param type    Type of the {@link Message}.
     * @return A {@link Message} of the same type as the given one with only selected fields.
     */
    public static <M extends Message, B extends Message.Builder> M applyMask(FieldMask mask, M message, TypeUrl type) {
        final ProtocolStringList filter = mask.getPathsList();
        final Class<B> builderClass = getBuilderForType(type);

        if (filter.isEmpty() || builderClass == null) {
            return message;
        }

        try {
            final Constructor<B> builderConstructor = builderClass.getDeclaredConstructor();
            builderConstructor.setAccessible(true);

            final M result = messageForFilter(filter, builderConstructor, message);
            return result;

        } catch (NoSuchMethodException |
                InvocationTargetException |
                IllegalAccessException |
                InstantiationException ignored) {
            return message;
        }
    }

    /**
     * Applies the {@code FieldMask} to the given {@link Message} the {@code mask} parameter is valid.
     *
     * <p>In case the {@code FieldMask} instance contains invalid field declarations, they are ignored and
     * do not affect the execution result.
     *
     * @param mask    The {@code FieldMask} to apply.
     * @param message The {@link Message} to apply given mask to.
     * @param typeUrl Type of given {@link Message}.
     * @return A {@link Message} of the same type as the given one with only selected fields
     * if the {@code mask} is valid, {@code message} itself otherwise.
     */
    public static <M extends Message> M applyIfValid(FieldMask mask, M message, TypeUrl typeUrl) {
        if (!mask.getPathsList()
                 .isEmpty()) {
            return applyMask(mask, message, typeUrl);
        }
        return message;
    }

    private static <M extends Message, B extends Message.Builder> M messageForFilter(
            ProtocolStringList filter,
            Constructor<B> builderConstructor, Message wholeMessage)
            throws InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        final B builder = builderConstructor.newInstance();

        final List<Descriptors.FieldDescriptor> fields = wholeMessage.getDescriptorForType()
                                                                     .getFields();
        for (Descriptors.FieldDescriptor field : fields) {
            if (filter.contains(field.getFullName())) {
                builder.setField(field, wholeMessage.getField(field));
            }
        }
        @SuppressWarnings("unchecked")       // It's fine as the constructor is of {@code MessageCls.Builder} type.
        final M result = (M) builder.build();
        return result;
    }

    @Nullable
    private static <B extends Message.Builder> Class<B> getBuilderForType(TypeUrl typeUrl) {
        try {
            final String className = KnownTypes.getClassName(typeUrl)
                                               .value();
            final Class<?> builderClazz = Class.forName(className)
                                               .getClasses()[0];
            // Assuming {@code KnownTypes#getClassName(TypeUrl)} works properly.
            @SuppressWarnings("unchecked")
            Class<B> result = (Class<B>) builderClazz;
            return result;
        } catch (ClassNotFoundException | ClassCastException ignored) {
            return null;
        }
    }
}
