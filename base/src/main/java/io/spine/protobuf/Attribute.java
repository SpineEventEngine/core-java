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

package io.spine.protobuf;

import com.google.common.base.Optional;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.util.NamedProperty;

import javax.annotation.Nullable;
import java.util.Map;

import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * An attribute stored in a protobuf {@code map<string, Any>}.
 *
 * @param <T> the type of the attribute value, which can be {@code Integer}, {@code Long},
 *            {@code Float}, {@code Double}, {@code Double}, or a class implementing {@code Message}
 * @param <M> the type of the message object to which the attribute belongs
 * @param <B> the type of the message builder
 *
 * @author Alexander Yevsyukov
 */
public abstract class Attribute<T, M extends Message, B extends Message.Builder>
        extends NamedProperty<T, M> {

    @Nullable
    private volatile Type type;

    /**
     * Creates a new instance with the passed name.
     *
     * @param name the key in the attribute map
     */
    protected Attribute(String name) {
        super(name);
    }

    /**
     * Obtains attribute map from the enclosing object.
     */
    protected abstract Map<String, Any> getMap(M obj);

    protected abstract Map<String, Any> getMutableMap(B builder);

    protected Type getType() {
        if (type == null) {
            final Class<T> cls = getValueClass();
            type = Type.getType(cls);
        }
        return type;
    }

    private T unpack(Any any) {
        final T result = getType().unpack(any);
        return result;
    }

    private Any pack(T value) {
        final Any result = getType().pack(value);
        return result;
    }

    /**
     * Returns the attribute value or {@code Optional.absent()} if the attribute is not set.
     */
    @Override
    public final Optional<T> getValue(M obj) {
        final Map<String, Any> map = getMap(obj);
        final Optional<T> result = getFromMap(map);
        return result;
    }

    private Optional<T> getFromMap(Map<String, Any> map) {
        final Any any = map.get(getName());
        if (any == null || Any.getDefaultInstance()
                              .equals(any)) {
            return Optional.absent();
        }

        final T result = unpack(any);
        return Optional.of(result);
    }

    /**
     * Sets the value of the attribute in the passed builder.
     */
    public final void setValue(B builder, T value) {
        final Map<String, Any> map = getMutableMap(builder);
        final Any packed = this.pack(value);
        map.put(getName(), packed);
    }

    @SuppressWarnings("unchecked") // Conversions are safe as we unpack specific types.
    private enum Type {
        BOOLEAN {
            @Override
            <T> Any pack(T value) {
                return Wrapper.forBoolean()
                              .pack((Boolean) value);
            }

            @Override
            <T> boolean matchClass(Class<T> valueClass) {
                return Boolean.class.equals(valueClass);
            }

            @Override
            Boolean unpack(Any any) {
                return Wrapper.forBoolean()
                              .unpack(any);
            }
        },

        STRING {
            @Override
            <T> Any pack(T value) {
                return Wrapper.forString()
                              .pack((String) value);
            }

            @Override
            <T> boolean matchClass(Class<T> valueClass) {
                return String.class.equals(valueClass);
            }

            @Override
            String unpack(Any any) {
                return Wrapper.forString()
                              .unpack(any);
            }
        },

        INTEGER {
            @Override
            Integer unpack(Any any) {
                return Wrapper.forInteger()
                              .unpack(any);
            }

            @Override
            <T> Any pack(T value) {
                return Wrapper.forInteger()
                              .pack((Integer) value);
            }

            @Override
            <T> boolean matchClass(Class<T> valueClass) {
                return Integer.class.equals(valueClass);
            }
        },

        LONG {
            @Override
            Long unpack(Any any) {
                return Wrapper.forLong()
                              .unpack(any);
            }

            @Override
            <T> Any pack(T value) {
                return Wrapper.forLong()
                              .pack((Long) value);
            }

            @Override
            <T> boolean matchClass(Class<T> valueClass) {
                return Long.class.equals(valueClass);
            }
        },

        FLOAT {
            @Override
            Float unpack(Any any) {
                return Wrapper.forFloat()
                              .unpack(any);
            }

            @Override
            <T> Any pack(T value) {
                return Wrapper.forFloat()
                              .pack((Float) value);
            }

            @Override
            <T> boolean matchClass(Class<T> valueClass) {
                return Float.class.equals(valueClass);
            }
        },

        DOUBLE {
            @Override
            Double unpack(Any any) {
                return Wrapper.forDouble()
                              .unpack(any);
            }

            @Override
            <T> Any pack(T value) {
                return Wrapper.forDouble()
                              .pack((Double) value);
            }

            @Override
            <T> boolean matchClass(Class<T> valueClass) {
                return Double.class.equals(valueClass);
            }
        },

        MESSAGE {
            @Override
            <T> T unpack(Any any) {
                final T result = AnyPacker.unpack(any);
                return result;
            }

            @Override
            <T> Any pack(T value) {
                return AnyPacker.pack((Message) value);
            }

            @Override
            <T> boolean matchClass(Class<T> valueClass) {
                return Message.class.isAssignableFrom(valueClass);
            }
        };

        static <T> Type getType(Class<T> valueClass) {
            for (Type type : values()) {
                if (type.matchClass(valueClass)) {
                    return type;
                }
            }
            throw unsupportedClass(valueClass);
        }

        abstract <T> T unpack(Any any);

        abstract <T> Any pack(T value);

        abstract <T> boolean matchClass(Class<T> valueClass);

        private static <I> IllegalArgumentException unsupportedClass(Class<I> idClass) {
            return newIllegalArgumentException("Unsupported attribute class encountered: %s",
                                                          idClass.getName());
        }
    }
}
