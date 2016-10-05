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
     * <p>Applies the given {@code FieldMask} to given collection of {@link Message}s.
     * Does not change the {@link Collection} itself.
     *
     * <p>n case the {@code FieldMask} instance contains invalid field declarations, they are ignored and
     * do not affect the execution result.
     *
     * @param mask     {@code FieldMask} to apply to each item of the input {@link Collection}.
     * @param messages {@link Message}s to filter.
     * @param typeUrl  Type of the {@link Message}s.
     * @return {@link Nonnull} unmodifiable {@link Collection<M>} containing {@link Message}s with {@code FieldMask} applied.
     */
    @Nonnull
    @SuppressWarnings({"MethodWithMultipleLoops", "unchecked"})
    public static <M extends  Message, B extends Message.Builder> Collection<M> applyMask(@SuppressWarnings("TypeMayBeWeakened") FieldMask mask, Collection<M> messages, TypeUrl typeUrl) {
        final List<M> filtered = new LinkedList<>();
        final ProtocolStringList filter = mask.getPathsList();

        final Class<B> builderClass = getBuilderForType(typeUrl);

        if (filter.isEmpty() || builderClass == null) {
            return Collections.unmodifiableCollection(messages);
        }

        try {
            final Constructor<B> builderConstructor = builderClass.getDeclaredConstructor();
            builderConstructor.setAccessible(true);

            for (Message wholeMessage : messages) {
                final B builder = builderConstructor.newInstance();

                for (Descriptors.FieldDescriptor field : wholeMessage.getDescriptorForType().getFields()) {
                    if (filter.contains(field.getFullName())) {
                        builder.setField(field, wholeMessage.getField(field));
                    }
                }

                filtered.add((M) builder.build());
            }

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            // If any reflection failure happens, return all the data without any mask applied.
            return Collections.unmodifiableCollection(messages);
        }

        return Collections.unmodifiableList(filtered);
    }

    /**
     * <p>Applies the given {@code FieldMask} to a single {@link Message}.
     **
     * <p>In case the {@code FieldMask} instance contains invalid field declarations, they are ignored and
     * do not affect the execution result.
     *
     * @param mask {@code FieldMask} instance to apply.
     * @param message The {@link Message} to apply given {@code FieldMask} to.
     * @param typeUrl Type of the {@link Message}.
     * @return A {@link Message} of the same type as the given one with only selected fields.
     */
    @SuppressWarnings("unchecked")
    public static <M extends  Message, B extends Message.Builder> M applyMask(@SuppressWarnings("TypeMayBeWeakened") FieldMask mask, M message, TypeUrl typeUrl) {
        final ProtocolStringList filter = mask.getPathsList();

        final Class<B> builderClass = getBuilderForType(typeUrl);

        if (filter.isEmpty() || builderClass == null) {
            return message;
        }

        try {
            final Constructor<B> builderConstructor = builderClass.getDeclaredConstructor();
            builderConstructor.setAccessible(true);

            final B builder = builderConstructor.newInstance();

            for (Descriptors.FieldDescriptor field : message.getDescriptorForType().getFields()) {
                if (filter.contains(field.getFullName())) {
                    builder.setField(field, message.getField(field));
                }
            }

            return (M) builder.build();

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            return message;
        }


    }

    /**
     * <p>Applies the {@code FieldMask} to the given {@link Message} the {@code mask} parameter is valid.
     *
     * <p>In case the {@code FieldMask} instance contains invalid field declarations, they are ignored and
     * do not affect the execution result.
     *
     * @param mask    The {@code FieldMask} to apply.
     * @param message  The {@link Message} to apply given mask to.
     * @param typeUrl Type of given {@link Message}.
     * @return A {@link Message} of the same type as the given one with only selected fields
     *          if the {@code mask} is valid, {@code message} itself otherwise.
     */
    public static <M extends  Message> M applyIfValid(@SuppressWarnings("TypeMayBeWeakened") FieldMask mask, M message, TypeUrl typeUrl) {
        if (!mask.getPathsList().isEmpty()) {
            return applyMask(mask, message, typeUrl);
        }

        return message;
    }

    @Nullable
    private static <B extends Message.Builder> Class<B> getBuilderForType(TypeUrl typeUrl) {
        Class<B> builderClass;
        try {
            //noinspection unchecked
            builderClass = (Class<B>) Class.forName(KnownTypes.getClassName(typeUrl).value())
                                           .getClasses()[0];
        } catch (ClassNotFoundException | ClassCastException e) {
            builderClass = null;
        }

        return builderClass;

    }
}
