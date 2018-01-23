/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.tuple;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import io.spine.validate.Validate;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Abstract base for tuple classes.
 *
 * @author Alexander Yevsyukov
 */
public abstract class Tuple implements Iterable<Message>, Serializable {

    private static final long serialVersionUID = 0L;

    /**
     * Immutable list of tuple values.
     *
     * <p>The first entry must be a {@link GeneratedMessageV3}.
     *
     * <p>Other entries can be either {@link GeneratedMessageV3} or {@link Optional}.
     */
    @SuppressWarnings("NonSerializableFieldInSerializableClass") // ensured in constructor
    private final List<Object> values;

    /**
     * Creates a new instance with the passed values.
     *
     * <p>Values must extend {@link GeneratedMessageV3}.
     */
    @SuppressWarnings("ChainOfInstanceofChecks") // Need for supporting optional entries.
    protected Tuple(Object... values) {
        super();

        final ImmutableList.Builder<Object> builder = ImmutableList.builder();
        boolean nonEmptyFound = false;
        for (Object value : values) {
            checkNotNull(value);

            if (value instanceof Optional) {
                Optional optional = (Optional) value;
                if (optional.isPresent()) {
                    nonEmptyFound = true;
                }
            } else if (value instanceof GeneratedMessageV3) {
                GeneratedMessageV3 messageV3 = (GeneratedMessageV3) value;

                final boolean isEmpty = checkNotDefaultOrEmpty(messageV3);
                if (!isEmpty) {
                    nonEmptyFound = true;
                }
            } else {
                throw newIllegalArgumentException(
                        "Unsupported tuple entry encountered: %s.", value
                );
            }

            builder.add(value);
        }

        checkArgument(nonEmptyFound, "A tuple cannot be empty");

        this.values = builder.build();
    }

    /**
     * Ensures that the passed message is not in default or is an instance of {@link Empty}.
     *
     * @return {@code true} if {@link Empty} is passed
     */
    private static boolean checkNotDefaultOrEmpty(Message value) {
        final boolean isEmpty = value instanceof Empty;
        if (!isEmpty) {
            final String valueClass = value.getClass()
                                           .getName();
            checkArgument(
                    Validate.isNotDefault(value),
                    "Tuples cannot contain default values. Default value of %s encountered.",
                    valueClass);
        }
        return isEmpty;
    }

    /**
     * Ensures that the passed message is not {@link Empty}.
     */
    protected static <M extends Message> M checkNotEmpty(M value, String errorMessage) {
        checkArgument(!(value instanceof Empty), errorMessage);
        return value;
    }

    @Nonnull
    @Override
    public final Iterator<Message> iterator() {
        final Iterator<Message> result = new ExtractingIterator(values);
        return result;
    }

    /**
     * Obtains a value at the specified index.
     *
     * @param index a zero-based index value
     * @return the value at the index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    protected Object get(int index) {
        final Object result = values.get(index);
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Tuple other = (Tuple) obj;
        return Objects.equals(this.values, other.values);
    }

    interface AValue<T extends Message> {
        T getA();
    }

    /*
     * Interfaces for obtaining tuple values.
     *****************************************/

    /**
     * A common interface for a tuple element which can be optional.
     *
     * @param <T> either {@link Message} or {@link Optional}.
     */
    @SuppressWarnings("unused")
            // The type is used for documentation purposes.
    interface OptionalElement<T> {
    }

    interface BValue<T> extends OptionalElement<T> {
        T getB();
    }

    /**
     * Polymorphically traverses through objects that can be either {@code Message} or
     * {@code Optional}.
     *
     * <p>If an entry is {@code Message} returns it.
     *
     * <p>If an entry is {@code Optional}, extracts its value, if there is one, and
     * returns {@code Empty} otherwise.
     */
    private static class ExtractingIterator extends UnmodifiableIterator<Message> {

        private final Iterator<Object> source;

        private ExtractingIterator(Iterable<Object> source) {
            super();
            this.source = source.iterator();
        }

        @Override
        public boolean hasNext() {
            return source.hasNext();
        }

        @Override
        public Message next() {
            final Object next = source.next();
            if (next instanceof Optional) {
                Optional optional = (Optional) next;
                Message result = optional.isPresent()
                                 ? (Message) optional.get()
                                 : Empty.getDefaultInstance();
                return result;
            }
            return (Message) next;
        }
    }
}
