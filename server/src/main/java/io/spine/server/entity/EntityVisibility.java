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

package io.spine.server.entity;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.option.EntityOption;
import io.spine.option.EntityOption.Kind;
import io.spine.option.EntityOption.Visibility;
import io.spine.option.OptionsProto;
import io.spine.type.TypeName;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.option.EntityOption.Kind.PROJECTION;
import static io.spine.option.EntityOption.Visibility.DEFAULT;
import static io.spine.option.EntityOption.Visibility.FULL;
import static io.spine.option.EntityOption.Visibility.NONE;
import static io.spine.option.EntityOption.Visibility.QUERY;
import static io.spine.option.EntityOption.Visibility.SUBSCRIBE;

@Internal
public final class EntityVisibility {

    private final Visibility value;

    private EntityVisibility(Visibility value) {
        this.value = value;
    }

    public static EntityVisibility of(Class<? extends Message> stateClass) {
        checkNotNull(stateClass);
        Descriptor descriptor = TypeName.of(stateClass).messageDescriptor();
        EntityOption entityOption = descriptor.getOptions()
                                              .getExtension(OptionsProto.entity);
        Visibility visibility = entityOption.getVisibility();
        if (visibility == DEFAULT) {
            Kind kind = entityOption.getKind();
            visibility = kind == PROJECTION ? FULL : NONE;
        }
        return new EntityVisibility(visibility);
    }

    public boolean canSubscribe() {
        return value == SUBSCRIBE || value == FULL;
    }

    public boolean canQuery() {
        return value == QUERY || value == FULL;
    }

    public boolean isNotNone() {
        return value != NONE;
    }

    public boolean is(Visibility visibility) {
        checkNotNull(visibility);
        return value == visibility;
    }

    @Override
    public String toString() {
        return value.name();
    }
}
