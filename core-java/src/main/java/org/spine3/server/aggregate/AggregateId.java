/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.aggregate;

import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.protobuf.Messages;
import org.spine3.server.internal.EntityId;

/**
 * Value object for aggregate IDs.
 *
 * @param <I> the type of aggregate IDs
 * @author Alexander Yevsyukov
 */
public final class AggregateId<I> extends EntityId<I> {

    /**
     * The standard name for properties holding an ID of an aggregate.
     */
    public static final String PROPERTY_NAME = "aggregateId";

    /**
     * The standard name for a parameter containing an aggregate ID.
     */
    public static final String PARAM_NAME = PROPERTY_NAME;

    private AggregateId(I value) {
        super(value);
    }

    /**
     * Creates a new non-null id of an aggregate root.
     *
     * @param value id value
     * @return new instance
     */
    public static <I> AggregateId<I> of(I value) {
        return new AggregateId<>(value);
    }

    @SuppressWarnings("TypeMayBeWeakened") // We want already built instances at this level of API.
    public static AggregateId<? extends Message> of(EventContext value) {
        final Message message = Messages.fromAny(value.getAggregateId());
        final AggregateId<Message> result = new AggregateId<>(message);
        return result;
    }
}
