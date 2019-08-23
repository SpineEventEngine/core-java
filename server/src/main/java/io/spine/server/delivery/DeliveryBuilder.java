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

package io.spine.server.delivery;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Duration;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.memory.InMemoryShardedWorkRegistry;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A builder for {@code Delivery} instances.
 */
public final class DeliveryBuilder {

    private @MonotonicNonNull InboxStorage inboxStorage;
    private @MonotonicNonNull DeliveryStrategy strategy;
    private @MonotonicNonNull ShardedWorkRegistry workRegistry;
    private @MonotonicNonNull Duration idempotenceWindow;

    /**
     * Prevents a direct instantiation of this class.
     */
    DeliveryBuilder() {
    }

    /**
     * Returns the value of the configured {@code InboxStorage} or {@code Optional.empty()} if no
     * such value was configured.
     */
    public Optional<InboxStorage> inboxStorage() {
        return Optional.ofNullable(inboxStorage);
    }

    /**
     * Returns the non-{@code null} value of the configured {@code InboxStorage}.
     */
    InboxStorage getInboxStorage() {
        return checkNotNull(inboxStorage);
    }

    /**
     * Returns the value of the configured {@code DeliveryStrategy} or {@code Optional.empty()}
     * if no such value was configured.
     */
    public Optional<DeliveryStrategy> strategy() {
        return Optional.ofNullable(strategy);
    }

    /**
     * Returns the non-{@code null} value of the configured {@code DeliveryStrategy}.
     */
    DeliveryStrategy getStrategy() {
        return checkNotNull(strategy);
    }

    /**
     * Returns the value of the configured {@code ShardedWorkRegistry} or {@code Optional.empty()}
     * if no such value was configured.
     */
    public Optional<ShardedWorkRegistry> workRegistry() {
        return Optional.ofNullable(workRegistry);
    }

    /**
     * Returns the non-{@code null} value of the configured {@code ShardedWorkRegistry}.
     */
    ShardedWorkRegistry getWorkRegistry() {
        return checkNotNull(workRegistry);
    }

    /**
     * Returns the value of the configured idempotence window or {@code Optional.empty()}
     * if no such value was configured.
     */
    public Optional<Duration> idempotenceWindow() {
        return Optional.ofNullable(idempotenceWindow);
    }

    /**
     * Returns the non-{@code null} value of the configured idempotence window.
     */
    Duration getIdempotenceWindow() {
        return checkNotNull(idempotenceWindow);
    }

    @CanIgnoreReturnValue
    public DeliveryBuilder setWorkRegistry(ShardedWorkRegistry workRegistry) {
        this.workRegistry = checkNotNull(workRegistry);
        return this;
    }

    @CanIgnoreReturnValue
    public DeliveryBuilder setStrategy(DeliveryStrategy strategy) {
        this.strategy = checkNotNull(strategy);
        return this;
    }

    @CanIgnoreReturnValue
    public DeliveryBuilder setIdempotenceWindow(Duration idempotenceWindow) {
        this.idempotenceWindow = checkNotNull(idempotenceWindow);
        return this;
    }

    @CanIgnoreReturnValue
    public DeliveryBuilder setInboxStorage(InboxStorage inboxStorage) {
        checkNotNull(inboxStorage);
        this.inboxStorage = inboxStorage;
        return this;
    }

    public Delivery build() {
        if (strategy == null) {
            strategy = UniformAcrossAllShards.singleShard();
        }

        if (idempotenceWindow == null) {
            idempotenceWindow = Duration.getDefaultInstance();
        }

        if (this.inboxStorage == null) {
            this.inboxStorage = ServerEnvironment.instance()
                                                 .storageFactory()
                                                 .createInboxStorage(true);
        }

        if (workRegistry == null) {
            workRegistry = new InMemoryShardedWorkRegistry();
        }

        Delivery delivery = new Delivery(this);
        return delivery;
    }
}
