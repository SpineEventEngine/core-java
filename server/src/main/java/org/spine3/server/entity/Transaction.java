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
import org.spine3.annotation.Internal;
import org.spine3.base.EventContext;
import org.spine3.base.Version;
import org.spine3.base.Versions;
import org.spine3.validate.AbstractValidatingBuilder;
import org.spine3.validate.ConstraintViolationThrowable;
import org.spine3.validate.ValidatingBuilder;

import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.spine3.base.Versions.checkIsIncrement;
import static org.spine3.server.entity.InvalidEntityStateException.onConstraintViolations;

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
 * @author Alex Tymchenko
 */
@Internal
public abstract class Transaction<I,
                                  E extends EventPlayingEntity<I, S, B>,
                                  S extends Message,
                                  B extends ValidatingBuilder<S, ? extends Message.Builder>> {

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
    private volatile Version version;

    /**
     * The lifecycle flags of the entity, modified within this transaction.
     *
     * <p>All the entity lifecycle changes made within the transaction are stored in this variable,
     * and not in the {@code Entity} itself.
     *
     * <p>This value is set to the entity upon the {@linkplain #commit() commit()}.
     */
    private volatile LifecycleFlags lifecycleFlags;

    /**
     * The flag, which becomes {@code true}, if the state of the entity
     * {@linkplain #commit() has been changed} since it has been
     * {@linkplain RecordBasedRepository#findOrCreate(Object)} loaded or created.
     */
    private volatile boolean stateChanged;

    /**
     * Allows to understand whether this transaction is active.
     *
     * <p>Has {@code true} value since the transaction instance creation
     * until {@linkplain #commit() commit()} is performed.
     */
    private volatile boolean active = false;

    /**
     * Creates a new instance of {@code Transaction} and
     * {@linkplain EventPlayingEntity#injectTransaction(Transaction) injects} the newly created
     * transaction into the given {@code entity}.
     *
     * <p>The entity state and attributes are set as starting values for this transaction.
     *
     * @param entity the entity to create the transaction for
     */
    protected Transaction(E entity) {
        this.entity = entity;
        this.builder = entity.builderFromState();
        this.version = entity.getVersion();
        this.lifecycleFlags = entity.getLifecycleFlags();
        this.active = true;

        injectTo(entity);
    }

    /**
     * Acts similar to {@linkplain Transaction#Transaction(EventPlayingEntity) an overloaded ctor},
     * but instead of using the original entity state and version, this transaction will have
     * the passed state and version as a starting point.
     *
     * <p>Note, that the given {@code state} and {@code version} are applied to the actual entity
     * upon commit.
     *
     * @param entity  the target entity to modify within this transaction
     * @param state   the entity state to set
     * @param version the entity version to set
     */
    protected Transaction(E entity, S state, Version version) {
        this(entity);
        initAll(state, version);
    }

    /**
     * Applies the given event message with its context to the currently modified entity.
     *
     * <p>This operation is always performed in scope of an active transaction.
     *
     * <p>By implementing this method, descendants specify how exactly the event message
     * should be applied to the particular entity.
     *
     * @param eventMessage the event message
     * @param context      the event context
     * @throws InvocationTargetException if case of any issues while applying the event
     */
    protected abstract void apply(Message eventMessage, EventContext context)
            throws InvocationTargetException;

    /**
     * Allows to understand whether this transaction is active.
     *
     * @return {@code true} if the transaction is active, {@code false} otherwise
     */
    boolean isActive() {
        return active;
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

    /**
     * Applies all the outstanding state modifications to the state of enclosed entity.
     *
     * @throws InvalidEntityStateException in case the new entity state is not valid
     */
    @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException") // it is NOT ignored.
    protected void commit() throws InvalidEntityStateException {

        final B builder = getBuilder();

        // The state is only updated, if at least some changes were made to the builder.
        if (builder.isDirty()) {
            try {
                final S newState = builder.build();

                markStateChanged();
                entity.updateState(newState, version);
            } catch (ConstraintViolationThrowable exception) {
                // should only happen if the state has been injected not using the builder methods.

                final Message invalidState = ((AbstractValidatingBuilder) builder).internalBuild();
                throw onConstraintViolations(invalidState, exception.getConstraintViolations());
            } finally {
                this.active = false;
                commitAttributeChanges();

                entity.releaseTransaction();
            }
        }
    }

    private void commitAttributeChanges() {
        entity.setLifecycleFlags(lifecycleFlags);
        entity.setStateChanged(this.stateChanged);
    }

    //TODO:5/15/17:alex.tymchenko: make it work only if the state was empty before and its version was zero.
    void initAll(S state, Version version) {
        final B builder = getBuilder();
        builder.clear();
        builder.mergeFrom(state);
        initVersion(version);
    }

    Phase applyAnd(Message eventMessage, EventContext context) throws InvocationTargetException {
        apply(eventMessage, context);
        return new Phase(this);
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

    private void advanceVersion(Version newVersion) {
        checkNotNull(newVersion);
        checkIsIncrement(this.version, newVersion);
        setVersion(newVersion);
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
     * <p>To set a new version which is several numbers ahead please use
     * {@link #advanceVersion(Version)}.
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

    protected static class Phase {

        private final Transaction underlyingTransaction;

        private Phase(Transaction transaction) {
            this.underlyingTransaction = transaction;
        }

        Phase thenAdvanceVersionFrom(Version current) {
            underlyingTransaction.advanceVersion(Versions.increment(current));
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
