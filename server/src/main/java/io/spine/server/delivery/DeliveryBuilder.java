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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Duration;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.memory.InMemoryShardedWorkRegistry;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A builder for {@code Delivery} instances.
 */
@SuppressWarnings("ClassWithTooManyMethods")    // That's expected for the centerpiece configurator.
public final class DeliveryBuilder {

    /**
     * The default number of messages to be delivered in scope of a single {@link DeliveryStage}.
     */
    private static final int DEFAULT_PAGE_SIZE = 500;

    /**
     * The default number of the events to recall per single read operation during the catch-up.
     */
    private static final int DEFAULT_CATCH_UP_PAGE_SIZE = 500;

    private @MonotonicNonNull InboxStorage inboxStorage;
    private @MonotonicNonNull CatchUpStorage catchUpStorage;
    private @MonotonicNonNull DeliveryStrategy strategy;
    private @MonotonicNonNull ShardedWorkRegistry workRegistry;
    private @MonotonicNonNull Duration deduplicationWindow;
    private @MonotonicNonNull DeliveryMonitor deliveryMonitor;
    private @MonotonicNonNull Integer pageSize;
    private @MonotonicNonNull Integer catchUpPageSize;

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
     * Returns the value of the configured {@code CatchUpStorage} or {@code Optional.empty()} if no
     * such value was configured.
     */
    public Optional<CatchUpStorage> catchUpStorage() {
        return Optional.ofNullable(catchUpStorage);
    }

    /**
     * Returns the non-{@code null} value of the configured {@code CatchUpStorage}.
     */
    CatchUpStorage getCatchUpStorage() {
        return checkNotNull(catchUpStorage);
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
     * Returns the value of the configured deduplication window or {@code Optional.empty()}
     * if no such value was configured.
     */
    public Optional<Duration> deduplicationWindow() {
        return Optional.ofNullable(deduplicationWindow);
    }

    /**
     * Returns the non-{@code null} value of the configured deduplication window.
     */
    Duration getDeduplicationWindow() {
        return checkNotNull(deduplicationWindow);
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

    /**
     * Returns the value of the configured catch-up page size or {@code Optional.empty()}
     * if no such value was configured.
     */
    public Optional<Integer> catchUpPageSize() {
        return Optional.ofNullable(catchUpPageSize);
    }

    Integer getCatchUpPageSize() {
        return checkNotNull(catchUpPageSize);
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
    public DeliveryBuilder setDeduplicationWindow(Duration deduplicationWindow) {
        this.deduplicationWindow = checkNotNull(deduplicationWindow);
        return this;
    }

    /**
     * Sets the custom {@code InboxStorage}.
     *
     * <p>If none set, the storage is initialized by the {@code StorageFactory} specific for
     * this {@code ServerEnvironment}.
     *
     * <p>If no {@code StorageFactory} is present in the {@code ServerEnvironment}, a new
     * {@code InMemoryStorage} is used.
     */
    @CanIgnoreReturnValue
    public DeliveryBuilder setInboxStorage(InboxStorage inboxStorage) {
        this.inboxStorage = checkNotNull(inboxStorage);
        return this;
    }

    /**
     * Sets the custom {@code CatchUpStorage}.
     *
     * <p>If none set, the storage is initialized by the {@code StorageFactory} specific for
     * this {@code ServerEnvironment}.
     *
     * <p>If no {@code StorageFactory} is present in the {@code ServerEnvironment}, a new
     * {@code InMemoryStorage} is used.
     */
    @CanIgnoreReturnValue
    public DeliveryBuilder setCatchUpStorage(CatchUpStorage catchUpStorage) {
        this.catchUpStorage = checkNotNull(catchUpStorage);
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
     * Sets the maximum amount of messages to deliver within a {@link DeliveryStage}.
     *
     * <p>If none set, {@linkplain #DEFAULT_PAGE_SIZE} is used.
     */
    @CanIgnoreReturnValue
    public DeliveryBuilder setPageSize(int pageSize) {
        checkArgument(pageSize > 0);
        this.pageSize = pageSize;
        return this;
    }

    /**
     * Sets the maximum number of events to read from an event store per single read operation
     * during the catch-up.
     *
     * <p>If none set, {@linkplain #DEFAULT_CATCH_UP_PAGE_SIZE} is used.
     */
    @CanIgnoreReturnValue
    public DeliveryBuilder setCatchUpPageSize(int catchUpPageSize) {
        checkArgument(catchUpPageSize > 0);
        this.catchUpPageSize = catchUpPageSize;
        return this;
    }

    @SuppressWarnings("PMD.NPathComplexity")    // The readability of this method is fine.
    public Delivery build() {
        if (strategy == null) {
            strategy = UniformAcrossAllShards.singleShard();
        }

        if (deduplicationWindow == null) {
            deduplicationWindow = Duration.getDefaultInstance();
        }

        StorageFactory factory = storageFactory();
        if (this.inboxStorage == null) {
            this.inboxStorage = factory.createInboxStorage(true);
        }

        if (this.catchUpStorage == null) {
            this.catchUpStorage = factory.createCatchUpStorage(true);
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

        if (catchUpPageSize == null) {
            catchUpPageSize = DEFAULT_CATCH_UP_PAGE_SIZE;
        }

        Delivery delivery = new Delivery(this);
        return delivery;
    }

    private static StorageFactory storageFactory() {
        ServerEnvironment serverEnvironment = ServerEnvironment.instance();
        Optional<StorageFactory> currentStorageFactory = serverEnvironment.optionalStorageFactory();
        return currentStorageFactory.orElseGet(InMemoryStorageFactory::newInstance);
    }
}
