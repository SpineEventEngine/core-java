/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import io.spine.annotation.Experimental;
import io.spine.base.EntityState;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.logging.Logging;
import io.spine.system.server.event.MigrationApplied;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Versions.increment;
import static io.spine.protobuf.Messages.isDefault;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A stored {@link Entity} transformation done to account for the domain model changes.
 *
 * <p>At its core the {@code Migration} is a mapping of {@link EntityState entity state}, from old
 * to new. It is also capable of performing basic entity modifications like
 * {@linkplain #markArchived() archiving} or {@linkplain #markDeleted() deleting} it.
 *
 * <p>The process of applying the migration operation is always preceded by an {@link Entity} load
 * by ID and may be finalized by either {@linkplain Repository#store(Entity) saving} the
 * transformed entity back into the storage or by {@linkplain #removeFromStorage() deleting} the
 * entity record if the migration is configured to do so.
 *
 * <p>All entity modifications are applied under the opened entity {@link Transaction}. The
 * last step of a migration operation is a transaction {@linkplain Transaction#commit() commit}. As
 * a consequence, all events related to the entity lifecycle occur as normal, having the
 * {@link MigrationApplied} event as the producing message.
 *
 * <p>To create a user-defined {@code Migration} in real life scenarios, consider inheriting from
 * {@link io.spine.server.projection.ProjectionMigration} or
 * {@link io.spine.server.procman.ProcessManagerMigration} type.
 *
 * @param <I>
 *         the entity ID type
 * @param <E>
 *         the entity type
 * @param <S>
 *         the entity state type
 */
@Experimental
public abstract class Migration<I, E extends TransactionalEntity<I, S, ?>, S extends EntityState>
        implements Function<S, S>, Logging {

    /**
     * The currently performed migration operation.
     */
    private @Nullable Operation<I, S, E> currentOperation;

    /**
     * Applies the migration to a given entity, starting a new migration
     * {@linkplain Operation operation}.
     */
    final void applyTo(E entity, RecordBasedRepository<I, E, S> repository) {
        currentOperation = new Operation<>(entity, repository);

        Transaction<I, E, S, ?> tx = txWithLifecycleMonitor();
        S oldState = entity.state();
        S newState = apply(oldState);
        currentOperation().updateState(newState);
        currentOperation().updateLifecycle();
        tx.commit();
    }

    /**
     * Marks the entity under migration as {@linkplain Entity#isArchived() archived}.
     */
    protected final void markArchived() {
        currentOperation().markArchived();
    }

    /**
     * Marks the entity under migration as {@linkplain Entity#isDeleted() deleted}.
     */
    protected final void markDeleted() {
        currentOperation().markDeleted();
    }

    /**
     * Configures the migration to delete the entity record from the storage.
     *
     * <p>The entity modifications are still applied, if any, allowing to trigger
     * {@linkplain EntityLifecycle entity lifecycle} events before the actual record deletion.
     */
    protected final void removeFromStorage() {
        currentOperation().removeFromStorage();
    }

    /**
     * Returns the ID of an entity under migration.
     */
    protected final I id() {
        return currentOperation().id();
    }

    /**
     * Returns the version of entity under migration.
     */
    protected final Version version() {
        return currentOperation().version();
    }

    /**
     * Returns {@code true} if the entity under migration is
     * {@linkplain Entity#isArchived() archived}.
     */
    protected final boolean isArchived() {
        return currentOperation().isArchived();
    }

    /**
     * Returns {@code true} if the entity under migration is
     * {@linkplain Entity#isDeleted() deleted}.
     */
    protected final boolean isDeleted() {
        return currentOperation().isDeleted();
    }

    final boolean physicallyRemoveRecord() {
        return currentOperation().physicallyRemoveRecord();
    }

    final void finishCurrentOperation() {
        currentOperation = null;
    }

    /**
     * Opens a transaction on an entity.
     */
    protected abstract Transaction<I, E, S, ?> startTransaction(E entity);

    /**
     * Opens a transaction with an {@link EntityLifecycleMonitor} as a {@link TransactionListener}.
     *
     * <p>The monitor is be configured to have a {@link MigrationApplied} instance as the last
     * handled message.
     */
    private Transaction<I, E, S, ?> txWithLifecycleMonitor() {
        E entity = currentOperation().entity;
        I id = entity.id();
        Transaction<I, E, S, ?> tx = startTransaction(entity);
        EntityLifecycleMonitor<I> monitor = configureLifecycleMonitor(id);
        tx.setListener(monitor);
        return tx;
    }

    /**
     * Creates an entity lifecycle monitor which will post lifecycle events as usual, assigning
     * a {@link MigrationApplied} instance as the event-producing message.
     */
    private EntityLifecycleMonitor<I> configureLifecycleMonitor(I id) {
        RecordBasedRepository<I, E, S> repository = currentOperation().repository;
        Optional<Event> posted = repository.lifecycleOf(id)
                                           .onMigrationApplied();
        Event migrationApplied = posted.orElseThrow(this::throwOnBlockingFilter);

        if (isDefault(migrationApplied)) {
            warnOnNoSystemEventsPosted();
            return EntityLifecycleMonitor.newInstance(repository, id);
        }
        return EntityLifecycleMonitor.withAcknowledgedMessage(repository, id, migrationApplied);
    }

    private IllegalStateException throwOnBlockingFilter() {
        throw newIllegalStateException(
                "The event filter of repository of type `%s` prevents system from posting " +
                        "the `%s` event. Re-configure an event filter by overriding the " +
                        "`Repository#eventFilter()` method if you want to apply the migration.",
                currentOperation().repository.getClass().getCanonicalName(),
                MigrationApplied.class.getCanonicalName()
        );
    }

    private void warnOnNoSystemEventsPosted() {
        _warn().log("The context uses a NO-OP system write side. " +
                            "No system events will be posted during the migration.");
    }

    private Operation<I, S, E> currentOperation() {
        return checkNotNull(currentOperation,
                            "Getter and mutator methods of migration should only be invoked " +
                                    "from within `apply(S)` metho`d.");
    }

    /**
     * A migration operation on an entity instance.
     */
    private static class Operation<I,
                                   S extends EntityState,
                                   E extends TransactionalEntity<I, S, ?>> {

        private boolean archive;
        private boolean delete;
        private boolean physicallyRemoveRecord;

        private final E entity;
        private final RecordBasedRepository<I, E, S> repository;

        private Operation(E entity, RecordBasedRepository<I, E, S> repository) {
            this.entity = entity;
            this.repository = repository;
        }

        private void updateState(S newState) {
            if (!entity.state().equals(newState)) {
                entity.updateState(newState, increment(entity.version()));
            }
        }

        private void updateLifecycle() {
            if (archive) {
                entity.setArchived(true);
            }
            if (delete) {
                entity.setDeleted(true);
            }
        }

        private void markArchived() {
            archive = true;
        }

        private void markDeleted() {
            delete = true;
        }

        private void removeFromStorage() {
            physicallyRemoveRecord = true;
        }

        private I id() {
            return entity.id();
        }

        private Version version() {
            return entity.version();
        }

        private boolean isArchived() {
            return archive;
        }

        private boolean isDeleted() {
            return delete;
        }

        private boolean physicallyRemoveRecord() {
            return physicallyRemoveRecord;
        }
    }
}
