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

package io.spine.server.stand;

import com.google.protobuf.Message;
import io.spine.string.Stringifier;
import io.spine.string.Stringifiers;
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Identifier.checkSupported;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;

/**
 * A {@link Stringifier} for the {@link AggregateStateId}.
 *
 * <p>An example of a stringified {@link AggregateStateId AggregateStateId<Int>}:
 * {@code "my.domain/my.type.of.State-Integer-271828"}.
 *
 * <p>An example of a stringified {@link AggregateStateId AggregateStateId<MyType>}:
 * {@code "my.domain/my.type.of.State-my.type.MyType-{foo=42, bar="abc"}"}.
 *
 * @author Dmytro Dashenkov
 */
final class AggregateStateIdStringifier extends Stringifier<AggregateStateId> {

    private static final String DIVIDER = "-";
    private static final int MEAN_STRING_LENGTH = 256;
    private static final String TYPE_NAME_DIVIDER = ".";
    private static final String JAVA_LANG_PACKAGE_NAME = "java.lang.";

    static Stringifier<AggregateStateId> getInstance() {
        return Singleton.INSTANCE.value;
    }

    private AggregateStateIdStringifier() {
        // Prevent direct instantiation.
    }

    @Override
    protected String toString(AggregateStateId id) {
        checkNotNull(id);

        String typeUrl = id.getStateType()
                           .value();
        Object genericId = id.getAggregateId();
        Class genericIdType = genericId.getClass();
        String idTypeString = idTypeToString(genericIdType);
        String genericIdString = Stringifiers.toString(genericId);

        String result = new StringBuilder(MEAN_STRING_LENGTH).append(typeUrl)
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

        int typeUrlEndIndex = s.indexOf(DIVIDER);
        checkArgument(typeUrlEndIndex > 0,
                      "Passed string does not contain a type URL.");
        String typeUrlString = s.substring(0, typeUrlEndIndex);
        TypeUrl typeUrl = TypeUrl.parse(typeUrlString);

        int idTypeStartIndex = typeUrlEndIndex + 1;
        int idTypeEndIndex = s.indexOf(DIVIDER, idTypeStartIndex);
        checkArgument(idTypeEndIndex > 0,
                      "Passed string does not contain the ID type.");
        String idTypeString = s.substring(idTypeStartIndex, idTypeEndIndex);
        Class idType = idTypeFromString(idTypeString);

        String idStringValue = s.substring(idTypeEndIndex + 1);
        Object genericId = Stringifiers.fromString(idStringValue, idType);

        AggregateStateId result = AggregateStateId.of(genericId, typeUrl);

        return result;
    }

    private static String idTypeToString(Class idType) {
        checkSupported(idType);
        String result;
        if (Message.class.isAssignableFrom(idType)) {
            result = TypeName.of(idType)
                             .value();
        } else {
            result = idType.getSimpleName();
        }
        return result;
    }

    private static Class idTypeFromString(String idTypeString) {
        Class result;
        if (idTypeString.contains(TYPE_NAME_DIVIDER)) {
            TypeName typeName = TypeName.of(idTypeString);
            result = typeName.getJavaClass();
        } else {
            try {
                result = Class.forName(JAVA_LANG_PACKAGE_NAME + idTypeString);
            } catch (ClassNotFoundException e) {
                throw illegalStateWithCauseOf(e);
            }
        }
        return result;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Stringifier<AggregateStateId> value = new AggregateStateIdStringifier();
    }
}
