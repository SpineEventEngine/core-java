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
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;

/**
 * Extends {@link SubscriptionUpdate} with useful methods.
 */
@GeneratedMixin
public interface SubscriptionUpdateMixin {

    @SuppressWarnings("override") // for generated code
    EntityUpdates getEntityUpdates();

    @SuppressWarnings("override") // for generated code
    EventUpdates getEventUpdates();

    /**
     * Obtains the entity state at the given index.
     *
     * @throws IndexOutOfBoundsException
     *         if the index is out of the range of entities returned by this update
     */
    default Message state(int index) {
        EntityStateUpdate stateUpdate =
                getEntityUpdates().getUpdateList()
                                  .get(index);
        Message result = AnyPacker.unpack(stateUpdate.getState());
        return result;
    }

    /**
     * Obtains the event at the given index.
     *
     * @throws IndexOutOfBoundsException
     *         if the index is out of the range of events returned by this update
     */
    default Event event(int index) {
        Event result = getEventUpdates().getEvent(index);
        return result;
    }
}
