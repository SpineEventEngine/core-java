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

import com.google.protobuf.Message;
import org.spine3.base.Stringifier;
import org.spine3.base.Stringifiers;
import org.spine3.type.TypeName;
import org.spine3.type.TypeUrl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Identifiers.checkSupported;
import static org.spine3.util.Exceptions.wrappedCause;

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
class AggregateStateIdStringifier extends Stringifier<AggregateStateId> {

    private static final String DIVIDER = "-";
    private static final int MEAN_STRING_LENGTH = 256;
    private static final String TYPE_NAME_DIVIDER = ".";
    private static final String JAVA_LANG_PACKAGE_NAME = "java.lang.";

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
        final String result;
        if (Message.class.isAssignableFrom(idType)) {
            result = TypeName.of(idType).value();
        } else {
            result = idType.getSimpleName();
        }
        return result;
    }

    private static Class idTypeFromString(String idTypeString) {
        final Class result;
        if (idTypeString.contains(TYPE_NAME_DIVIDER)) {
            final TypeName typeName = TypeName.of(idTypeString);
            result = typeName.getJavaClass();
        } else {
            try {
                result = Class.forName(JAVA_LANG_PACKAGE_NAME + idTypeString);
            } catch (ClassNotFoundException e) {
                throw wrappedCause(e);
            }
        }
        return result;
    }
}
