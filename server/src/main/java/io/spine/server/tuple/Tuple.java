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

package io.spine.server.tuple;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Abstract base for tuple classes.
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
    private final List<Element> values;

    /**
     * Creates a new instance with the passed values.
     */
    protected Tuple(Object... values) {
        super();

        ImmutableList.Builder<Element> builder = ImmutableList.builder();
        for (Object value : values) {
            checkNotNull(value);
            Element element = new Element(value);
            builder.add(element);
        }

        this.values = builder.build();
    }

    /**
     * Ensures that the passed message is not an instance of {@link Empty}.
     *
     * @return the passed value
     * @throws IllegalArgumentException if the passed value is {@link Empty}
     */
    @CanIgnoreReturnValue
    static @Nullable <M extends Message, T extends Tuple>
    M checkNotEmpty(Class<T> checkingClass, @Nullable M value) {
        if (value == null) {
            return null;
        }
        boolean isEmpty = value instanceof Empty;
        if (isEmpty) {
            String shortClassName = checkingClass.getSimpleName();
            throw newIllegalArgumentException(
                    "`%s` cannot have `Empty` elements. Use `Optional` instead",
                    shortClassName);
        }
        return value;
    }

    @CanIgnoreReturnValue
    static <M extends Message, T extends Tuple>
    M checkNotNullOrEmpty(Class<T> checkingClass, M value) {
        checkNotNull(value);
        M result = checkNotEmpty(checkingClass, value);
        return result;
    }

    static <T extends Tuple>
    void checkAllNotNullOrEmpty(Class<T> checkingClass, Message... values) {
        for (Message value : values) {
            checkNotNullOrEmpty(checkingClass, value);
        }
    }

    static <T extends Tuple>
    void checkAllNotEmpty(Class<T> checkingClass, Message... values) {
        for (Message value : values) {
            checkNotEmpty(checkingClass, value);
        }
    }

    @Override
    public final @NonNull Iterator<Message> iterator() {
        Iterator<Message> result = new ExtractingIterator(values);
        return result;
    }

    /**
     * Obtains a value at the specified index.
     *
     * @param index a zero-based index value
     * @return the value at the index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    protected final Object get(int index) {
        Element element = values.get(index);
        Object result = element.getValue();
        return result;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Tuple)) {
            return false;
        }
        Tuple other = (Tuple) obj;
        return Objects.equals(this.values, other.values);
    }

    /**
     * Traverses through elements obtaining a message value from them.
     */
    private static final class ExtractingIterator extends UnmodifiableIterator<Message> {

        private final Iterator<Element> source;

        private ExtractingIterator(Iterable<Element> source) {
            super();
            this.source = source.iterator();
        }

        @Override
        public boolean hasNext() {
            return source.hasNext();
        }

        @Override
        public Message next() {
            Element next = source.next();
            Message result = next.getMessage();
            return result;
        }
    }
}
