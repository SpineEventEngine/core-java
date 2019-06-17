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
package io.spine.server.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.core.Version;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.server.entity.TransactionListener.SilentWitness;
import io.spine.validate.NonValidated;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.lang.String.format;

/**
 * The abstract class for the {@linkplain TransactionalEntity entity} transactions.
 *
 * <p>The transaction is a set of changes made to an entity state or entity attributes
 * (e.g. version, lifecycle flags etc).
 *
 * <p>Serves as a buffer, accumulating the changes, intended for the enclosed {@code Entity};
 * the changes are only applied to the actual object upon {@linkplain #commit() commit}.
 *
 * <p>The transaction is injected to the entity, which state should be modified. By doing so,
 * the {@linkplain Transaction#builder() "buffering" builder} is exposed to concrete
 * {@code TransactionalEntity} subclasses. In turn, they receive an ability to change the entity
 * state by modifying {@link TransactionalEntity#builder() entity state builder}.
 *
 * <p>Same applies to the entity lifecycle flags.
 *
 * <p>Version management is performed automatically by the transaction itself.
 *
 * @param <I>
 *         the type of entity IDs
 * @param <E>
 *         the type of entity
 * @param <S>
 *         the type of entity state
 * @param <B>
 *         the type of a {@code ValidatingBuilder} for the entity state
 */
