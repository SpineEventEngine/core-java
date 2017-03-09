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

package org.spine3.base;

import com.google.common.base.Objects;

/**
 * A value object, serves as key of the {@code StringifierRegistry} warehouse.
 *
 * <p>It used when is needed to identify object by the few classes.
 *    For example:
 *    <pre>
 *         {@code
 *               class SetStringifier<T> extends Stringifier<Set<T>>{
 *                   ...
 *               }
 *
 *              final Stringifier<Set<CustomType>> setStringifier = new SetStringifier<>();
 *
 *              final StringifierRegistry registry = StringifierRegistry.getInstance();
 *              registry.put(new PluralKey<>(Set.class, CustomType.class), setStringifier)
 *         }
 *    </pre>
 * @author Illia Shepilov
 */
public class PluralKey<A, B> implements RegistryKey {

    private final Class<A> typeClass;
    private final Class<B> genericClass;

    public PluralKey(Class<A> typeClass, Class<B> genericClass) {
        this.typeClass = typeClass;
        this.genericClass = genericClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PluralKey<?, ?> pluralKey = (PluralKey<?, ?>) o;
        return Objects.equal(typeClass, pluralKey.typeClass) &&
               Objects.equal(genericClass, pluralKey.genericClass);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(typeClass, genericClass);
    }
}
