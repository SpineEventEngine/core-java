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
import io.spine.server.entity.given.tx.event.TxCreated;
import io.spine.server.entity.given.tx.event.TxErrorRequested;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.procman.ProcessManager;
import io.spine.validate.ConstraintViolation;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;

/**
 * Test environment Process Manager for {@link io.spine.server.procman.PmTransactionTest}.
 */
public class TxProcessManager extends ProcessManager<Id, PmState, PmState.Builder> {

    private final List<Message> receivedEvents = newLinkedList();
    private final @Nullable List<ConstraintViolation> violations;

    public TxProcessManager(Id id) {
        this(id, null);
    }

    public TxProcessManager(Id id, @Nullable List<ConstraintViolation> violations) {
        super(id);
        this.violations = violations == null
                          ? null
                          : ImmutableList.copyOf(violations);
    }

    @Override
    protected List<ConstraintViolation> checkEntityState(PmState newState) {
        if (violations != null) {
            return ImmutableList.copyOf(violations);
        }
        return super.checkEntityState(newState);
    }

    @React
    Nothing event(TxCreated e) {
        builder().setId(id());
        receivedEvents.add(e);
        PmState newState = PmState.newBuilder(state())
                                  .setId(e.getId())
                                  .build();
        builder().mergeFrom(newState);
        return nothing();
    }

    /**
     * Always throws {@code RuntimeException} to emulate the case of an error in
     * a reacting method of a Process Manager.
     *
     * @see io.spine.server.procman.PmTransactionTest#createEventThatFailsInHandler()
     */
    @React
    Nothing event(TxErrorRequested e) {
        throw new RuntimeException("that tests the tx behaviour for process manager");
    }

    public List<Message> receivedEvents() {
        return ImmutableList.copyOf(receivedEvents);
    }
}