@SuppressWarnings("ClassWithTooManyMethods")
@Internal
public abstract class Transaction<I,
                                  E extends TransactionalEntity<I, S, B>,
                                  S extends Message,
                                  B extends ValidatingBuilder<S>> {

    /**
     * The entity, which state and attributes are modified in this transaction.
     */
    private final E entity;

    /**
     * The state of the entity before the beginning of the transaction.
     */
    private final S initialState;

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
     * The {@link EntityRecord} containing the entity data and meta-info before the transaction
     * start.
     */
    private final EntityRecord entityBeforeTransaction;

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
    @SuppressWarnings("UnusedAssignment") // is used to make the initial tx. state explicit.
    private boolean active = false;

    /**
     * An ordered collection of the phases that were propagated in scope of this transaction.
     *
     * <p>Contains all the phases, including failed.
     */
    private final List<Phase<I, ?>> phases = newLinkedList();

    private TransactionListener<I> transactionListener;

    /**
     * Creates a new instance of {@code Transaction} and
     * {@linkplain TransactionalEntity#injectTransaction(Transaction) injects} the newly created
     * transaction into the given {@code entity}.
     *
     * <p>The entity state and attributes are set as starting values for this transaction.
     *
     * @param entity
     *         the entity to create the transaction for
     * @see TransactionListener
     */
    protected Transaction(E entity) {
        this.entity = checkNotNull(entity);
        this.initialState = entity.state();
        this.builder = toBuilder(entity);
        this.version = entity.version();
        this.lifecycleFlags = entity.lifecycleFlags();
        this.active = true;

        this.transactionListener = new SilentWitness<>();
        injectTo(entity);
        this.entityBeforeTransaction = entityRecord();
    }

    /**
     * Creates the builder for being used by a transaction when modifying the passed entity.
     *
     * <p>If the entity has the default state, and the first field of the state is its ID, and
     * the field is required, initializes the builder with the value of the entity ID.
     */
    @VisibleForTesting
    static <I,
            E extends TransactionalEntity<I, S, B>,
            S extends Message,
            B extends ValidatingBuilder<S>>
    B toBuilder(E entity) {
        S currentState = entity.state();
        @SuppressWarnings("unchecked") // ensured by argument of <E>.
        B result = (B) currentState.toBuilder();

        if (currentState.equals(entity.defaultState())) {
            IdField idField = IdField.of(entity.modelClass());
            idField.initBuilder(result, entity.id());
        }
        return result;
    }

    /**
     * Acts similar to {@linkplain Transaction#Transaction(TransactionalEntity)
     * an overloaded ctor}, but instead of using the original entity state and version,
     * this transaction will have the passed state and version as a starting point.
     *
     * <p>Note, that the given {@code state} and {@code version} are applied to the actual entity
     * upon commit.
     *
     * @param entity
     *         the target entity to modify within this transaction
     * @param state
     *         the entity state to set
     * @param version
     *         the entity version to set
     */
    protected Transaction(E entity, S state, Version version) {
        this(entity);
        initAll(state, version);
    }

    /**
     * Allows to understand whether this transaction is active.
     *
     * @return {@code true} if the transaction is active, {@code false} otherwise
     */
    @VisibleForTesting
    public boolean isActive() {
        return active;
    }

    LifecycleFlags lifecycleFlags() {
        return lifecycleFlags;
    }

    protected E entity() {
        return entity;
    }

    /**
     * Returns the version of the entity, modified within this transaction.
     */
    Version version() {
        return version;
    }

    List<Phase<I, ?>> phases() {
        return ImmutableList.copyOf(phases);
    }

    /**
     * Propagates a phase and performs a rollback in case of an exception.
     *
     * <p>The transaction {@linkplain #listener() listener} is called for both failed and
     * successful phases.
     *
     * @param phase
     *         the phase to propagate
     * @param <R>
     *         the type of the phase propagation result
     * @return the phase propagation result
     */
    @CanIgnoreReturnValue
    protected <R> R propagate(Phase<I, R> phase) {
        TransactionListener<I> listener = listener();
        listener.onBeforePhase(phase);
        try {
            return phase.propagate();
        } catch (Throwable t) {
            rollback(t);
            throw illegalStateWithCauseOf(t);
        } finally {
            phases.add(phase);
            listener.onAfterPhase(phase);
        }
    }

    /**
     * Applies all the outstanding modifications to the enclosed entity.
     *
     * @throws InvalidEntityStateException
     *         in case the new entity state is not valid
     * @throws IllegalStateException
     *         in case of a generic error
     */
    protected void commit() throws InvalidEntityStateException, IllegalStateException {
        B builder = builder();

        S newState = builder.buildPartial();
        if (initialState.equals(newState)) {
            commitUnchangedState();
        } else {
            commitChangedState(newState);
        }
    }

    /**
     * Commits this transaction and sets the new state to the entity.
     *
     * <p>In case if the commit is failed, the transaction is rolled back and the entity keeps
     * the current state.
     *
     * @param newState
     *         the new state of the entity
     */
    private void commitChangedState(@NonValidated S newState) {
        try {
            markStateChanged();
            Version pendingVersion = version();
            beforeCommit(newState, pendingVersion);
            entity.updateState(newState, pendingVersion);
            commitAttributeChanges();
            EntityRecord newRecord = entityRecord();
            afterCommit(newRecord);
        } catch (InvalidEntityStateException e) {
            /* New state of the entity does not pass validation. */
            rollback(e);
            //TODO:2019-06-16:alex.tymchenko: do we need to throw InvalidEntityStateException?
            //throw e;
        } catch (RuntimeException e) {
            /* Exception occurred during execution of a handler method. */
            rollback(e);
            throw illegalStateWithCauseOf(e);
        } finally {
            releaseTx();
        }
    }

    /**
     * Commits this transaction skipping the entity state update.
     *
     * <p>This method is called when none of the transaction phases has changed the entity state.
     */
    private void commitUnchangedState() {
        S unchanged = entity().state();
        Version pendingVersion = version();
        beforeCommit(unchanged, pendingVersion);
        if (!pendingVersion.equals(entity.version())) {
            entity.updateState(unchanged, pendingVersion);
        }
        commitAttributeChanges();
        releaseTx();
        EntityRecord newRecord = entityRecord();
        afterCommit(newRecord);
    }

    private void beforeCommit(S newState, Version newVersion) {
        LifecycleFlags newFlags = lifecycleFlags();
        @NonValidated EntityRecord record = EntityRecord
                .newBuilder()
                .setEntityId(Identifier.pack(entity.id()))
                .setState(pack(newState))
                .setLifecycleFlags(newFlags)
                .setVersion(newVersion)
                .buildPartial();
        transactionListener.onBeforeCommit(record);
    }

    private void afterCommit(EntityRecord newEntity) {
        EntityRecordChange change = EntityRecordChange
                .newBuilder()
                .setPreviousValue(entityBeforeTransaction)
                .setNewValue(newEntity)
                .build();
        transactionListener.onAfterCommit(change);
    }

    /**
     * Cancels the changes made within this transaction and removes the injected transaction object
     * from the enclosed entity.
     *
     * @param cause
     *         the reason of the rollback
     */
    void rollback(Throwable cause) {
        beforeRollback(cause);
        TransactionListener<I> listener = listener();
        @NonValidated EntityRecord record = EntityRecord
                .newBuilder()
                .setEntityId(Identifier.pack(entity.id()))
                .setState(pack(currentBuilderState()))
                .setVersion(version)
                .setLifecycleFlags(lifecycleFlags())
                .buildPartial();
        listener.onTransactionFailed(cause, record);
        this.active = false;
        entity.releaseTransaction();
    }

    /**
     * Does necessary preparations before the transaction is marked as inactive and released.
     */
    @SuppressWarnings("NoopMethodInAbstractClass")
    // Do not force descendants to override the method.
    protected void beforeRollback(Throwable cause) {
        // NO-OP.
    }

    /**
     * Creates an {@link EntityRecord} for the entity under transaction.
     *
     * @return new {@link EntityRecord}
     */
    private EntityRecord entityRecord() {
        E entity = entity();
        Any entityId = Identifier.pack(entity.id());
        Version version = entity.version();
        Any state = pack(entity.state());
        LifecycleFlags lifecycleFlags = entity.lifecycleFlags();
        return EntityRecord
                .newBuilder()
                .setEntityId(entityId)
                .setVersion(version)
                .setState(state)
                .setLifecycleFlags(lifecycleFlags)
                .build();
    }

    private @NonValidated S currentBuilderState() {
        return builder.buildPartial();
    }

    private void releaseTx() {
        this.active = false;
        entity.releaseTransaction();
    }

    /**
     * Applies lifecycle flag modifications to the entity under transaction.
     */
    protected void commitAttributeChanges() {
        entity.setLifecycleFlags(lifecycleFlags());
        entity.updateStateChanged();
    }

    void initAll(S state, Version version) {
        B builder = builder();
        builder.clear();
        builder.mergeFrom(state);
        initVersion(version);
    }

    /**
     * Obtains the builder for the current transaction.
     */
    B builder() {
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
        Transaction<I, E, S, B> tx = this;
        entity.injectTransaction(tx);
    }

    void setVersion(Version version) {
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
     * @param version
     *         the version to set.
     */
    private void initVersion(Version version) {
        checkNotNull(version);

        int versionNumber = this.version.getNumber();
        if (versionNumber > 0) {
            String errMsg = format(
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
     */
    private TransactionListener<I> listener() {
        return transactionListener;
    }

    /**
     * Injects a {@linkplain TransactionListener listener} into this transaction.
     *
     * <p>Each next invocation overrides the previous one.
     *
     * @param listener
     *         the listener to use in this transaction
     */
    public void setListener(TransactionListener<I> listener) {
        checkNotNull(listener);
        this.transactionListener = listener;
    }

    public void setArchived(boolean archived) {
        lifecycleFlags = lifecycleFlags.toBuilder()
                                       .setArchived(archived)
                                       .build();
    }

    public void setDeleted(boolean deleted) {
        lifecycleFlags = lifecycleFlags.toBuilder()
                                       .setDeleted(deleted)
                                       .build();
    }
}
