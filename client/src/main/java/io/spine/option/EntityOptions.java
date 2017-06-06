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

package io.spine.option;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.option.EntityOption.Visibility;
import io.spine.type.TypeName;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utilities for working with {@link EntityOption}s.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public class EntityOptions {

    private EntityOptions() {
        // Prevent instantiation of this utility class.
    }

    public static Visibility getVisibility(Class<? extends Message> stateClass) {
        checkNotNull(stateClass);
        final Descriptors.Descriptor descriptor = TypeName.of(stateClass).getDescriptor();
        final EntityOption entityOption = descriptor.getOptions()
                                                    .getExtension(OptionsProto.entity);
        final Visibility definedVisibility = entityOption.getVisibility();
        final Visibility result = (definedVisibility == Visibility.VISIBILITY_UNKNOWN)
                ? Visibility.FULL
                : definedVisibility;
        return result;
    }
}
