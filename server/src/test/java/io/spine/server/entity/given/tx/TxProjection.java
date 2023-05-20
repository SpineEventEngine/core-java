/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.entity.given.tx;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.core.Subscribe;
import io.spine.server.entity.given.tx.ProjectionState.ProjectionType;
import io.spine.server.entity.given.tx.event.TxCreated;
import io.spine.server.entity.given.tx.event.TxErrorRequested;
import io.spine.server.entity.given.tx.event.TxStateErrorRequested;
import io.spine.server.projection.Projection;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.server.entity.given.tx.ProjectionState.ProjectionType.VERY_USEFUL;

/**
 * Test environment projection for {@link io.spine.server.projection.ProjectionTransactionTest}.
 */
public final class TxProjection
        extends Projection<Id, ProjectionState, ProjectionState.Builder>
        implements ProjectionStateWithColumns {

    private final List<Message> receivedEvents = newLinkedList();

    private final boolean isNullType;

    public TxProjection(Id id, boolean isNullType) {
        super(id);
        this.isNullType = isNullType;
    }

    public TxProjection(Id id) {
        this(id, false);
    }

    @Subscribe
    void event(TxCreated e) {
        receivedEvents.add(e);
        builder().setId(id())
                 .setName(e.getName());
    }

    /**
     * Always throws {@code RuntimeException} to emulate an error in
     * a subscribing method of a Projection.
     *
     * @see io.spine.server.projection.ProjectionTransactionTest#failingInHandler
     */
    @Subscribe
    void event(TxErrorRequested e) {
        throw new RuntimeException("that tests the projection tx behaviour");
    }

    @Subscribe
    void event(TxStateErrorRequested e) {
        // By convention the first field of state is required.
        // Clearing it should fail the validation when the transaction is committed.
        builder().clearId();
    }

    public List<Message> receivedEvents() {
        return ImmutableList.copyOf(receivedEvents);
    }

    @Override
    public int getNameLength() {
        return state().getName()
                      .length();
    }

    @Override
    public @Nullable ProjectionType getType() {
        if (isNullType) {
            return null;
        }
        return VERY_USEFUL;
    }
}
