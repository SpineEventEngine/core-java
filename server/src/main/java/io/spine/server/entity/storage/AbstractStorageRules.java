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

public abstract class AbstractStorageRules<R> implements ColumnStorageRules<R> {

    private final
    ImmutableMap<Class<?>, Supplier<ColumnStorageRule<?, ? extends R>>> standardRules
            = standardRules();

    private @MonotonicNonNull
    ImmutableMap<Class<?>, ColumnStorageRule<?, ? extends R>> customRules;

    @Override
    public ColumnStorageRule<?, ? extends R> of(Class<?> type) {
        checkNotNull(type);
        Optional<ColumnStorageRule<?, ? extends R>> rule = customRuleFor(type);
        if (rule.isPresent()) {
            return rule.get();
        }
        rule = standardRuleFor(type);
        return rule.orElseThrow(() -> unsupportedType(type));
    }

    private ImmutableMap<Class<?>, Supplier<ColumnStorageRule<?, ? extends R>>>
    standardRules() {
        ImmutableMap.Builder<Class<?>, Supplier<ColumnStorageRule<?, ? extends R>>> builder =
                ImmutableMap.builder();

        builder.put(String.class, this::ofString);
        builder.put(Integer.class, this::ofInteger);
        builder.put(Long.class, this::ofLong);
        builder.put(Float.class, this::ofFloat);
        builder.put(Double.class, this::ofDouble);
        builder.put(Boolean.class, this::ofBoolean);
        builder.put(ByteString.class, this::ofByteString);
        builder.put(Enum.class, this::ofEnum);
        builder.put(Message.class, this::ofMessage);

        return builder.build();
    }

    private ImmutableMap<Class<?>, ColumnStorageRule<?, ? extends R>>
    customRules() {
        if (customRules == null) {
            ImmutableMap.Builder<Class<?>, ColumnStorageRule<?, ? extends R>> builder =
                    ImmutableMap.builder();

            setupCustomRules(builder);

            customRules = builder.build();
        }
        return customRules;
    }

    @SuppressWarnings("NoopMethodInAbstractClass") // Do not enforce implementation in descendants.
    protected void
    setupCustomRules(ImmutableMap.Builder<Class<?>, ColumnStorageRule<?, ? extends R>> builder) {
        // NO-OP by default.
    }

    protected abstract ColumnStorageRule<String, ? extends R> ofString();

    protected abstract ColumnStorageRule<Integer, ? extends R> ofInteger();

    protected abstract ColumnStorageRule<Long, ? extends R> ofLong();

    protected abstract ColumnStorageRule<Float, ? extends R> ofFloat();

    protected abstract ColumnStorageRule<Double, ? extends R> ofDouble();

    protected abstract ColumnStorageRule<Boolean, ? extends R> ofBoolean();

    protected abstract ColumnStorageRule<ByteString, ? extends R> ofByteString();

    protected abstract ColumnStorageRule<Enum<?>, ? extends R> ofEnum();

    protected abstract ColumnStorageRule<Message, ? extends R> ofMessage();

    protected IllegalArgumentException unsupportedType(Class<?> aClass) {
        throw newIllegalArgumentException(
                "The columns of class `%s` are not supported by the storage rules.",
                aClass.getCanonicalName());
    }

    private Optional<ColumnStorageRule<?, ? extends R>> customRuleFor(Class<?> aClass) {
        Optional<ColumnStorageRule<?, ? extends R>> result =
                customRules().keySet()
                             .stream()
                             .filter(cls -> cls.isAssignableFrom(aClass))
                             .map(customRules()::get)
                             .findFirst()
                             .map(rule -> (ColumnStorageRule<?, ? extends R>) rule);
        return result;
    }

    private Optional<ColumnStorageRule<?, ? extends R>> standardRuleFor(Class<?> aClass) {
        Optional<ColumnStorageRule<?, ? extends R>> result =
                standardRules.keySet()
                             .stream()
                             .filter(cls -> cls.isAssignableFrom(aClass))
                             .map(standardRules::get)
                             .findFirst()
                             .map(Supplier::get);
        return result;
    }
}
