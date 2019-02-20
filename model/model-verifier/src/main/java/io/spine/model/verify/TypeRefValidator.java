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

import com.google.common.collect.ImmutableSet;
import io.spine.code.proto.MessageType;
import io.spine.code.proto.ref.CompositeTypeRef;
import io.spine.code.proto.ref.TypeRef;
import io.spine.server.model.UnknownReferencedTypeError;
import io.spine.type.KnownTypes;

import java.util.Collection;

final class TypeRefValidator {

    private final TypeRef typeRef;
    private final TypeValidator<MessageType> typeValidator;

    private TypeRefValidator(TypeRef typeRef, TypeValidator<MessageType> typeValidator) {
        this.typeRef = typeRef;
        this.typeValidator = typeValidator;
    }

    static TypeRefValidator withTypeValidator(TypeRef ref,
                                              TypeValidator<MessageType> typeValidator) {
        return new TypeRefValidator(ref, typeValidator);
    }

    void validate() {
        ImmutableSet<MessageType> referencedTypes = KnownTypes.instance()
                                                              .findAll(typeRef);
        verifyTypeCount(referencedTypes);
        checkAgainstValidator(referencedTypes);
    }

    private void verifyTypeCount(Collection<MessageType> referenced) {
        if (!typeCountMatches(referenced)) {
            throwNonexistentTypeError();
        }
    }

    private boolean typeCountMatches(Collection<MessageType> referenced) {
        if (typeRef instanceof CompositeTypeRef) {
            CompositeTypeRef composite = (CompositeTypeRef) typeRef;
            return composite.refCount() <= referenced.size();
        }
        return !referenced.isEmpty();
    }

    private void throwNonexistentTypeError() {
        throw new UnknownReferencedTypeError(typeRef);
    }

    private void checkAgainstValidator(Iterable<MessageType> types) {
        types.forEach(typeValidator::check);
    }
}
