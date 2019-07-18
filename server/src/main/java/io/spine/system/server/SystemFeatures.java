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

package io.spine.system.server;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.annotation.Internal;

/**
 * A configuration of features of a system context.
 *
 * <p>Users may choose to turn certain system features on or off depending on the required
 * performance.
 */
public final class SystemFeatures {

    private boolean commandLog;
    private boolean aggregateMirrors;
    private boolean storeEvents;

    /**
     * Prevents direct instantiation.
     */
    private SystemFeatures() {
    }

    /**
     * Obtains the default configuration.
     *
     * <p>By default, the system context:
     * <ol>
     *     <li>Stores Aggregate mirrors for querying.
     *     <li>Does not store {@link io.spine.system.server.CommandLog CommandLog}.
     *     <li>Does not store system events.
     * </ol>
     */
    public static SystemFeatures defaults() {
        return new SystemFeatures()
                .disableCommandLog()
                .enableAggregateQuerying()
                .forgetEvents();
    }

    /**
     * Enables the configured system context to store
     * {@linkplain io.spine.system.server.CommandLog logs} for domain commands.
     *
     * @return self for method chaining
     * @see #disableCommandLog()
     */
    @CanIgnoreReturnValue
    public SystemFeatures enableCommandLog() {
        this.commandLog = true;
        return this;
    }

    /**
     * Disables {@linkplain io.spine.system.server.CommandLog CommandLog}.
     *
     * <p>This is the default setting.
     *
     * @return self for method chaining
     * @see #enableCommandLog()
     */
    @CanIgnoreReturnValue
    public SystemFeatures disableCommandLog() {
        this.commandLog = false;
        return this;
    }

    /**
     * Enables the configured system context to store
     * {@linkplain io.spine.system.server.Mirror mirrors} for domain aggregates.
     *
     * <p>This is the default setting.
     *
     * @return self for method chaining
     * @see #disableAggregateQuerying()
     */
    @CanIgnoreReturnValue
    public SystemFeatures enableAggregateQuerying() {
        this.aggregateMirrors = true;
        return this;
    }

    /**
     * Disables domain Aggregate mirrors.
     *
     * @return self for method chaining
     * @see #enableAggregateQuerying()
     */
    @CanIgnoreReturnValue
    public SystemFeatures disableAggregateQuerying() {
        this.aggregateMirrors = false;
        return this;
    }

    /**
     * Configures the the system context to store system events.
     *
     * @return self for method chaining
     */
    public SystemFeatures persistEvents() {
        this.storeEvents = true;
        return this;
    }

    /**
     * Configures the the system context NOT to store system events for better performance.
     *
     * <p>This is the default setting.
     *
     * @return self for method chaining
     */
    public SystemFeatures forgetEvents() {
        this.storeEvents = false;
        return this;
    }

    /**
     * Obtains the Command log setting.
     *
     * @return {@code true} if the Command log should be stored, {@code false} otherwise
     */
    boolean includeCommandLog() {
        return commandLog;
    }

    /**
     * Obtains the Aggregate mirrors setting.
     *
     * @return {@code true} if the Aggregate mirrors should be stored, {@code false} otherwise
     */
    boolean includeAggregateMirroring() {
        return aggregateMirrors;
    }

    /**
     * Obtains the system events persistence setting.
     *
     * @return {@code true} if system events should be stored, {@code false} otherwise
     */
    @Internal
    public boolean includePersistentEvents() {
        return storeEvents;
    }
}
