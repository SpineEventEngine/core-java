/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.Version;
import io.spine.server.entity.TransactionListener.SilentWitness;
import io.spine.validate.AbstractValidatingBuilder;
import io.spine.validate.ValidatingBuilder;
import io.spine.validate.ValidationException;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.core.Versions.checkIsIncrement;
import static io.spine.server.entity.EntityVersioning.FROM_EVENT;
import static io.spine.server.entity.InvalidEntityStateException.onConstraintViolations;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.lang.String.format;

/**
 * The abstract class for the {@linkplain EventPlayingEntity} transactions.
 *
 * <p>The transaction is a set of changes made to an entity state or entity attributes
 * (e.g. version, lifecycle flags etc).
 *
 * <p>Serves as a buffer, accumulating the changes, intended for the enclosed {@code Entity};
 * the changes are only applied to the actual object upon {@linkplain #commit() commit}.
 *
 * <p>The transaction is injected to the entity, which state should be modified. By doing so,
 * the {@linkplain Transaction#getBuilder() "buffering" builder} is exposed to concrete
 * {@code EventPlayingEntity} subclasses. In turn, they receive an ability to change the entity
 * state by modifying {@link EventPlayingEntity#getBuilder() entity state builder}.
 *
 * <p>Same applies to the entity lifecycle flags.
 *
 * <p>Version management is performed automatically by the transaction itself. Each event message
 * applied leads to the version increment.
 *
 * @param <I> the type of entity IDs
 * @param <E> the type of entity
 * @param <S> the type of entity state
 * @param <B> the type of a {@code ValidatingBuilder} for the entity state
 * @author Alex Tymchenko
 */
