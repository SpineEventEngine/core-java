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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A builder for {@code Delivery} instances.
 */
public final class DeliveryBuilder {

    /**
     * The default number of messages to be delivered in scope of a single {@link DeliveryStage}.
     */
    private static final int DEFAULT_PAGE_SIZE = 500;

    private @MonotonicNonNull InboxStorage inboxStorage;
    private @MonotonicNonNull DeliveryStrategy strategy;
    private @MonotonicNonNull ShardedWorkRegistry workRegistry;
    private @MonotonicNonNull Duration idempotenceWindow;
    private @MonotonicNonNull DeliveryMonitor deliveryMonitor;
    private @MonotonicNonNull Integer pageSize;

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

    /**
     * Returns the value of the configured {@code DeliveryMonitor} or {@code Optional.empty()}
     * if no such value was configured.
     */
    public Optional<DeliveryMonitor> deliveryMonitor() {
        return Optional.ofNullable(deliveryMonitor);
    }

    DeliveryMonitor getMonitor() {
        return checkNotNull(deliveryMonitor);
    }

    /**
     * Returns the value of the configured page size or {@code Optional.empty()}
     * if no such value was configured.
     */
    public Optional<Integer> pageSize() {
        return Optional.ofNullable(pageSize);
    }

    Integer getPageSize() {
        return checkNotNull(pageSize);
    }

    @CanIgnoreReturnValue
    public DeliveryBuilder setWorkRegistry(ShardedWorkRegistry workRegistry) {
        this.workRegistry = checkNotNull(workRegistry);
        return this;
    }

    /**
     * Sets strategy of assigning a shard index for a message that is delivered to a particular
     * target.
     *
     * <p>If none set, {@link UniformAcrossAllShards#singleShard()} is be used.
     */
    @CanIgnoreReturnValue
    public DeliveryBuilder setStrategy(DeliveryStrategy strategy) {
        this.strategy = checkNotNull(strategy);
        return this;
    }

    /**
     * Sets for how long the previously delivered messages should be kept in the {@code Inbox}
     * to ensure the incoming messages aren't duplicates.
     *
     * <p>If none set, zero duration is used.
     */
    @CanIgnoreReturnValue
    public DeliveryBuilder setIdempotenceWindow(Duration idempotenceWindow) {
        this.idempotenceWindow = checkNotNull(idempotenceWindow);
        return this;
    }

    /**
     * Sets the custom {@code InboxStorage}.
     *
     * <p>If none set, the storage is initialized by the {@code StorageFactory} specific for
     * this {@code ServerEnvironment}.
     */
    @CanIgnoreReturnValue
    public DeliveryBuilder setInboxStorage(InboxStorage inboxStorage) {
        this.inboxStorage = checkNotNull(inboxStorage);
        return this;
    }

    /**
     * Sets the custom {@code DeliveryMonitor}.
     *
     * <p>If none set, {@link DeliveryMonitor#alwaysContinue()}  is used.
     */
    @CanIgnoreReturnValue
    public DeliveryBuilder setMonitor(DeliveryMonitor monitor) {
        this.deliveryMonitor = checkNotNull(monitor);
        return this;
    }

    /**
     * Sets the  maximum amount of messages to deliver within a {@link DeliveryStage}.
     *
     * <p>If none set, {@linkplain #DEFAULT_PAGE_SIZE} is used.
     */
    @CanIgnoreReturnValue
    public DeliveryBuilder setPageSize(int pageSize) {
        checkArgument(pageSize > 0);
        this.pageSize = pageSize;
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

        if (deliveryMonitor == null) {
            deliveryMonitor = DeliveryMonitor.alwaysContinue();
        }

        if (pageSize == null) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        Delivery delivery = new Delivery(this);
        return delivery;
    }
}
