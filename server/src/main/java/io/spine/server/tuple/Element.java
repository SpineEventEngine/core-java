/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import io.spine.validate.Validate;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * An element of a tuple.
 *
 * <p>Can hold either {@link Message}, or {@link Optional} message, or an instance of
 * {@link Either}.
 */
final class Element implements Serializable {

    private static final long serialVersionUID = 0L;

    @SuppressWarnings("NonSerializableFieldInSerializableClass") // possible values are serializable
    private Object value;
    private Type type;

    /**
     * Creates a tuple element with a value which can be {@link GeneratedMessageV3},
     * {@link Optional}, or {@link Either}.
     */
    @SuppressWarnings("ChainOfInstanceofChecks")
    Element(Object value) {
        if (value instanceof Either) {
            this.type = Type.EITHER;
        } else if (value instanceof Optional) {
            this.type = Type.OPTIONAL;
        } else if (value instanceof GeneratedMessageV3) {
            GeneratedMessageV3 messageV3 = (GeneratedMessageV3) value;
            checkNotDefault(messageV3);
            this.type = Type.MESSAGE;
        } else {
            throw newIllegalArgumentException(
                    "Tuple element of unsupported type passed: %s.", value
            );
        }

        this.value = value;
    }

    /**
     * Obtains the value of the element by its index and casts it to the type {@code <T>}.
     */
    @SuppressWarnings("TypeParameterUnusedInFormals") // See Javadoc.
    static <T> T value(Tuple tuple, int index) {
        @SuppressWarnings("unchecked") // The caller is responsible for the correct type.
        T value = (T) tuple.get(index);
        return value;
    }

    /**
     * Ensures that the passed message is not default or is an instance of {@link Empty}.
     */
    private static void checkNotDefault(Message value) {
        String valueClass = value.getClass()
                                 .getName();
        checkArgument(
                Validate.isNotDefault(value),
                "Tuples cannot contain default values. Default value of %s encountered.",
                valueClass);
    }

    Object getValue() {
        return this.value;
    }

    Message getMessage() {
        switch (type) {
            case MESSAGE:
                return (Message) value;
            case EITHER:
                return ((Either) value).getValue();
            case OPTIONAL: {
                Optional optional = (Optional) value;
                Message result = optional.isPresent()
                                 ? (Message) optional.get()
                                 : Empty.getDefaultInstance();
                return result;
            }
            default:
                throw uncoveredType();
        }
    }

    private IllegalStateException uncoveredType() {
        throw newIllegalStateException("Unsupported element type encountered %s", this.type);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(type);
        final Serializable obj;
        if (type != Type.OPTIONAL) {
            obj = (Serializable) value;
            out.writeObject(obj);
        } else /* (type == Type.OPTIONAL) */ {
            Optional<?> optionalValue = (Optional) value;
            obj = (Serializable) optionalValue.orElse(null);
        }
        out.writeObject(obj);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        type = (Type) in.readObject();
        if (type == Type.OPTIONAL) {
            value = Optional.ofNullable(in.readObject());
        }
        if (type != Type.OPTIONAL) {
            value = in.readObject();
        }
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
        // The fields are non-final to support serialization.
        // Otherwise, they are set only in the constructor.
    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }

    @SuppressWarnings("NonFinalFieldReferenceInEquals")
        // The fields are non-final to support serialization.
        // Otherwise, they are set only in the constructor.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Element other = (Element) obj;
        return Objects.equals(this.value, other.value)
                && this.type == other.type;
    }

    private enum Type {
        MESSAGE,
        OPTIONAL,
        EITHER
    }

    interface AValue<T extends Message> {
        /**
         * Obtains the first element of the tuple.
         */
        T getA();
    }

    /**
     * A marker interface for a tuple element which value can be
     * {@link java.util.Optional Optional}.
     */
    interface OptionalValue {
    }

    interface BValue<T> extends OptionalValue {
        /**
         * Obtains the second element of the tuple.
         */
        T getB();
    }

    interface CValue<T> extends OptionalValue {
        /**
         * Obtains the third element of the tuple.
         */
        T getC();
    }

    interface DValue<T> extends OptionalValue {
        /**
         * Obtains the fourth element of the tuple.
         */
        T getD();
    }

    interface EValue<T> extends OptionalValue {
        /**
         * Obtains the fifth element of the tuple.
         */
        T getE();
    }
}
