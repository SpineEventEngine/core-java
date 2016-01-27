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

package org.spine3.type;

import com.google.common.base.Joiner;
import com.google.protobuf.Message;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A base class for value objects storing references to message classes.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods") // is OK for value object base.
// NOTE: this class is named using 'Type' infix to prevent the name clash with java.lang.ClassValue.
public abstract class ClassTypeValue {

    private final Class<? extends Message> value;

    protected ClassTypeValue(Class<? extends Message> value) {
        this.value = value;
    }

    /**
     * @return value of the object
     */
    public Class<? extends Message> value() {
        return this.value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ClassTypeValue other = (ClassTypeValue) obj;
        return Objects.equals(this.value, other.value);
    }

    /**
     * Ensures that the passed set of classes is empty.
     *
     * <p>This is a convenience method for checking registration of handling dispatching.
     *
     * @param alreadyRegistered the set of already registered classes or an empty set
     * @param registeringObject the object which tries to register dispatching or handling
     * @param singularFormat the message format for the case if the {@code alreadyRegistered} set contains only one element
     * @param pluralFormat the message format if {@code alreadyRegistered} set has more than one element
     * @throws IllegalArgumentException if the set is not empty
     */
    public static void checkNotAlreadyRegistered(Set<? extends ClassTypeValue> alreadyRegistered, Object registeringObject,
                                                 String singularFormat, String pluralFormat) {
        final String format = alreadyRegistered.size() > 1 ? pluralFormat : singularFormat;
        checkArgument(alreadyRegistered.isEmpty(), format, registeringObject, Joiner.on(", ").join(alreadyRegistered));
    }
}
