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
import com.google.protobuf.BoolValue;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Messages;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Identifiers.EMPTY_ID;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.util.Exceptions.newIllegalArgumentException;
import static org.spine3.util.Exceptions.newIllegalStateException;

/**
 * // TODO:2017-03-13:dmytro.dashenkov: Edit javadoc
 * Wrapper of an identifier value.
 *
 * @author Alexander Yevsyukov
 * @author Dmytro Dashenkov
 */
class Messagifier<I> {

    private final Type type;
    private final I value;

    static <I> Messagifier<I> from(I value) {
        checkNotNull(value);
        final Type type = Type.getType(value);
        final Messagifier<I> result = create(type, value);
        return result;
    }

    static Messagifier<Message> fromMessage(Message value) {
        checkNotNull(value);
        final Messagifier<Message> result = create(Type.MESSAGE, value);
        return result;
    }

    private static <I> Messagifier<I> create(Type type, I value) {
        return new Messagifier<>(type, value);
    }

    static <I> I getDefaultValue(Class<I> idClass) {
        final Type type = Type.getType(idClass);
        final I result = type.getDefaultValue(idClass);
        return result;
    }

    private Messagifier(Type type, I value) {
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

    boolean isBoolean() {
        return type == Type.BOOLEAN;
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
            case BOOLEAN:
            case INTEGER:
            case LONG:
                result = value.toString();
                break;

            case STRING:
                result = value.toString().trim();
                break;

            case MESSAGE:
                result = Identifiers.idMessageToString((Message)value);
                result = result.trim();
                break;
            default:
                throw newIllegalStateException("toString() is not supported for type: %s", type);
        }

        if (result.isEmpty()) {
            result = EMPTY_ID;
        }

        return result;
    }

    /**
     * Supported types of identifiers.
     */
    @SuppressWarnings(
       {"OverlyStrongTypeCast" /* For clarity. We cannot get OrBuilder instances here. */,
        "unchecked" /* We ensure type by matching it first. */})
    enum Type {
        STRING {
            @Override
            <I> boolean matchValue(I object) {
                return object instanceof String;
            }

            @Override
            boolean matchMessage(Message message) {
                return message instanceof StringValue;
            }

            @Override
            <I> boolean matchClass(Class<I> cls) {
                return String.class.equals(cls);
            }

            @Override
            <I> Message toMessage(I object) {
                return newStringValue((String) object);
            }

            @Override
            String fromMessage(Message message) {
                return ((StringValue)message).getValue();
            }

            @Override
            <I> I getDefaultValue(Class<I> cls) {
                return (I) "";
            }
        },

        BOOLEAN {
            @Override
            <I> boolean matchValue(I object) {
                return object instanceof Boolean;
            }

            @Override
            boolean matchMessage(Message message) {
                return message instanceof BoolValue;
            }

            @Override
            <I> boolean matchClass(Class<I> cls) {
                // TODO:2017-03-13:dmytro.dashenkov: Check if we need to math primitive types here.
                return Boolean.class.equals(cls);
            }

            @Override
            <I> Message toMessage(I object) {
                return BoolValue.newBuilder()
                                .setValue((Boolean) object)
                                .build();
            }

            @Override
            Object fromMessage(Message message) {
                return ((BoolValue) message).getValue();
            }

            @Override
            <I> I getDefaultValue(Class<I> cls) {
                return (I) Boolean.FALSE;
            }
        },

        INTEGER {
            @Override
            <I> boolean matchValue(I object) {
                return object instanceof Integer;
            }

            @Override
            boolean matchMessage(Message message) {
                return message instanceof UInt32Value;
            }

            @Override
            <I> boolean matchClass(Class<I> cls) {
                return Integer.class.equals(cls);
            }

            @Override
            <I> Message toMessage(I object) {
                return UInt32Value.newBuilder()
                                  .setValue((Integer) object)
                                  .build();
            }

            @Override
            Integer fromMessage(Message message) {
                return ((UInt32Value)message).getValue();
            }

            @Override
            <I> I getDefaultValue(Class<I> cls) {
                return (I) Integer.valueOf(0);
            }
        },

        LONG {
            @Override
            <I> boolean matchValue(I object) {
                return object instanceof Long;
            }

            @Override
            boolean matchMessage(Message message) {
                return message instanceof UInt64Value;
            }

            @Override
            <I> boolean matchClass(Class<I> cls) {
                return Long.class.equals(cls);
            }

            @Override
            <I> Message toMessage(I object) {
                return UInt64Value.newBuilder()
                                  .setValue((Long) object)
                                  .build();
            }

            @Override
            Long fromMessage(Message message) {
                return ((UInt64Value)message).getValue();
            }

            @Override
            <I> I getDefaultValue(Class<I> cls) {
                return (I) Long.valueOf(0);
            }
        },

        MESSAGE {
            @Override
            <I> boolean matchValue(I object) {
                return object instanceof Message;
            }

            /**
             * Verifies if the passed message is not an instance of a wrapper for
             * simple types that are used for packing simple Java types into {@code Any}.
             *
             * @return {@code true} if the message is not {@code StringValue}, or {@code UInt32Value}, or {@code UInt64Value}
             */
            @Override
            boolean matchMessage(Message message) {
                return !(message instanceof StringValue
                        || message instanceof UInt32Value
                        || message instanceof UInt64Value);
            }

            @Override
            <I> boolean matchClass(Class<I> cls) {
                return Message.class.isAssignableFrom(cls);
            }

            @Override
            <I> Message toMessage(I object) {
                return (Message) object;
            }

            @Override
            Message fromMessage(Message message) {
                return message;
            }

            @Override
            <I> I getDefaultValue(Class<I> cls) {
                Class<? extends Message> msgClass = (Class<? extends Message>) cls;
                final Message result = Messages.newInstance(msgClass);
                return (I) result;
            }
        };

        abstract <I> boolean matchValue(I object);

        abstract boolean matchMessage(Message message);

        abstract <I> boolean matchClass(Class<I> cls);

        abstract <I> Message toMessage(I object);

        abstract Object fromMessage(Message message);

        abstract <I> I getDefaultValue(Class<I> cls);

        private static <I> Type getType(I object) {
            for (Type type : values()) {
                if (type.matchValue(object)) {
                    return type;
                }
            }
            throw unsupported(object);
        }

        <I> Any pack(I object) {
            final Message msg = toMessage(object);
            final Any result = AnyPacker.pack(msg);
            return result;
        }

        static <I> Type getType(Class<I> cls) {
            for (Type type : values()) {
                if (type.matchClass(cls)) {
                    return type;
                }
            }
            throw unsupportedClass(cls);
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
            return newIllegalArgumentException("ID of unsupported type encountered: %s", id);
        }

        private static <I> IllegalArgumentException unsupportedClass(Class<I> idClass) {
            return newIllegalArgumentException("Unsupported ID class encountered: %s",
                                               idClass.getName());
        }
    }
}
