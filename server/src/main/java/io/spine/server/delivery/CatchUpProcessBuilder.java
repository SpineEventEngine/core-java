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

package io.spine.server.delivery;

import io.spine.server.delivery.CatchUpProcess.DispatchCatchingUp;
import io.spine.server.delivery.CatchUpProcess.RepositoryIndex;
import io.spine.server.event.EventStore;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A builder for {@link CatchUpProcess}.
 */
public final class CatchUpProcessBuilder<I> {

    private final Class<I> idClass;
    private final TypeUrl projectionStateType;
    private @Nullable Supplier<EventStore> eventStore;
    private @Nullable CatchUpStorage storage;
    private @Nullable RepositoryIndex<I> index;
    private @Nullable DispatchCatchingUp<I> dispatchOp;

    CatchUpProcessBuilder(Class<I> aClass, TypeUrl type) {
        idClass = aClass;
        projectionStateType = type;
    }

    public Class<I> idClass() {
        return idClass;
    }

    public TypeUrl projectionStateType() {
        return projectionStateType;
    }

    public Optional<Supplier<EventStore>> getEventStore() {
        return Optional.ofNullable(eventStore);
    }
    public Supplier<EventStore> eventStore() {
        return checkNotNull(eventStore);
    }

    public CatchUpProcessBuilder<I> withEventStore(Supplier<EventStore> eventStore) {
        this.eventStore = checkNotNull(eventStore);
        return this;
    }

    Optional<CatchUpStorage> getStorage() {
        return Optional.ofNullable(storage);
    }

    CatchUpStorage storage() {
        return checkNotNull(storage);
    }

    CatchUpProcessBuilder<I> withStorage(CatchUpStorage storage) {
        this.storage = checkNotNull(storage);
        return this;
    }

    public Optional<RepositoryIndex<I>> getIndex() {
        return Optional.ofNullable(index);
    }

    public RepositoryIndex<I> index() {
        return checkNotNull(index);
    }

    public CatchUpProcessBuilder<I> withIndex(RepositoryIndex<I> index) {
        this.index = checkNotNull(index);
        return this;
    }

    public Optional<DispatchCatchingUp<I>> getDispatchOp() {
        return Optional.ofNullable(dispatchOp);
    }

    public  DispatchCatchingUp<I> dispatchOp() {
        return checkNotNull(dispatchOp);
    }

    public CatchUpProcessBuilder<I> withDispatchOp(DispatchCatchingUp<I> operation) {
        this.dispatchOp = checkNotNull(operation);
        return this;
    }

    public CatchUpProcess<I> build() {
        checkNotNull(eventStore);
        checkNotNull(storage);
        checkNotNull(index);
        checkNotNull(dispatchOp);
        return new CatchUpProcess<>(this);
    }
}
