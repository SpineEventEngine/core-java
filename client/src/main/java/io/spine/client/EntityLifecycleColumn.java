/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.client;

import io.spine.annotation.Internal;
import io.spine.query.ColumnName;
import io.spine.query.CustomColumn;
import io.spine.reflect.GenericTypeIndex;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.function.Supplier;

import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings({"Immutable", "MissingSummary"})  //TODO:2021-01-18:alex.tymchenko: document!
@Internal
public abstract class EntityLifecycleColumn<V> extends CustomColumn<Supplier<V>, V> {

    private final ColumnName name;

    private volatile @MonotonicNonNull Class<V> valueType;

    @Override
    @SuppressWarnings("unchecked")   // Ensured by the declaration of the generic parameter.
    public Class<V> type() {
        if (valueType == null) {
            valueType = (Class<V>) GenericParameter.VALUE.argumentIn(getClass());
        }
        return valueType;
    }

    EntityLifecycleColumn(String name) {
        this.name = ColumnName.of(name);
    }

    @Override
    public ColumnName name() {
        return name;
    }

    @Override
    public final V valueIn(Supplier<V> source) {
        throw newIllegalStateException("`EntityLifecycleColumn.valueIn(..)` " +
                                               "must not be called directly.");
    }

    /**
     * Enumeration of generic type parameters of this class.
     */
    @SuppressWarnings("rawtypes")
    private enum GenericParameter implements GenericTypeIndex<EntityLifecycleColumn> {

        /** The index of the generic type {@code <V>}. */
        VALUE(0);

        private final int index;

        GenericParameter(int index) {
            this.index = index;
        }

        @Override
        public int index() {
            return this.index;
        }
    }
}
