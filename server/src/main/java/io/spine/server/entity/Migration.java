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
import io.spine.annotation.Internal;
import io.spine.base.EntityState;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.logging.Logging;
import io.spine.system.server.event.MigrationApplied;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Optional;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Versions.increment;
import static io.spine.protobuf.Messages.isDefault;
import static io.spine.util.Exceptions.newIllegalStateException;

@Experimental
public abstract class Migration<I, S extends EntityState, E extends TransactionalEntity<I, S, ?>>
        implements Function<S, S>, Logging {

    private boolean archive;
    private boolean delete;
    private boolean physicallyRemoveRecord;

    private @MonotonicNonNull E entity;
    private @MonotonicNonNull RecordBasedRepository<I, E, S> repository;

    @Internal
    final void applyTo(E entity, RecordBasedRepository<I, E, S> repository) {
        this.entity = entity;
        this.repository = repository;

        Transaction<I, E, S, ?> tx = txWithLifecycleMonitor();
        S oldState = entity.state();
        S newState = apply(oldState);
        updateState(newState);
        updateLifecycle();
        tx.commit();
    }

    private void updateState(S newState) {
        if (!entity.state().equals(newState)) {
            entity.updateState(newState, increment(version()));
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

    public final void markArchived() {
        archive = true;
    }

    public final void markDeleted() {
        delete = true;
    }

    public final void removeFromStorage() {
        physicallyRemoveRecord = true;
    }

    protected final I id() {
        checkAmidstApplyingToEntity();
        return entity.id();
    }

    protected final Version version() {
        checkAmidstApplyingToEntity();
        return entity.version();
    }

    protected final boolean isArchived() {
        checkAmidstApplyingToEntity();
        return entity.isArchived();
    }

    protected final boolean isDeleted() {
        checkAmidstApplyingToEntity();
        return entity.isDeleted();
    }

    final boolean physicallyRemoveRecord() {
        return physicallyRemoveRecord;
    }

    private Transaction<I, E, S, ?> txWithLifecycleMonitor() {
        I id = entity.id();
        Transaction<I, E, S, ?> tx = startTransaction(entity);
        EntityLifecycleMonitor<I> monitor = configureLifecycleMonitor(id);
        tx.setListener(monitor);
        return tx;
    }

    /**
     * Will post lifecycle events as usual, assigning a {@link MigrationApplied} instance as last
     * handled message.
     */
    private EntityLifecycleMonitor<I> configureLifecycleMonitor(I id) {
        Optional<Event> posted = repository.lifecycleOf(id)
                                           .onMigrationApplied();
        Event migrationApplied = posted.orElseThrow(this::throwOnBlockingFilter);

        if (isDefault(migrationApplied)) {
            warnOnNoSystemEventsPosted();
            return EntityLifecycleMonitor.newInstance(repository, id);
        }
        return EntityLifecycleMonitor.withAcknowledgedMessage(repository, id, migrationApplied);
    }

    protected abstract Transaction<I, E, S, ?> startTransaction(E entity);

    private void checkAmidstApplyingToEntity() {
        checkNotNull(entity,
                     "This method should only be invoked from within `apply(S)` method.");
    }

    private IllegalStateException throwOnBlockingFilter() {
        throw newIllegalStateException(
                "The event filter of repository of type `%s` prevents system from posting " +
                        "the `%s` event. Re-configure an event filter by overriding the " +
                        "`Repository#eventFilter()` method if you want to apply the migration.",
                repository.getClass().getCanonicalName(),
                MigrationApplied.class.getCanonicalName()
        );
    }

    private void warnOnNoSystemEventsPosted() {
        _warn().log("The context uses a NO-OP system write side. " +
                            "No system events will be posted during the migration.");
    }
}
