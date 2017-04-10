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

package org.spine3.server.stand;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.protobuf.Message;
import org.spine3.base.Stringifier;
import org.spine3.base.Stringifiers;
import org.spine3.type.ClassName;
import org.spine3.type.KnownTypes;
import org.spine3.type.TypeName;
import org.spine3.type.TypeUrl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Identifiers.checkSupported;
import static org.spine3.util.Exceptions.wrappedCause;

/**
 * A {@link Stringifier} for the {@link AggregateStateId}.
 *
 * <p>The protocol of strigification is as follows:
 * {@code {@linkplain TypeUrl}_of_the_state_type}-{@code ID_TYPE_NAME_or_SHORT_REPR_FOR_PRIMITIVES}-{@code generic_ID_stringifier_output}
 *
 * @author Dmytro Dashenkov
 */
class AggregateStateIdStringifier extends Stringifier<AggregateStateId> {

    private static final String DIVIDER = "-";
    private static final int MEAN_STRING_LENGTH = 256;

    @Override
    protected String toString(AggregateStateId id) {
        checkNotNull(id);

        final String typeUrl = id.getStateType()
                                 .value();
        final Object genericId = id.getAggregateId();
        final Class genericIdType = genericId.getClass();
        final String idTypeString = idTypeToString(genericIdType);
        final String genericIdString = Stringifiers.toString(genericId);

        final String result = new StringBuilder(MEAN_STRING_LENGTH).append(typeUrl)
                                                                   .append(DIVIDER)
                                                                   .append(idTypeString)
                                                                   .append(DIVIDER)
                                                                   .append(genericIdString)
                                                                   .toString();
        return result;
    }

    @Override
    protected AggregateStateId fromString(String s) {
        checkNotNull(s);

        final int typeUrlEndIndex = s.indexOf(DIVIDER);
        checkArgument(typeUrlEndIndex > 0,
                      "Passed string does not contain a type URL.");
        final String typeUrlString = s.substring(0, typeUrlEndIndex);
        final TypeUrl typeUrl = TypeUrl.parse(typeUrlString);

        final int idTypeStartIndex = typeUrlEndIndex + 1;
        final int idTypeEndIndex = s.indexOf(DIVIDER, idTypeStartIndex);
        checkArgument(typeUrlEndIndex > 0,
                      "Passed string does not contain the ID type.");
        final String idTypeString = s.substring(idTypeStartIndex, idTypeEndIndex);
        final Class idType = idTypeFromString(idTypeString);

        final String idStringValue = s.substring(idTypeEndIndex + 1);
        final Object genericId = Stringifiers.fromString(idStringValue, idType);

        final AggregateStateId result = AggregateStateId.of(genericId, typeUrl);

        return result;
    }

    private static String idTypeToString(Class idType) {
        checkSupported(idType);
        final IdType type = IdType.get(idType);
        final String typeString = type.describe(idType);
        return typeString;
    }

    private static Class idTypeFromString(String idTypeString) {
        final Class type = IdType.getTypeFrom(idTypeString);
        return type;
    }

    /**
     * An {@code enum} of allowed ID types.
     */
    private enum IdType {

        STRING {
            @Override
            Class getType() {
                return String.class;
            }
        },
        INT {
            @Override
            Class getType() {
                return Integer.class;
            }
        },
        LONG {
            @Override
            Class getType() {
                return Long.class;
            }
        },
        MESSAGE {
            @SuppressWarnings("unchecked") // Logically checked
            @Override
            String describe(Class cls) {
                return TypeName.of(cls)
                               .value();
            }

            @Override
            Class getType() {
                return Message.class;
            }
        };

        static IdType get(Class cls) {
            if (cls.equals(String.class)) {
                return STRING;
            } else if (cls.equals(Integer.class)) {
                return INT;
            } else if (cls.equals(Long.class)) {
                return LONG;
            } else {
                return MESSAGE;
            }
        }

        static Class getTypeFrom(String description) {
            final Optional<IdType> type = Enums.getIfPresent(IdType.class, description);
            if (type.isPresent()) {
                return type.get()
                           .getType();
            } else {
                final TypeName typeName = TypeName.of(description);
                final ClassName className = KnownTypes.getClassName(typeName.toUrl());
                try {
                    final Class result = Class.forName(className.value());
                    return result;
                } catch (ClassNotFoundException e) {
                    // Should never happen
                    throw wrappedCause(e);
                }
            }
        }

        String describe(Class cls) {
            return name();
        }

        abstract Class getType();
    }
}
