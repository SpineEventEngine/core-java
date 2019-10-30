/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.entity.storage;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * The basic implementation of mapping rules for entity {@linkplain Column columns}.
 *
 * <p>Since entity columns are proto-based and have a limited amount of possible types, this class
 * allows descendants to override concrete type mapping rules in a convenient way.
 */
public abstract class AbstractColumnMapping<R> implements ColumnMapping<R> {

    /**
     * The mapping rules of built-in proto types.
     */
    private final
    ImmutableMap<Class<?>, Supplier<ColumnTypeMapping<?, ? extends R>>> standardTypesMapping
            = standardTypesMapping();

    /**
     * The mapping rules for custom user-defined types.
     *
     * @see #setupCustomMapping(ImmutableMap.Builder)
     */
    private @MonotonicNonNull
    ImmutableMap<Class<?>, ColumnTypeMapping<?, ? extends R>> customMapping;

    @SuppressWarnings("unchecked") // Ensured by mapping declaration.
    @Override
    public <T> ColumnTypeMapping<T, ? extends R> of(Class<T> type) {
        checkNotNull(type);
        ColumnTypeMapping<?, ? extends R> result;
        Optional<ColumnTypeMapping<?, ? extends R>> rule = customMappingFor(type);
        if (rule.isPresent()) {
            result = rule.get();
        } else {
            rule = standardMappingFor(type);
            result = rule.orElseThrow(() -> unsupportedType(type));
        }
        return (ColumnTypeMapping<T, ? extends R>) result;
    }

    /**
     * Allows to specify custom mapping rules.
     *
     * <p>If some message types are needed to be stored differently to the generic
     * {@linkplain Message messages}, the rules for their storage can be specified using this
     * method.
     *
     * <p>The common examples of such messages are {@link com.google.protobuf.Timestamp Timestamp}
     * and {@link io.spine.core.Version Version}.
     *
     * <p>The custom mapping can also be specified for marker interfaces of messages like
     * {@link io.spine.base.EventMessage}.
     */
    @SuppressWarnings("NoopMethodInAbstractClass") // Do not enforce implementation in descendants.
    protected void
    setupCustomMapping(ImmutableMap.Builder<Class<?>, ColumnTypeMapping<?, ? extends R>> builder) {
        // NO-OP by default.
    }

    /**
     * Returns the mapping rules of {@code String} columns.
     */
    protected abstract ColumnTypeMapping<String, ? extends R> ofString();

    /**
     * Returns the mapping rules of {@code Integer} columns.
     */
    protected abstract ColumnTypeMapping<Integer, ? extends R> ofInteger();

    /**
     * Returns the mapping rules of {@code Long} columns.
     */
    protected abstract ColumnTypeMapping<Long, ? extends R> ofLong();

    /**
     * Returns the mapping rules of {@code Float} columns.
     */
    protected abstract ColumnTypeMapping<Float, ? extends R> ofFloat();

    /**
     * Returns the mapping rules of {@code Double} columns.
     */
    protected abstract ColumnTypeMapping<Double, ? extends R> ofDouble();

    /**
     * Returns the mapping rules of {@code Boolean} columns.
     */
    protected abstract ColumnTypeMapping<Boolean, ? extends R> ofBoolean();

    /**
     * Returns the mapping rules of {@code ByteString} columns.
     */
    protected abstract ColumnTypeMapping<ByteString, ? extends R> ofByteString();

    /**
     * Returns the mapping rules of {@code Enum} columns.
     */
    protected abstract ColumnTypeMapping<Enum<?>, ? extends R> ofEnum();

    /**
     * Returns the mapping rules of {@code Message} columns.
     */
    protected abstract ColumnTypeMapping<Message, ? extends R> ofMessage();

    /**
     * Throws an exception about an unsupported column type.
     */
    protected IllegalArgumentException unsupportedType(Class<?> aClass) {
        throw newIllegalArgumentException(
                "The columns of type `%s` are not supported by the column mapping.",
                aClass.getCanonicalName());
    }

    private ImmutableMap<Class<?>, Supplier<ColumnTypeMapping<?, ? extends R>>>
    standardTypesMapping() {
        ImmutableMap.Builder<Class<?>, Supplier<ColumnTypeMapping<?, ? extends R>>> builder =
                ImmutableMap.builder();

        builder.put(String.class, this::ofString);

        builder.put(int.class, this::ofInteger);
        builder.put(Integer.class, this::ofInteger);

        builder.put(long.class, this::ofLong);
        builder.put(Long.class, this::ofLong);

        builder.put(float.class, this::ofFloat);
        builder.put(Float.class, this::ofFloat);

        builder.put(double.class, this::ofDouble);
        builder.put(Double.class, this::ofDouble);

        builder.put(boolean.class, this::ofBoolean);
        builder.put(Boolean.class, this::ofBoolean);

        builder.put(ByteString.class, this::ofByteString);

        builder.put(Enum.class, this::ofEnum);

        builder.put(Message.class, this::ofMessage);

        return builder.build();
    }

    private ImmutableMap<Class<?>, ColumnTypeMapping<?, ? extends R>>
    customMapping() {
        if (customMapping == null) {
            ImmutableMap.Builder<Class<?>, ColumnTypeMapping<?, ? extends R>> builder =
                    ImmutableMap.builder();

            setupCustomMapping(builder);

            customMapping = builder.build();
        }
        return customMapping;
    }

    private Optional<ColumnTypeMapping<?, ? extends R>> customMappingFor(Class<?> aClass) {
        Optional<ColumnTypeMapping<?, ? extends R>> result =
                customMapping().keySet()
                               .stream()
                               .filter(cls -> cls.isAssignableFrom(aClass))
                               .map(customMapping()::get)
                               .findFirst()
                               .map(rule -> (ColumnTypeMapping<?, ? extends R>) rule);
        return result;
    }

    private Optional<ColumnTypeMapping<?, ? extends R>> standardMappingFor(Class<?> aClass) {
        Optional<ColumnTypeMapping<?, ? extends R>> result =
                standardTypesMapping.keySet()
                                    .stream()
                                    .filter(cls -> cls.isAssignableFrom(aClass))
                                    .map(standardTypesMapping::get)
                                    .findFirst()
                                    .map(Supplier::get);
        return result;
    }
}
