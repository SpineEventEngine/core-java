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

import static io.spine.util.Exceptions.newIllegalArgumentException;

public abstract class AbstractTypeRegistry<R> implements TypeRegistry<R> {

    private final
    ImmutableMap<Class<?>, Supplier<PersistenceStrategy<?, ? extends R>>> standardStrategies
            = standardStrategies();

    private @MonotonicNonNull
    ImmutableMap<Class<?>, Supplier<PersistenceStrategy<?, ? extends R>>> customStrategies;


    @Override
    public PersistenceStrategy<?, ? extends R> persistenceStrategyOf(Class<?> type) {
        Optional<PersistenceStrategy<?, ? extends R>> strategy = searchCustom(type);
        if (strategy.isPresent()) {
            return strategy.get();
        }
        strategy = searchStandard(type);
        return strategy.orElseThrow(() -> unsupportedType(type));
    }

    private ImmutableMap<Class<?>, Supplier<PersistenceStrategy<?, ? extends R>>>
    standardStrategies() {
        ImmutableMap.Builder<Class<?>, Supplier<PersistenceStrategy<?, ? extends R>>> builder =
                ImmutableMap.builder();

        builder.put(String.class, this::stringPersistenceStrategy);
        builder.put(Integer.class, this::integerPersistenceStrategy);
        builder.put(Long.class, this::longPersistenceStrategy);
        builder.put(Float.class, this::floatPersistenceStrategy);
        builder.put(Double.class, this::doublePersistenceStrategy);
        builder.put(Boolean.class, this::booleanPersistenceStrategy);
        builder.put(ByteString.class, this::byteStringPersistenceStrategy);
        builder.put(Enum.class, this::enumPersistenceStrategy);
        builder.put(Message.class, this::messagePersistenceStrategy);

        return builder.build();
    }

    private ImmutableMap<Class<?>, Supplier<PersistenceStrategy<?, ? extends R>>>
    customStrategies() {
        if (customStrategies == null) {
            ImmutableMap.Builder<Class<?>, Supplier<PersistenceStrategy<?, ? extends R>>> builder =
                    ImmutableMap.builder();

            setupCustomStrategies(builder);

            customStrategies = builder.build();
        }
        return customStrategies;
    }

    protected void setupCustomStrategies(
            ImmutableMap.Builder<Class<?>, Supplier<PersistenceStrategy<?, ? extends R>>> builder) {
        // NO-OP by default.
    }

    protected abstract PersistenceStrategy<String, ? extends R> stringPersistenceStrategy();

    protected abstract PersistenceStrategy<Integer, ? extends R> integerPersistenceStrategy();

    protected abstract PersistenceStrategy<Long, ? extends R> longPersistenceStrategy();

    protected abstract PersistenceStrategy<Float, ? extends R> floatPersistenceStrategy();

    protected abstract PersistenceStrategy<Double, ? extends R> doublePersistenceStrategy();

    protected abstract PersistenceStrategy<Boolean, ? extends R> booleanPersistenceStrategy();

    protected abstract PersistenceStrategy<ByteString, ? extends R> byteStringPersistenceStrategy();

    protected abstract PersistenceStrategy<Enum<?>, ? extends R> enumPersistenceStrategy();

    protected abstract PersistenceStrategy<Message, ? extends R> messagePersistenceStrategy();

    protected IllegalArgumentException unsupportedType(Class<?> aClass) {
        throw newIllegalArgumentException("The class %s is not supported by the type registry.",
                                          aClass.getCanonicalName());
    }

    private Optional<PersistenceStrategy<?, ? extends R>> searchCustom(Class<?> aClass) {
        Optional<PersistenceStrategy<?, ? extends R>> result =
                strategyOf(aClass, customStrategies());
        return result;
    }

    private Optional<PersistenceStrategy<?, ? extends R>> searchStandard(Class<?> aClass) {
        Optional<PersistenceStrategy<?, ? extends R>> result =
                strategyOf(aClass, standardStrategies);
        return result;
    }

    private Optional<PersistenceStrategy<?, ? extends R>>
    strategyOf(Class<?> aClass,
               ImmutableMap<Class<?>, Supplier<PersistenceStrategy<?, ? extends R>>> strategies) {
        Optional<PersistenceStrategy<?, ? extends R>> result =
                strategies.keySet()
                          .stream()
                          .filter(cls -> cls.isAssignableFrom(aClass))
                          .map(strategies::get)
                          .findFirst()
                          .map(Supplier::get);
        return result;
    }
}
