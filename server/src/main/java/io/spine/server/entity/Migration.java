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
import io.spine.base.Identifier;
import io.spine.core.MessageId;
import io.spine.core.Version;
import io.spine.system.server.event.MigrationApplied;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Time.currentTime;
import static io.spine.core.Versions.increment;

@Experimental
public abstract class Migration<I, S extends EntityState, E extends TransactionalEntity<I, S, ?>>
        implements Function<S, S> {

    private boolean archive;
    private boolean delete;
    private boolean physicallyRemoveRecord;

    private @MonotonicNonNull E entity;

    @Internal
    final void applyTo(E entity, RecordBasedRepository<I, E, S> repository) {
        this.entity = entity;

        Transaction<I, E, S, ?> tx = startTransaction(entity);
        I id = entity.id();
        EntityLifecycleMonitor<I> monitor =
                EntityLifecycleMonitor.newInstance(repository, id);
        MessageId entityId = MessageId
                .newBuilder()
                .setId(Identifier.pack(entity.id()))
                .setTypeUrl(repository.entityStateType().value())
                .vBuild();
        MigrationApplied migrationApplied = MigrationApplied
                .newBuilder()
                .setEntity(entityId)
                .setWhen(currentTime())
                .build();

        repository.lifecycleOf(id)
                  .onMigrationApplied();

//        monitor.setLastMessage(migrationApplied);
        tx.setListener(monitor);

        S oldState = entity.state();
        S newState = apply(oldState);
        if (physicallyRemoveRecord) {
            tx.commit();
            return;
        }
        if (!oldState.equals(newState)) {
            entity.updateState(newState, increment(version()));
        }
        if (archive) {
            entity.setArchived(true);
        }
        if (delete) {
            entity.setDeleted(true);
        }
        tx.commit();
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

    public final void markArchived() {
        archive = true;
    }

    public final void markDeleted() {
        delete = true;
    }

    public final void removeFromStorage() {
        physicallyRemoveRecord = true;
    }

    final boolean physicallyRemoveRecord() {
        return physicallyRemoveRecord;
    }

    protected abstract Transaction<I, E, S, ?> startTransaction(E entity);

    private void checkAmidstApplyingToEntity() {
        checkNotNull(entity,
                     "This method should only be invoked from `apply(S)` method.");
    }
}
