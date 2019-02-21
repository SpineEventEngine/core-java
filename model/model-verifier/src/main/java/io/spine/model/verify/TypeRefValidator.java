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

package io.spine.model.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.code.proto.MessageType;
import io.spine.code.proto.ref.CompositeTypeRef;
import io.spine.code.proto.ref.TypeRef;
import io.spine.server.model.TypeMismatchError;
import io.spine.server.model.UnknownReferencedTypeError;
import io.spine.type.KnownTypes;

import java.util.function.Predicate;

final class TypeRefValidator {

    private final Predicate<MessageType> predicate;
    private final String errorMessage;

    private TypeRefValidator(Predicate<MessageType> predicate, String errorMessage) {
        this.predicate = predicate;
        this.errorMessage = errorMessage;
    }

    static TypeRefValidator withPredicate(Predicate<MessageType> predicate, String errorMessage) {
        return new TypeRefValidator(predicate, errorMessage);
    }

    void check(TypeRef typeRef) {
        if (typeRef instanceof CompositeTypeRef) {
            CompositeTypeRef composite = (CompositeTypeRef) typeRef;
            ImmutableList<TypeRef> elements = composite.elements();
            elements.forEach(this::doCheck);
        } else {
            doCheck(typeRef);
        }
    }

    private void doCheck(TypeRef typeRef) {
        ImmutableSet<MessageType> types = KnownTypes.instance()
                                                    .resolve(typeRef);
        if (types.isEmpty()) {
            throwUnresolvedRefError(typeRef);
        }
        types.forEach(this::checkAgainstPredicate);
    }

    private static void throwUnresolvedRefError(TypeRef typeRef) {
        throw new UnknownReferencedTypeError(typeRef);
    }

    private void checkAgainstPredicate(MessageType type) {
        if (!predicate.test(type)) {
            throwTypeMismatchError(type);
        }
    }

    private void throwTypeMismatchError(MessageType type) {
        throw new TypeMismatchError(errorMessage, type);
    }
}
