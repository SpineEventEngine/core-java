/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.util;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The abstract base for classes obtaining a value of a named property.
 *
 * @param <T> the type of the property
 * @param <O> the type of the object from which we get the value
 * @author Alexander Yevsyukov
 */
public abstract class NamedProperty<T, O> {

    private final String name;

    protected NamedProperty(String name) {
        checkNotNull(name);
        checkArgument(name.length() > 0, "Property name cannot be empty");
        this.name = name;
    }

    /**
     * Extracts the property value from the passed object.
     */
    protected abstract Optional<T> getValue(O obj);

    /**
     * Obtains the class of the value.
     */
    protected Class<T> getValueClass() {
        @SuppressWarnings("unchecked") /* The type is ensured by the first generic param
                                          of this class declaration. */
        final Class<T> cls = (Class<T>) GenericParameter.PROPERTY_TYPE.getArgumentIn(getClass());
        return cls;
    }

    /**
     * Obtains the name of the property.
     */
    protected String getName() {
        return name;
    }

    /**
     * Enumeration of generic type parameters of the {@link NamedProperty} class.
     */
    public enum GenericParameter implements GenericTypeIndex<NamedProperty> {

        /**
         * The index of the declaration of the generic parameter {@code <T>}.
         */
        PROPERTY_TYPE(0),

        /**
         * The index of the declaration of the generic parameter {@code <O>}.
         */
        OBJECT_TYPE(1);

        private final int index;

        GenericParameter(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return this.index;
        }

        @Override
        public Class<?> getArgumentIn(Class<? extends NamedProperty> cls) {
            return Default.getArgument(this, cls);
        }
    }
}
