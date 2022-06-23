/*
 * Copyright 2022, TeamDev. All rights reserved.
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
package io.spine.server.entity;

import io.spine.base.EntityState;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.dispatch.Success;
import io.spine.server.type.EventEnvelope;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.event.EntProjectCreated;
import io.spine.testing.server.TestEventFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A utility class providing various test-only methods, which in production mode are allowed
 * with the transactions only.
 */
public class TestTransaction {

    /** Prevents instantiation from outside. */
    private TestTransaction() {
    }

    /**
     * A hack allowing to inject state and version into an {@code TransactionalEntity} instance
     * by creating and committing a fake transaction.
     *
     * <p>To be used in tests only.
     */
    public static void
    injectState(TransactionalEntity entity, EntityState state, Version version) {
        TestTx tx = new TestTx(entity, state, version);
        tx.commit();
    }

    /**
     * A hack allowing to archive an {@code TransactionalEntity} instance by creating and committing
     * a fake transaction.
     *
     * <p>To be used in tests only.
     */
    public static void archive(TransactionalEntity entity) {
        TestTx tx = new TestTx(entity) {

            @Override
            protected DispatchOutcome dispatch(TransactionalEntity entity, EventEnvelope event) {
                entity.setArchived(true);
                return super.dispatch(entity, event);
            }
        };

        tx.dispatchForTest();
        tx.commit();
    }

    /**
     * A hack allowing to mark an {@code TransactionalEntity} instance deleted by creating and
     * committing a fake transaction.
     *
     * <p>To be used in tests only.
     */
    public static void delete(TransactionalEntity entity) {
        TestTx tx = new TestTx(entity) {

            @Override
            protected DispatchOutcome dispatch(TransactionalEntity entity, EventEnvelope event) {
                entity.setDeleted(true);
                return super.dispatch(entity, event);
            }
        };

        tx.dispatchForTest();
        tx.commit();
    }

    @SuppressWarnings("unchecked")
    private static class TestTx extends EventPlayingTransaction {

        private TestTx(TransactionalEntity entity) {
            super(entity);
        }

        private TestTx(TransactionalEntity entity, EntityState state, Version version) {
            super(entity, state, version);
        }

        @Override
        protected DispatchOutcome dispatch(TransactionalEntity entity, EventEnvelope event) {
            // NoOp by default
            return DispatchOutcome
                    .newBuilder()
                    .setPropagatedSignal(event.outerObject().messageId())
                    .setSuccess(Success.getDefaultInstance())
                    .vBuild();
        }

        @Override
        protected VersionIncrement createVersionIncrement(EventEnvelope event) {
            return new NoIncrement(this);
        }

        private void dispatchForTest() {
            EntProjectCreated eventMessage = EntProjectCreated
                    .newBuilder()
                    .setProjectId(ProjectId.getDefaultInstance())
                    .buildPartial();
            TestEventFactory factory = TestEventFactory.newInstance(TestTransaction.class);
            Event event = factory.createEvent(eventMessage);
            dispatch(entity(), EventEnvelope.of(event));
        }
    }

    private static class NoIncrement extends VersionIncrement {

        private final Transaction transaction;

        private NoIncrement(Transaction transaction) {
            this.transaction = checkNotNull(transaction);
        }

        @Override
        public Version nextVersion() {
            return transaction.version();
        }
    }
}
