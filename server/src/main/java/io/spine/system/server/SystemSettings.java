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

package io.spine.system.server;

import com.google.common.base.Objects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.annotation.Internal;
import io.spine.base.Environment;
import io.spine.base.Production;

/**
 * A configuration of features of a system context.
 *
 * <p>Users may choose to turn certain system features on or off depending on the required
 * performance.
 */
public final class SystemSettings implements SystemFeatures {

    private boolean commandLog;
    private boolean aggregateMirrors;
    private boolean storeEvents;
    private boolean parallelPosting;

    /**
     * Prevents direct instantiation.
     */
    private SystemSettings() {
    }

    /**
     * Obtains the default configuration.
     *
     * <p>By default, the system context:
     * <ol>
     *     <li>Enables querying of the latest {@code Aggregate} states.
     *     <li>Does not store {@link io.spine.system.server.CommandLog CommandLog}.
     *     <li>Does not store system events.
     *     <li>Allows parallel posting of system events in production and disallows in tests.
     * </ol>
     */
    public static SystemSettings defaults() {
        SystemSettings settings = new SystemSettings()
                .disableCommandLog()
                .enableAggregateQuerying()
                .forgetEvents();
        if (Environment.instance().is(Production.class)) {
            settings.enableParallelPosting();
        } else {
            settings.disableParallelPosting();
        }
        return settings;
    }

    /**
     * Enables the configured system context to store
     * {@link io.spine.system.server.CommandLog CommandLog}s for domain commands.
     *
     * @return self for method chaining
     * @see #disableCommandLog()
     */
    @CanIgnoreReturnValue
    public SystemSettings enableCommandLog() {
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
    public SystemSettings disableCommandLog() {
        this.commandLog = false;
        return this;
    }

    /**
     * Enables querying of the latest domain {@code Aggregate} states.
     *
     * <p>The system context stores domain {@code Aggregate} states in the form of
     * {@link io.spine.system.server.Mirror} projections.
     *
     * <p>This is the default setting.
     *
     * @return self for method chaining
     * @see #disableAggregateQuerying()
     */
    @CanIgnoreReturnValue
    public SystemSettings enableAggregateQuerying() {
        this.aggregateMirrors = true;
        return this;
    }

    /**
     * Disables querying of the latest domain {@code Aggregate} states.
     *
     * @return self for method chaining
     * @see #enableAggregateQuerying()
     */
    @CanIgnoreReturnValue
    public SystemSettings disableAggregateQuerying() {
        this.aggregateMirrors = false;
        return this;
    }

    /**
     * Configures the the system context to store system events.
     *
     * @return self for method chaining
     */
    public SystemSettings persistEvents() {
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
    public SystemSettings forgetEvents() {
        this.storeEvents = false;
        return this;
    }

    /**
     * Configures the system context clients to post system events in parallel.
     *
     * <p>The events are posted using {@link java.util.concurrent.ForkJoinPool#commonPool()}.
     *
     * <p>This is the default setting in production environment.
     *
     * @return self for method chaining
     */
    @CanIgnoreReturnValue
    public SystemSettings enableParallelPosting() {
        this.parallelPosting = true;
        return this;
    }

    /**
     * Configures the system context clients NOT to post system events in parallel.
     *
     * <p>Choosing this configuration option may effect performance.
     *
     * <p>This is the default setting in test environment.
     *
     * @return self for method chaining
     */
    @CanIgnoreReturnValue
    public SystemSettings disableParallelPosting() {
        this.parallelPosting = false;
        return this;
    }

    @Internal
    @Override
    public boolean includeCommandLog() {
        return commandLog;
    }

    @Internal
    @Override
    public boolean includeAggregateMirroring() {
        return aggregateMirrors;
    }

    @Internal
    @Override
    public boolean includePersistentEvents() {
        return storeEvents;
    }

    @Internal
    @Override
    public boolean postEventsInParallel() {
        return parallelPosting;
    }

    /**
     * Copies these settings into an immutable feature set.
     */
    SystemConfig freeze() {
        return new SystemConfig(commandLog, aggregateMirrors, storeEvents, parallelPosting);
    }

    @SuppressWarnings({"OverlyComplexBooleanExpression", "NonFinalFieldReferenceInEquals"})
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SystemSettings)) {
            return false;
        }
        SystemSettings settings = (SystemSettings) o;
        return commandLog == settings.commandLog &&
                aggregateMirrors == settings.aggregateMirrors &&
                storeEvents == settings.storeEvents &&
                parallelPosting == settings.parallelPosting;
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public int hashCode() {
        return Objects.hashCode(commandLog, aggregateMirrors, storeEvents, parallelPosting);
    }
}
