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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import io.spine.protobuf.AnyPacker;
import io.spine.validate.Validate;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
     * <p>The list contains serializable messages.
     */
    private final List<GeneratedMessageV3> values;

    /**
     * Creates a new instance with the passed values.
     *
     * <p>The constructor checks the type of passed messages. In an unlikely case of a message
     * not being a {@link GeneratedMessageV3} (e.g. {@link com.google.protobuf.DynamicMessage
     * DynamicMessage} it is packed into {@link com.google.protobuf.Any Any}.
     */
    protected Tuple(Message... values) {
        super();

        final ImmutableList.Builder<GeneratedMessageV3> builder = ImmutableList.builder();
        boolean nonEmptyFound = false;
        for (Message value : values) {
            checkNotNull(value);
            final boolean isEmpty = checkNotDefaultOrEmpty(value);
            if (!isEmpty) {
                nonEmptyFound = true;
            }

            GeneratedMessageV3 valueToPut;
            valueToPut = value instanceof GeneratedMessageV3
                         ? (GeneratedMessageV3) value
                         : AnyPacker.pack(value);

            builder.add(valueToPut);
        }
        checkArgument(nonEmptyFound, "Tuple cannot be all Empty");

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

    @Nonnull
    @Override
    public final Iterator<Message> iterator() {
        final Iterator<Message> result = new UnpackingIterator(values);
        return result;
    }

    /**
     * Obtains a value at the specified index.
     *
     * @param  index a zero-based index value
     * @return the value at the index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    protected Message get(int index) {
        final Message result = values.get(index);
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {return true;}
        if (obj == null || getClass() != obj.getClass()) {return false;}
        final Tuple other = (Tuple) obj;
        return Objects.equals(this.values, other.values);
    }

    /**
     * Unpacks {@link Any} if encounters it during iteration.
     *
     * <p>The iterator verifies if a next message to be returned is {@link Any}.
     * If so, returns enclosed value. Otherwise returns the message instance.
     */
    private static class UnpackingIterator extends UnmodifiableIterator<Message> {

        private final Iterator<GeneratedMessageV3> source;

        private UnpackingIterator(Iterable<GeneratedMessageV3> source) {
            this.source = source.iterator();
        }

        @Override
        public boolean hasNext() {
            return source.hasNext();
        }

        @Override
        public Message next() {
            final GeneratedMessageV3 next = source.next();
            final Message result;
            if (next instanceof Any) {
                Any any = (Any) next;
                result = AnyPacker.unpack(any);
            } else {
                result = next;
            }
            return result;
        }
    }

    /*
     * Interfaces for obtaining tuple values.
     *****************************************/

    interface AValue<T extends Message> {
        T getA();
    }

    interface BValue<T extends Message> {
        T getB();
    }
}
