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
package org.spine3.server.entity;

import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.base.Version;
import org.spine3.base.Versions;
import org.spine3.validate.AbstractValidatingBuilder;
import org.spine3.validate.ConstraintViolationThrowable;
import org.spine3.validate.ValidatingBuilder;

import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.server.entity.InvalidEntityStateException.onConstraintViolations;

/**
 * @author Alex Tymchenko
 */
public abstract class Transaction<I,
                                  E extends EventPlayingEntity<I, S, B>,
                                  S extends Message,
                                  B extends ValidatingBuilder<S, ? extends Message.Builder>> {

    /**
     * The builder for the entity state.
     */
    private final B builder;

    private final E entity;

    private volatile Version version;

    private volatile LifecycleFlags lifecycleFlags;

    /**
     * The flag, which becomes {@code true}, if the state of the entity
     * {@linkplain #commit() has been changed} since it has been
     * {@linkplain RecordBasedRepository#findOrCreate(Object)} loaded or created.
     */
    private volatile boolean stateChanged;


    /**
     * Creates a new instance of {@code Transaction} and
     * {@linkplain EventPlayingEntity#injectTransaction(Transaction) injects} the newly created
     * transaction into the given {@code entity}.
     *
     * @param entity the entity to create the transaction for
     */
    protected Transaction(E entity) {
        this.entity = entity;
        this.builder = entity.builderFromState();
        this.version = entity.getVersion();
        this.lifecycleFlags = entity.getLifecycleFlags();
        final Transaction<I, E, S, B> tx = this;
        entity.injectTransaction(tx);
    }

    protected Transaction(E entity, S state, Version version) {
        this(entity);
        entity.getBuilder().mergeFrom(state);
        setVersion(version);
    }

    protected abstract void apply(Message eventMessage, EventContext context)
            throws InvocationTargetException;

    protected void commit() throws InvalidEntityStateException {

        final B builder = getBuilder();

        // The state is only updated, if at least some changes were made to the builder.
        if (builder.isDirty()) {
            try {
                final S newState = builder.build();

                markStateChanged();
                entity.updateState(newState, version);
            } catch (ConstraintViolationThrowable exception) {
                // should only happen if someone is injecting the state not using the builder.

                final Message invalidState = ((AbstractValidatingBuilder) builder).internalBuild();
                throw onConstraintViolations(invalidState, exception.getConstraintViolations());
            } finally {
                entity.releaseTransaction();
            }
        }
    }


    Phase applyAnd(Message eventMessage, EventContext context) throws InvocationTargetException {
        apply(eventMessage, context);
        return new Phase(this);
    }

    B getBuilder() {
        return builder;
    }

    private void markStateChanged() {
        this.stateChanged = true;
    }

    protected boolean isStateChanged() {
        return stateChanged;
    }


    void setVersion(Version version) {
        checkNotNull(version);
        this.version = version;
    }

    LifecycleFlags getLifecycleFlags() {
        return lifecycleFlags;
    }

    void setLifecycleFlags(LifecycleFlags lifecycleFlags) {
        checkNotNull(lifecycleFlags);
        this.lifecycleFlags = lifecycleFlags;
    }

    protected E getEntity() {
        return entity;
    }

    protected static class Phase {

        private final Transaction underlyingTransaction;

        public Phase(Transaction transaction) {
            this.underlyingTransaction = transaction;
        }

        Phase thenAdvanceVersionFrom(Version current) {
            underlyingTransaction.setVersion(Versions.increment(current));
            return this;
        }

        protected Phase thenApply(Message eventMessage,
                                  EventContext context) throws
                                                                   InvocationTargetException {
            underlyingTransaction.apply(eventMessage, context);
            return this;
        }

        protected Phase thenCommit() {
            underlyingTransaction.commit();
            return this;
        }

        public Transaction getTransaction() {
            return underlyingTransaction;
        }
    }
}
