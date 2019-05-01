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
package io.spine.server.stand;

import com.google.protobuf.Message;
import io.spine.client.Target;
import io.spine.option.EntityOption.Visibility;
import io.spine.server.entity.EntityVisibility;
import io.spine.type.TypeUrl;

/**
 * An abstract base for {@code RequestValidator}s, that check
 * whether the {@link Target target} is supported.
 */
abstract class AbstractTargetValidator<M extends Message> extends RequestValidator<M> {

    private final Visibility requiredVisibility;
    private final TypeRegistry typeRegistry;

    AbstractTargetValidator(Visibility requiredVisibility,
                            TypeRegistry typeRegistry) {
        super();
        this.requiredVisibility = requiredVisibility;
        this.typeRegistry = typeRegistry;
    }

    boolean typeRegistryContains(Target target) {
        TypeUrl typeUrl = getTypeOf(target);
        boolean result = typeRegistry.getTypes()
                                     .contains(typeUrl);
        return result;
    }

    boolean visibilitySufficient(Target target) {
        TypeUrl typeUrl = getTypeOf(target);
        EntityVisibility visibility = EntityVisibility.of(typeUrl.getMessageClass());
        return visibility.isAsLeast(requiredVisibility);
    }

    static TypeUrl getTypeOf(Target target) {
        String typeAsString = target.getType();
        return TypeUrl.parse(typeAsString);
    }
}