@Internal
public abstract class Transaction<I,
                                  E extends EventPlayingEntity<I, S, B>,
                                  S extends Message,
                                  B extends ValidatingBuilder<S, ? extends Message.Builder>> {

    private final TransactionListener<I, E, S, B> listener;

    /**
     * The entity, which state and attributes are modified in this transaction.
     */
    private final E entity;

    /**
     * The builder for the entity state.
     *
     * <p>All the state changes made within the transaction go to this {@code Builder},
     * and not to the {@code Entity} itself.
     *
     * <p>The state of the builder is merged to the entity state
     * upon the {@linkplain #commit() commit()}.
     */
    private final B builder;

    /**
     * The version of the entity, modified within this transaction.
     *
     * <p>All the version changes made within the transaction are stored in this variable,
     * and not in the {@code Entity} itself.
     *
     * <p>This value is propagated to the entity upon the {@linkplain #commit() commit()}.
     */
    private Version version;

    /**
     * The lifecycle flags of the entity, modified within this transaction.
     *
     * <p>All the entity lifecycle changes made within the transaction are stored in this variable,
     * and not in the {@code Entity} itself.
     *
     * <p>This value is set to the entity upon the {@linkplain #commit() commit()}.
     */
    private LifecycleFlags lifecycleFlags;

    /**
     * The flag, which becomes {@code true}, if the state of the entity
     * {@linkplain #commit() has been changed} since it has been
     * {@linkplain RecordBasedRepository#findOrCreate(Object)} loaded or created.
     */
    private boolean stateChanged;

    /**
     * Allows to understand whether this transaction is active.
     *
     * <p>Has {@code true} value since the transaction instance creation
     * until {@linkplain #commit() commit()} is performed.
     */
    private boolean active = false;

    /**
     * An ordered collection of the phases that were propagated in scope of this transaction.
     *
     * <p>Contains all the phases, including failed.
     */
    private final List<Phase<I, E, S, B>> phases = newLinkedList();

    /**
     * Creates a new instance of {@code Transaction} and
     * {@linkplain EventPlayingEntity#injectTransaction(Transaction) injects} the newly created
     * transaction into the given {@code entity}.
     *
     * <p>The entity state and attributes are set as starting values for this transaction.
     *
     * @param entity   the entity to create the transaction for
     * @param listener the transaction listener
     * @see TransactionListener
     */
    protected Transaction(E entity, TransactionListener<I, E, S, B> listener) {
        checkNotNull(entity);
        checkNotNull(listener);

        this.entity = entity;
        this.builder = entity.builderFromState();
        this.version = entity.getVersion();
        this.lifecycleFlags = entity.getLifecycleFlags();
        this.active = true;
        this.listener = listener;

        injectTo(entity);
    }

    /**
     * Acts similar to
     * {@linkplain Transaction#Transaction(EventPlayingEntity, TransactionListener)
     * Transaction(EventPlayingEntity, TransactionListener)}, but passes an instance of
     * {@link SilentWitness} as a listener.
     *
     * @param entity the entity to create the transaction for.
     */
    protected Transaction(E entity) {
        this(entity, new SilentWitness<I, E, S, B>());
    }

    /**
     * Acts similar to {@linkplain Transaction#Transaction(EventPlayingEntity)
     * an overloaded ctor}, but instead of using the original entity state and version,
     * this transaction will have the passed state and version as a starting point.
     *
     * <p>Note, that the given {@code state} and {@code version} are applied to the actual entity
     * upon commit.
     *
     * @param entity   the target entity to modify within this transaction
     * @param state    the entity state to set
     * @param version  the entity version to set
     * @param listener the transaction listener
     */
    protected Transaction(E entity, S state, Version version,
                          TransactionListener<I, E, S, B> listener) {
        this(entity, listener);
        initAll(state, version);
    }

    /**
     * Acts similar to {@linkplain Transaction#Transaction(EventPlayingEntity,
     * Message, Version, TransactionListener) an overloaded ctor},
     * but passes an instance of {@link SilentWitness} as a listener.
     *
     * @param entity  the target entity to modify within this transaction
     * @param state   the entity state to set
     * @param version the entity version to set
     */
    protected Transaction(E entity, S state, Version version) {
        this(entity, state, version, new SilentWitness<I, E, S, B>());
    }

    /**
     * Dispatches the event message and its context to the current entity-in-transaction.
     *
     * <p>This operation is always performed in scope of an active transaction.
     *
     * @param entity  the target entity
     * @param event   the event to dispatch
     */
    protected abstract void dispatch(E entity, EventEnvelope event);

    /**
     * Allows to understand whether this transaction is active.
     *
     * @return {@code true} if the transaction is active, {@code false} otherwise
     */
    @VisibleForTesting
    public boolean isActive() {
        return active;
    }

    LifecycleFlags getLifecycleFlags() {
        return lifecycleFlags;
    }

    @VisibleForTesting
    E getEntity() {
        return entity;
    }

    /**
     * @return the version of the entity, modified within this transaction
     */
    Version getVersion() {
        return version;
    }

    List<Phase<I, E, S, B>> getPhases() {
        return ImmutableList.copyOf(phases);
    }

    /**
     * Applies all the outstanding modifications to the enclosed entity.
     *
     * @throws InvalidEntityStateException in case the new entity state is not valid
     * @throws IllegalStateException       in case of a generic error
     */
    protected void commit() throws InvalidEntityStateException, IllegalStateException {

        final TransactionListener<I, E, S, B> listener = getListener();
        final B builder = getBuilder();
        final Version pendingVersion = getVersion();

        // The state is only updated, if at least some changes were made to the builder.
        if (builder.isDirty()) {
            try {
                final S newState = builder.build();
                markStateChanged();

                listener.onBeforeCommit(getEntity(), newState,
                                        pendingVersion, getLifecycleFlags());

                entity.updateState(newState, pendingVersion);
                commitAttributeChanges();
            } catch (ValidationException exception) {  /* Could only happen if the state
                                                                   has been injected not using
                                                                   the builder setters. */
                final InvalidEntityStateException invalidStateException = of(exception);
                rollback(invalidStateException);

                throw invalidStateException;
            } catch (RuntimeException genericException) {
                rollback(genericException);
                throw illegalStateWithCauseOf(genericException);
            } finally {
                releaseTx();
            }
        } else {
            // The state isn't modified, but other attributes may have been modified.
            final S unmodifiedState = getEntity().getState();
            listener.onBeforeCommit(getEntity(), unmodifiedState,
                                    pendingVersion, getLifecycleFlags());

            // Set the version if it has changed.
            if(!pendingVersion.equals(entity.getVersion())) {
                entity.updateState(unmodifiedState, pendingVersion);
            }
            commitAttributeChanges();
            releaseTx();
        }
    }

    /**
     * Cancels the changes made within this transaction and removes the injected transaction object
     * from the enclosed entity.
     *
     * @param cause the reason of the rollback
     */
    void rollback(Throwable cause) {
        final S currentState = currentBuilderState();
        final TransactionListener<I, E, S, B> listener = getListener();
        listener.onTransactionFailed(cause, getEntity(), currentState,
                                          getVersion(), getLifecycleFlags());
        this.active = false;
        entity.releaseTransaction();
    }

    /**
     * Creates a new {@linkplain Phase transaction phase} for the given event
     * and propagates the phase.
     *
     * <p>In case of an exception, the {@linkplain #rollback(Throwable) transaction rollback}
     * is performed.
     *
     * @param event the envelope with the event
     * @return this instance of the transaction
     * @see Transaction#apply(EventEnvelope)
     */
    @SuppressWarnings("OverlyBroadCatchBlock")  // to `rollback(..)` in case of any exception.
    Transaction<I, E, S, B> apply(EventEnvelope event) {
        final Phase<I, E, S, B> phase = new Phase<>(this, event);

        Phase<I, E, S, B> appliedPhase = null;
        try {
            appliedPhase = phase.propagate();
        } catch (Throwable t) {
            rollback(t);
            throw illegalStateWithCauseOf(t);
        } finally {
            final Phase<I, E, S, B> phaseToAdd = appliedPhase == null ? phase : appliedPhase;
            phases.add(phaseToAdd);

            getListener().onAfterPhase(phaseToAdd);
        }

        return this;
    }

    private InvalidEntityStateException of(ValidationException exception) {
        final Message invalidState = currentBuilderState();
        return onConstraintViolations(invalidState, exception.getConstraintViolations());
    }

    private S currentBuilderState() {
        @SuppressWarnings("unchecked")  // OK, as `AbstractValidatingBuilder` is the only subclass.
        final AbstractValidatingBuilder<S, ?> abstractBuilder =
                (AbstractValidatingBuilder<S, ?>) builder;
        return abstractBuilder.internalBuild();
    }

    private void releaseTx() {
        this.active = false;
        entity.releaseTransaction();
    }

    private void commitAttributeChanges() {
        entity.setLifecycleFlags(getLifecycleFlags());
        entity.updateStateChanged();
    }

    void initAll(S state, Version version) {
        final B builder = getBuilder();
        builder.clear();
        builder.mergeFrom(state);
        initVersion(version);
    }

    /**
     * Obtains the builder for the current transaction.
     */
    B getBuilder() {
        return builder;
    }

    /**
     * Allows to determine, whether this transaction is active or not.
     */
    boolean isStateChanged() {
        return stateChanged;
    }

    private void markStateChanged() {
        this.stateChanged = true;
    }

    /**
     * Injects the current transaction instance into an entity.
     */
    private void injectTo(E entity) {
        // assigning `this` to a variable to explicitly specify
        // the generic bounds for Java compiler.
        final Transaction<I, E, S, B> tx = this;
        entity.injectTransaction(tx);
    }

    private void setVersion(Version version) {
        checkNotNull(version);
        this.version = version;
    }

    /**
     * Initializes the entity with the passed version.
     *
     * <p>This method assumes that the entity version is zero.
     * If this is not so, {@code IllegalStateException} will be thrown.
     *
     * <p>One of the usages for this method is for creating an entity instance
     * from a storage.
     *
     * @param version the version to set.
     */
    private void initVersion(Version version) {
        checkNotNull(version);

        final int versionNumber = this.version.getNumber();
        if (versionNumber > 0) {
            final String errMsg = format(
                    "initVersion() called on an entity with non-zero version number (%d).",
                    versionNumber
            );
            throw new IllegalStateException(errMsg);
        }
        setVersion(version);
    }

    /**
     * Obtains an instance of the {@code TransactionListener} for this transaction.
     *
     * <p>By default, the returned listener {@linkplain SilentWitness does nothing}.
     *
     * <p>Descendant classes may override this method to specify a custom listener implementation.
     */
    private TransactionListener<I, E, S, B> getListener() {
        return listener;
    }

    public void setArchived(boolean archived) {
        lifecycleFlags = lifecycleFlags.toBuilder().setArchived(archived).build();
    }

    public void setDeleted(boolean deleted) {
        lifecycleFlags = lifecycleFlags.toBuilder().setDeleted(deleted).build();
    }

    /**
     * Retrieves the {@link EntityVersioning} of current {@code Transaction}.
     *
     * <p>The value should be constant among all the instances of a certain (runtime) type.
     *
     * <p>By default the version is taken from
     * the {@linkplain EntityVersioning#FROM_EVENT latest event applied}.
     */
    protected EntityVersioning versioningStrategy() {
        return FROM_EVENT;
    }

    /**
     * A stage of transaction, which is created by applying a single event (i.e. its message along
     * with the context) to the entity.
     *
     * <p>Invokes an event applier method for the entity modified in scope of the underlying
     * transaction, passing the event data to it. If such an invocation is successful,
     * an entity version is incremented in scope of the transaction.
     *
     * @param <I> the type of entity IDs
     * @param <E> the type of entity
     * @param <S> the type of entity state
     * @param <B> the type of a {@code ValidatingBuilder} for the entity state
     */
    protected static class Phase<I,
                                 E extends EventPlayingEntity<I, S, B>,
                                 S extends Message,
                                 B extends ValidatingBuilder<S, ? extends Message.Builder>> {

        private final Transaction<I, E, S, B> underlyingTransaction;
        private final EventEnvelope event;

        private boolean successful = false;

        private Phase(Transaction<I, E, S, B> transaction, EventEnvelope event) {
            this.underlyingTransaction = transaction;
            this.event = event;
        }

        /**
         * Invokes the event applier method and, if the invocation is successful, increments the
         * current entity version for this transaction. Also marks the current phase as successful.
         *
         * @return this instance of {@code Phase}
         */
        private Phase<I, E, S, B> propagate() {
            underlyingTransaction.dispatch(underlyingTransaction.getEntity(), event);
            advanceVersion();
            markSuccessful();
            return this;
        }

        private void advanceVersion() {
            final Version version = underlyingTransaction.versioningStrategy()
                                                         .nextVersion(this);
            checkIsIncrement(underlyingTransaction.getVersion(), version);
            underlyingTransaction.setVersion(version);
        }

        Transaction<I, E, S, B> getUnderlyingTransaction() {
            return underlyingTransaction;
        }

        private void markSuccessful() {
            this.successful = true;
        }

        boolean isSuccessful() {
            return successful;
        }

        protected Message getEventMessage() {
            return event.getMessage();
        }

        protected EventContext getContext() {
            return event.getEventContext();
        }
    }
}
