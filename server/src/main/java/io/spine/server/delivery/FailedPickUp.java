/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.annotation.SPI;

import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An abstract base for shard pick-up-related failures.
 *
 * <p>This class is internal and is not designed to be extended by the framework users. The actual
 * inheritor of this class will be created by framework and passed to the corresponding failure
 * handler.
 *
 * <p>The internal {@linkplain Action} may be implemented by the framework users to provide
 * a custom behaviour for pickup failures in the {@linkplain DeliveryMonitor} in addition to
 * predefined behaviours provided by the inheritors of this class.
 */
public abstract class FailedPickUp {

    private final ShardIndex shard;

    private final Supplier<Optional<DeliveryStats>> retryDelivery;

    /**
     * Creates a new {@code FailedPickUp} with the given {@code shard}
     * and {@code retryDelivery} action.
     */
    FailedPickUp(ShardIndex shard, Supplier<Optional<DeliveryStats>> retryDelivery) {
        checkNotNull(shard);
        checkNotNull(retryDelivery);
        this.retryDelivery = retryDelivery;
        this.shard = shard;
    }

    /**
     * Returns an {@code Action} that will retry the delivery from the shard.
     */
    public final Action retry() {
        return retryDelivery::get;
    }

    /**
     * Returns a {@code ShardIndex} of the not picked shard.
     */
    public final ShardIndex shard() {
        return shard;
    }

    /**
     * Action to take in relation to failed pick-up.
     */
    @SPI
    @FunctionalInterface
    public interface Action {

        /**
         * Executes the {@code Action}.
         */
        Optional<DeliveryStats> execute();
    }
}
