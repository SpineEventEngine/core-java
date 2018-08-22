/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.reflect.GenericTypeIndex;
import io.spine.validate.ValidatingBuilder;
import io.spine.validate.ValidatingBuilders;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.server.entity.TransactionalEntity.GenericParameter.STATE_BUILDER;

/**
 * A base for entities, perform transactions {@linkplain Event events}.
 *
 * <p>Defines a transaction-based mechanism for state, version and lifecycle flags update.
 *
 * <p>Exposes {@linkplain #getBuilder()} validating builder} for the state as the only way
 * to modify the state from the descendants.
 *
 * @author Alex Tymchenko
 */
public abstract class TransactionalEntity<I,
                                          S extends Message,
                                          B extends ValidatingBuilder<S, ? extends Message.Builder>>
                      extends AbstractVersionableEntity<I, S> {

    private final RecentHistory recentHistory = new RecentHistory();

    /**
     * The flag, which becomes {@code true}, if the state of the entity has been changed
     * since it has been {@linkplain RecordBasedRepository#findOrCreate(Object) loaded or created}.
     */
    private volatile boolean stateChanged;

    private volatile
    @Nullable Transaction<I, ? extends TransactionalEntity<I, S, B>, S, B> transaction;

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the
     *                                  {@linkplain Entity supported types}
     */
    protected TransactionalEntity(I id) {
        super(id);
    }

    /**
     * Obtains recent history of events of this entity.
     */
    protected RecentHistory recentHistory() {
        return recentHistory;
    }

    /**
     * Adds events to the {@linkplain #recentHistory() recent history}.
     */
    protected void remember(Iterable<Event> events) {
        recentHistory.addAll(events);
    }

    /**
     * Clears {@linkplain #recentHistory() recent history}.
     */
    protected void clearRecentHistory() {
        recentHistory.clear();
    }

    /**
     * Determines whether the state of this entity or its lifecycle flags have been modified
     * since this entity instance creation.
     *
     * <p>This method is used internally to determine whether this entity instance should be
     * stored or the storage update can be skipped for this instance.
     *
     * @return {@code true} if the state or flags have been modified, {@code false} otherwise.
     */
    @Internal
    public boolean isChanged() {
        boolean lifecycleFlagsChanged = lifecycleFlagsChanged();
        Transaction<?, ?, ?, ?> tx = this.transaction;
        boolean stateChanged = tx != null
                               ? tx.isStateChanged()
                               : this.stateChanged;
        return stateChanged || lifecycleFlagsChanged;
    }

    /**
     * Obtains the instance of the state builder.
     *
     * <p>This method must be called only from within an active transaction.
     *
     * @return the instance of the new state builder
     * @throws IllegalStateException if the method is called not within a transaction
     */
    protected B getBuilder() {
        return tx().getBuilder();
    }

    /**
     * Ensures that the entity has non-null and active transaction.
     *
     * @throws IllegalStateException if the transaction is null or not active
     */
    @SuppressWarnings("ConstantConditions") // see Javadoc
    private Transaction<I, ? extends TransactionalEntity<I, S, B>, S, B> ensureTransaction() {
        if (!isTransactionInProgress()) {
            throw new IllegalStateException(getMissingTxMessage());
        }
        return transaction;
    }

    /**
     * Provides error message text for the case of not having an active transaction when a state
     * modification call is made.
     */
    protected String getMissingTxMessage() {
        return "Cannot modify entity state: transaction is not available.";
    }

    Transaction<I, ? extends TransactionalEntity<I, S, B>, S, B> tx() {
        return ensureTransaction();
    }

    /**
     * Determines if the state update cycle is currently active.
     *
     * @return {@code true} if it is active, {@code false} otherwise
     */
    @VisibleForTesting
    boolean isTransactionInProgress() {
        Transaction<?, ?, ?, ?> tx = this.transaction;
        boolean result = tx != null && tx.isActive();
        return result;
    }

    @SuppressWarnings({"ObjectEquality", "ReferenceEquality"}
            /* The refs must to point to the same object; see below. */)
    void injectTransaction(Transaction<I, ? extends TransactionalEntity<I, S, B>, S, B> tx) {
        checkNotNull(tx);

        /*
            In order to be sure we are not hijacked, we must be sure that the transaction
            is injected to the very same object, wrapped into the transaction.
        */
        checkState(tx.getEntity() == this,
                   "Transaction injected to this " + this
                           + " is wrapped around a different entity: " + tx.getEntity());

        this.transaction = tx;
    }

    void releaseTransaction() {
        this.transaction = null;
    }

    @Nullable
    @VisibleForTesting
    Transaction<I, ? extends TransactionalEntity<I, S, B>, S, B> getTransaction() {
        return transaction;
    }

    /**
     * Updates own {@code stateChanged} flag from the underlying transaction.
     */
    void updateStateChanged() {
        this.stateChanged = tx().isStateChanged();
    }

    B builderFromState() {
        B builder = newBuilderInstance();
        builder.setOriginalState(getState());
        return builder;
    }

    /**
     * Sets an initial state for the entity.
     *
     * <p>The execution of this method requires a {@linkplain #isTransactionInProgress() presence
     * of active transaction}.
     */
    protected void setInitialState(S initialState, Version version) {
        tx().initAll(initialState, version);
    }

    /**
     * Obtains the current state of the entity lifecycle flags.
     *
     * <p>If the transaction is in progress, returns the lifecycle flags value for the transaction.
     */
    @Override
    public LifecycleFlags getLifecycleFlags() {
        if (isTransactionInProgress()) {
            return tx().getLifecycleFlags();
        }
        return super.getLifecycleFlags();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The execution of this method requires a {@linkplain #isTransactionInProgress() presence
     * of active transaction}.
     */
    @Override
    protected void setArchived(boolean archived) {
        tx().setArchived(archived);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The execution of this method requires a {@linkplain #isTransactionInProgress() presence
     * of active transaction}.
     */
    @Override
    protected void setDeleted(boolean deleted) {
        tx().setDeleted(deleted);
    }

    private B newBuilderInstance() {
        @SuppressWarnings("unchecked")   // it's safe, as we rely on the definition of this class.
        Class<? extends TransactionalEntity<I, S, B>> cls =
                (Class<? extends TransactionalEntity<I, S, B>>) getClass();
        Class<B> builderClass = TypeInfo.getBuilderClass(cls);
        B builder = ValidatingBuilders.newInstance(builderClass);
        return builder;
    }

    /**
     * Enumeration of generic type parameters of this class.
     */
    enum GenericParameter implements GenericTypeIndex<TransactionalEntity> {

        /** The index of the generic type {@code <I>}. */
        ID(0),

        /** The index of the generic type {@code <S>}. */
        STATE(1),

        /** The index of the generic type {@code <B>}. */
        STATE_BUILDER(2);

        private final int index;

        GenericParameter(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return this.index;
        }
    }

    /**
     * Provides type information on classes extending {@code TransactionalEntity}.
     */
    static class TypeInfo {

        /**
         * Prevents instantiation.
         */
        private TypeInfo() {
        }

        /**
         * Obtains the class of the {@linkplain ValidatingBuilder} for the given
         * {@code TransactionalEntity} descendant class {@code entityClass}.
         */
        private static <I,
                        S extends Message,
                        B extends ValidatingBuilder<S, ? extends Message.Builder>>
        Class<B> getBuilderClass(Class<? extends TransactionalEntity<I, S, B>> entityClass) {
            checkNotNull(entityClass);
            @SuppressWarnings("unchecked") // The type is ensured by this class declaration.
            Class<B> builderClass = (Class<B>) STATE_BUILDER.getArgumentIn(entityClass);
            return builderClass;
        }
    }
}
