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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Messages;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Stringifiers.EMPTY_ID;
import static org.spine3.protobuf.Values.newStringValue;

/**
 * Wrapper of an identifier value.
 *
 * @author Alexander Yevsyukov
 */
class Identifier<I> {

    private final Type type;
    private final I value;

    static <I> Identifier<I> from(I value) {
        checkNotNull(value);
        final Type type = Type.getType(value);
        final Identifier<I> result = create(type, value);
        return result;
    }

    static Identifier<Message> fromMessage(Message value) {
        checkNotNull(value);
        final Identifier<Message> result = create(Type.MESSAGE, value);
        return result;
    }

    private static <I> Identifier<I> create(Type type, I value) {
        return new Identifier<>(type, value);
    }

    static <I> I getDefaultValue(Class<I> idClass) {
        final Type type = Type.getType(idClass);
        final I result = type.getDefaultValue(idClass);
        return result;
    }

    private Identifier(Type type, I value) {
        this.value = value;
        this.type = type;
    }

    boolean isString() {
        return type == Type.STRING;
    }

    boolean isInteger() {
        return type == Type.INTEGER;
    }

    boolean isLong() {
        return type == Type.LONG;
    }

    boolean isMessage() {
        return type == Type.MESSAGE;
    }

    Any pack() {
        final Any result = type.pack(value);
        return result;
    }

    @Override
    public String toString() {
        String result;

        switch (type) {
            case INTEGER:
            case LONG:
                result = value.toString();
                break;

            case STRING:
                result = value.toString().trim();
                break;

            case MESSAGE:
                result = Stringifiers.idMessageToString((Message)value);
                result = result.trim();
                break;
            default:
                throw new IllegalStateException("toString() is not supported for type: " + type);
        }

        if (result.isEmpty()) {
            result = EMPTY_ID;
        }

        return result;
    }

    /**
     * Supported types of identifiers.
     */
    @SuppressWarnings({"OverlyStrongTypeCast" /* We do this for clarity. We cannot get OrBuilder instances here. */,
                        "unchecked" /* We ensure type by matching it first. */})
    enum Type {
        STRING {
            @Override
            <I> boolean matchValue(I id) {
                return id instanceof String;
            }

            @Override
            boolean matchMessage(Message message) {
                return message instanceof StringValue;
            }

            @Override
            <I> boolean matchClass(Class<I> idClass) {
                return String.class.equals(idClass);
            }

            @Override
            <I> Message toMessage(I id) {
                return newStringValue((String)id);
            }

            @Override
            String fromMessage(Message message) {
                return ((StringValue)message).getValue();
            }

            @Override
            <I> I getDefaultValue(Class<I> idClass) {
                return (I) "";
            }
        },

        INTEGER {
            @Override
            <I> boolean matchValue(I id) {
                return id instanceof Integer;
            }

            @Override
            boolean matchMessage(Message message) {
                return message instanceof UInt32Value;
            }

            @Override
            <I> boolean matchClass(Class<I> idClass) {
                return Integer.class.equals(idClass);
            }

            @Override
            <I> Message toMessage(I id) {
                return UInt32Value.newBuilder()
                                  .setValue((Integer) id)
                                  .build();
            }

            @Override
            Integer fromMessage(Message message) {
                return ((UInt32Value)message).getValue();
            }

            @Override
            <I> I getDefaultValue(Class<I> idClass) {
                return (I) Integer.valueOf(0);
            }
        },

        LONG {
            @Override
            <I> boolean matchValue(I id) {
                return id instanceof Long;
            }

            @Override
            boolean matchMessage(Message message) {
                return message instanceof UInt64Value;
            }

            @Override
            <I> boolean matchClass(Class<I> idClass) {
                return Long.class.equals(idClass);
            }

            @Override
            <I> Message toMessage(I id) {
                return UInt64Value.newBuilder()
                                  .setValue((Long) id)
                                  .build();
            }

            @Override
            Long fromMessage(Message message) {
                return ((UInt64Value)message).getValue();
            }

            @Override
            <I> I getDefaultValue(Class<I> idClass) {
                return (I) Long.valueOf(0);
            }
        },

        MESSAGE {
            @Override
            <I> boolean matchValue(I id) {
                return id instanceof Message;
            }

            @Override
            boolean matchMessage(Message message) {
                return !(message instanceof StringValue
                        || message instanceof UInt32Value
                        || message instanceof UInt64Value);
            }

            @Override
            <I> boolean matchClass(Class<I> idClass) {
                return Message.class.isAssignableFrom(idClass);
            }

            @Override
            <I> Message toMessage(I id) {
                return (Message) id;
            }

            @Override
            Message fromMessage(Message message) {
                return message;
            }

            @Override
            <I> I getDefaultValue(Class<I> idClass) {
                Class<? extends Message> msgClass = (Class<? extends Message>) idClass;
                final Message result = Messages.newInstance(msgClass);
                return (I) result;
            }
        };

        abstract <I> boolean matchValue(I id);

        abstract boolean matchMessage(Message message);

        abstract <I> boolean matchClass(Class<I> idClass);

        abstract <I> Message toMessage(I id);

        abstract Object fromMessage(Message message);

        abstract <I> I getDefaultValue(Class<I> idClass);

        private static <I> Type getType(I id) {
            for (Type type : values()) {
                if (type.matchValue(id)) {
                    return type;
                }
            }
            throw unsupported(id);
        }

        <I> Any pack(I id) {
            final Message msg = toMessage(id);
            final Any result = AnyPacker.pack(msg);
            return result;
        }

        static <I> Type getType(Class<I> idClass) {
            for (Type type : values()) {
                if (type.matchClass(idClass)) {
                    return type;
                }
            }
            throw unsupportedClass(idClass);
        }

        static Object unpack(Any any) {
            final Message unpacked = AnyPacker.unpack(any);

            for (Type type : values()) {
                if (type.matchMessage(unpacked)) {
                    final Object result = type.fromMessage(unpacked);
                    return result;
                }
            }

            throw unsupported(unpacked);
        }

        static <I> IllegalArgumentException unsupported(I id) {
            return new IllegalArgumentException("ID of unsupported type encountered: " + id);
        }

        private static <I> IllegalArgumentException unsupportedClass(Class<I> idClass) {
            return new IllegalArgumentException("Unsupported ID class encountered: " + idClass);
        }
    }
}
