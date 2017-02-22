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

package org.spine3.server.entity.idfunc;

import com.google.protobuf.Message;
import org.spine3.base.EventContext;

/**
 * Obtains an event producer ID based on an event {@link Message} and context.
 *
 * <p>An ID must be the first field in event messages (in Protobuf definition).
 * Its name must end with the
 * {@link org.spine3.base.Identifiers#ID_PROPERTY_SUFFIX Identifiers.ID_PROPERTY_SUFFIX}.
 *
 * @param <I> the type of target entity IDs
 * @param <M> the type of event messages to get IDs from
 * @author Alexander Litus
 */
class GetProducerIdFromEvent<I, M extends Message> extends GetIdByFieldIndex<I, M, EventContext> {

    private GetProducerIdFromEvent(int idIndex) {
        super(idIndex);
    }

    /**
     * Creates a new instance.
     *
     * @param index a zero-based index of an ID field in this type of messages
     */
    static<I, M extends Message> GetProducerIdFromEvent<I, M> fromFieldIndex(int index) {
        return new GetProducerIdFromEvent<>(index);
    }
}
