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

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An abstract base for shard pick-up-related failures.
 */
public abstract class FailedPickUp {

    private final ShardIndex shard;

    private final RetryDelivery retry;

    /**
     * Creates a new {@code FailedPickUp} with the given {@code shard} and {@code retry} action.
     */
    FailedPickUp(ShardIndex shard, RetryDelivery retry) {
        checkNotNull(shard);
        checkNotNull(retry);
        this.retry = retry;
        this.shard = shard;
    }

    /**
     * Returns an {@code Action} that will retry the delivery from the shard.
     */
    public final Action retry() {
        return retry::retry;
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
    public interface Action {

        /**
         * Executes the {@code Action}.
         */
        Optional<DeliveryStats> execute();
    }

    /**
     * Specifies a way to retry the delivery.
     */
    interface RetryDelivery {

        /**
         * Retries the delivery.
         */
        Optional<DeliveryStats> retry();
    }
}
