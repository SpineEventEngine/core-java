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

package io.spine.server.entity.given.tx;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.core.Subscribe;
import io.spine.server.entity.given.tx.event.TxCreated;
import io.spine.server.entity.given.tx.event.TxErrorRequested;
import io.spine.server.projection.Projection;
import io.spine.validate.ConstraintViolation;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;

/**
 * Test environment projection for {@link io.spine.server.projection.ProjectionTransactionTest}.
 */
public class TxProjection extends Projection<Id, ProjectionState, ProjectionState.Builder> {

    private final List<Message> receivedEvents = newLinkedList();
    private final List<ConstraintViolation> violations;

    public TxProjection(Id id) {
        this(id, null);
    }

    public TxProjection(Id id,
                        @Nullable ImmutableList<ConstraintViolation> violations) {
        super(id);
        this.violations = violations;
    }

    @Override
    protected List<ConstraintViolation> checkEntityState(ProjectionState newState) {
        if (violations != null) {
            return ImmutableList.copyOf(violations);
        }
        return super.checkEntityState(newState);
    }

    @Subscribe
    void event(TxCreated e) {
        builder().setId(id());
        receivedEvents.add(e);
        ProjectionState newState = ProjectionState
                .newBuilder(state())
                .setId(e.getId())
                .build();
        builder().mergeFrom(newState);
    }

    /**
     * Always throws {@code RuntimeException} to emulate an error in
     * a subscribing method of a Projection.
     *
     * @see io.spine.server.projection.ProjectionTransactionTest#createEventThatFailsInHandler
     */
    @Subscribe
    void event(TxErrorRequested e) {
        throw new RuntimeException("that tests the projection tx behaviour");
    }

    public List<Message> receivedEvents() {
        return ImmutableList.copyOf(receivedEvents);
    }
}
