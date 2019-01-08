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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import io.spine.string.StringifierRegistry;
import io.spine.type.TypeUrl;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An identifier for the state of a certain {@link io.spine.server.aggregate.Aggregate Aggregate}.
 *
 * <p>{@code Aggregate} state is defined by {@link TypeUrl TypeUrl}.
 *
 * <p>The {@code AggregateStateId} is used to store and access the latest {@code Aggregate}
 * states in a {@link Stand}.
 *
 * @param <I> the type for IDs of the source aggregate
 * @author Alex Tymchenko
 */
public final class AggregateStateId<I> {

    static {
        StringifierRegistry.getInstance()
                           .register(AggregateStateIdStringifier.getInstance(),
                                     AggregateStateId.class);
    }

    private final I aggregateId;
    private final TypeUrl stateType;

    private AggregateStateId(I aggregateId, TypeUrl stateType) {
        this.aggregateId = checkNotNull(aggregateId);
        this.stateType = checkNotNull(stateType);
    }

    public static <I> AggregateStateId of(I aggregateId, TypeUrl stateType) {
        return new AggregateStateId<>(aggregateId, stateType);
    }

    public I getAggregateId() {
        return aggregateId;
    }

    public TypeUrl getStateType() {
        return stateType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AggregateStateId)) {
            return false;
        }
        AggregateStateId<?> that = (AggregateStateId<?>) o;
        return Objects.equal(aggregateId, that.aggregateId) &&
                Objects.equal(stateType, that.stateType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(aggregateId, stateType);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("aggregateId", aggregateId)
                          .add("stateType", stateType)
                          .toString();
    }
}
