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
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a scenario when shard could not be picked because of some occurred exception.
 */
public final class RuntimeFailure extends FailedPickUp {

    private final RuntimeException exception;

    /**
     * Creates a new {@code RuntimeFailure}.
     *
     * @param shard
     *         a shard that could not be picked
     * @param exception
     *         an occurred exception
     * @param retry
     *         a way to retry delivery
     * @apiNote The constructor has the package-private access because this class is
     *         instantiated by Spine and passed to the {@code DeliveryMonitor}, users don't need
     *         to instantiate it. Users are only able to call methods returning an {@code Action}
     *         to modify the error handling behaviour.
     */
    RuntimeFailure(ShardIndex shard,
                   RuntimeException exception,
                   Supplier<Optional<DeliveryStats>> retry
    ) {
        super(shard, retry);
        checkNotNull(exception);
        this.exception = exception;
    }

    /**
     * Returns an {@code Action} that propagates the occurred exception.
     */
    public Action propagate() {
        return () -> {
            throw exception;
        };
    }

    /**
     * Returns the occurred exception.
     */
    public RuntimeException exception() {
        return exception;
    }
}
