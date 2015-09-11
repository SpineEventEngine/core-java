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
import org.spine3.util.MessageValue;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Value object for aggregate root IDs.
 * <p>
 * An aggregate root ID can be defined as any valid Protobuf message, which content fits the business
 * logic of an application. The purpose of this class is to provide type safety for these messages.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters") // is OK as we want many factory methods.
public final class AggregateId extends MessageValue {

    private AggregateId(Message value) {
        super(checkNotNull(value));
    }

    /**
     * Creates a new non-null id of an aggregate root.
     *
     * @param value id value
     * @return new instance
     */
    public static AggregateId of(Message value) {
        return new AggregateId(value);
    }

    @SuppressWarnings("TypeMayBeWeakened") // We want already built instances on this level of API.
    public static AggregateId of(EventContext value) {
        return new AggregateId(Messages.fromAny(value.getAggregateId()));
    }

    public static String idToString(Message aggregateRootId) {
        return Messages.toJson(aggregateRootId);
    }

    @Override
    public String toString() {
        final Message value = value();
        return (idToString(value));
    }

    @Nonnull
    @Override
    public Message value() {
        final Message result = super.value();
        // We check this in constructor.
        assert result != null;
        return result;
    }
}
