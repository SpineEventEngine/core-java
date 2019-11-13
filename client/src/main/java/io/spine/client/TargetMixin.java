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

package io.spine.client;

import com.google.protobuf.Message;
import io.spine.annotation.GeneratedMixin;
import io.spine.base.EntityState;
import io.spine.base.EventMessage;
import io.spine.type.TypeUrl;

import java.util.Collection;

import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Extends the {@link Target} with validation routines.
 */
@GeneratedMixin
@SuppressWarnings("override") // Methods are implemented in the generated code.
public interface TargetMixin {

    String getType();

    TargetFilters getFilters();

    /**
     * Verifies that the target type is a valid type for querying.
     *
     * @throws IllegalArgumentException
     *         if the target type is not a valid type for querying
     */
    default void checkTypeValid() {
        String type = getType();
        Class<Message> targetClass = TypeUrl.parse(type)
                                            .getMessageClass();
        boolean isEntityState = EntityState.class.isAssignableFrom(targetClass);
        boolean isEventMessage = EventMessage.class.isAssignableFrom(targetClass);
        if (!isEntityState && !isEventMessage) {
            throw newIllegalArgumentException(
                    "The queried type should represent either an entity state or an event " +
                            "message. Got type `%s` instead.", targetClass.getCanonicalName());
        }
    }

    /**
     * Verifies that the target has valid type and filters.
     *
     * @throws IllegalArgumentException
     *         if either the target type is not a valid type for querying or the filters are
     *         invalid
     * @see FilterMixin#validateAgainst(TypeUrl)
     */
    default void checkValid() {
        checkTypeValid();
        TypeUrl typeUrl = TypeUrl.parse(getType());
        getFilters()
                .getFilterList()
                .stream()
                .map(CompositeFilter::getFilterList)
                .flatMap(Collection::stream)
                .forEach(filter -> filter.validateAgainst(typeUrl));
    }
}
