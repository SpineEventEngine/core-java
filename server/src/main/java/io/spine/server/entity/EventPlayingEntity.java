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
package io.spine.server.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Event;
import io.spine.base.EventContext;
import io.spine.base.Version;
import io.spine.util.GenericTypeIndex;
import io.spine.validate.ValidatingBuilder;
import io.spine.validate.ValidatingBuilders;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.core.Events.getMessage;
import static io.spine.server.entity.EventPlayingEntity.GenericParameter.STATE_BUILDER;

/**
 * A base for entities, which can play {@linkplain Event events}.
 *
 * <p>Defines a transaction-based mechanism for state, version and lifecycle flags update.
 *
 * <p>Exposes {@linkplain #getBuilder()} validating builder} for the state as the only way
 * to modify the state from the descendants.
 *
 * @author Alex Tymchenko
 */
public abstract class EventPlayingEntity <I,
                                          S extends Message,
                                          B extends ValidatingBuilder<S, ? extends Message.Builder>>
                      extends AbstractVersionableEntity<I, S> {

    /**
     * The flag, which becomes {@code true}, if the state of the entity has been changed
     * since it has been {@linkplain RecordBasedRepository#findOrCreate(Object) loaded or created}.
     */
    private volatile boolean stateChanged;

    @Nullable
    private volatile Transaction<I, ? extends EventPlayingEntity<I, S, B>, S, B> transaction;

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the
     *                                  {@linkplain Entity supported types}
     */
    protected EventPlayingEntity(I id) {
        super(id);
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
        final boolean lifecycleFlagsChanged = lifecycleFlagsChanged();
        final boolean stateChanged = transaction != null
                ? transaction.isStateChanged()
                : this.stateChanged;

        return stateChanged || lifecycleFlagsChanged;
    }

    /**
     * Obtains the instance of the state builder.
     *
     * <p>This method must be called only from within an event applier.
     *
     * @return the instance of the new state builder
     * @throws IllegalStateException if the method is called from outside an event applier
     */
    protected B getBuilder() {
        return tx().getBuilder();
    }

    private void ensureTransaction() {
        if (!isTransactionInProgress()) {
            throw new IllegalStateException(
                    "Modification of state and lifecycle is not available. " +
                            "Make sure to modify those only from an event applier method.");
        }
    }

    private Transaction<I, ? extends EventPlayingEntity<I,S,B>, S, B> tx() {
        ensureTransaction();
        return transaction;
    }

    /**
     * Determines if the state update cycle is currently active.
     *
     * @return {@code true} if it is active, {@code false} otherwise
     */
    @VisibleForTesting
    boolean isTransactionInProgress() {
        final boolean result = transaction != null && transaction.isActive();
        return result;
    }

    /**
     * Plays the given events upon this entity.
     *
     * <p>Please note that the entity version is set according to the version of the event context.
     * Therefore, if the passed event(s) are in fact so called "integration" events and originated
     * from another application, they should be properly imported first.
     * Otherwise, the version conflict is possible.
     *
     * <p>Execution of this method requires the {@linkplain #isTransactionInProgress()
     * presence of active transaction}.
     *
     * @param events the events to play
     */
    protected void play(Iterable<Event> events) {
        final Transaction<I, ? extends EventPlayingEntity<I, S, B>, S, B> tx = tx();
        for (Event event : events) {
            final Message message = getMessage(event);
            final EventContext context = event.getContext();
            tx.apply(message, context);
        }
    }

    @SuppressWarnings("ObjectEquality") // the refs must to point to the same object; see below.
    void injectTransaction(Transaction<I, ? extends EventPlayingEntity<I, S, B>, S, B> tx) {
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
    Transaction<I, ? extends EventPlayingEntity<I, S, B>, S, B> getTransaction() {
        return transaction;
    }

    /**
     * Updates own {@code stateChanged} flag from the underlying transaction.
     */
    void updateStateChanged() {
        this.stateChanged = tx().isStateChanged();
    }

    B builderFromState() {
        final B builder = newBuilderInstance();
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
        if(isTransactionInProgress()) {
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
        final Class<? extends EventPlayingEntity<I, S, B>> aClass =
                (Class<? extends EventPlayingEntity<I, S, B>>)getClass();
        final Class<B> builderClass = TypeInfo.getBuilderClass(aClass);
        final B builder = ValidatingBuilders.newInstance(builderClass);
        return builder;
    }

    /**
     * Enumeration of generic type parameters of this class.
     */
    enum GenericParameter implements GenericTypeIndex<EventPlayingEntity> {

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

        @Override
        public Class<?> getArgumentIn(Class<? extends EventPlayingEntity> cls) {
            return Default.getArgument(this, cls);
        }
    }

    /**
     * Provides type information on classes extending {@code EventPlayingEntity}.
     */
    static class TypeInfo {

        private TypeInfo() {
            // Prevent construction from outside.
        }

        /**
         * Obtains the class of the {@linkplain ValidatingBuilder} for the given
         * {@code EventPlayingEntity} descendant class {@code entityClass}.
         */
        private static <I,
                        S extends Message,
                        B extends ValidatingBuilder<S, ? extends Message.Builder>>
        Class<B> getBuilderClass(Class<? extends EventPlayingEntity<I, S, B>> entityClass) {
            checkNotNull(entityClass);
            @SuppressWarnings("unchecked") // The type is ensured by this class declaration.
            final Class<B> builderClass = (Class<B>)STATE_BUILDER.getArgumentIn(entityClass);
            return builderClass;
        }
    }
}
