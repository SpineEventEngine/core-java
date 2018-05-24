/*
 * Copyright 2018, TeamDev. All rights reserved.
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
package io.spine.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.core.ActorContext;
import io.spine.core.CommandContext;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;

import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Time.getCurrentTime;

/**
 * A factory for the various requests fired from the client-side by an actor.
 *
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 */
public class ActorRequestFactory {

    private final UserId actor;

    /**
     * In case the zone offset is not defined, the current time zone offset value is set by default.
     */
    private final ZoneOffset zoneOffset;

    /**
     * The ID of the tenant in a multitenant application.
     *
     * <p>This field is null in a single tenant application.
     */
    @Nullable
    private final TenantId tenantId;

    protected ActorRequestFactory(Builder builder) {
        this.actor = builder.actor;
        this.zoneOffset = builder.zoneOffset;
        this.tenantId = builder.tenantId;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public UserId getActor() {
        return actor;
    }

    public ZoneOffset getZoneOffset() {
        return zoneOffset;
    }

    @Nullable
    public TenantId getTenantId() {
        return tenantId;
    }

    /**
     * Creates new factory with the same user and tenant ID, but with new time zone offset.
     *
     * @param zoneOffset the offset of the time zone
     * @return new factory at new time zone
     */
    public ActorRequestFactory switchTimezone(ZoneOffset zoneOffset) {
        ActorRequestFactory result =
                newBuilder().setActor(getActor())
                            .setZoneOffset(zoneOffset)
                            .setTenantId(getTenantId())
                            .build();
        return result;
    }

    /**
     * Creates an instance of {@link QueryFactory} based on configuration of this
     * {@code ActorRequestFactory} instance.
     *
     * @return an instance of {@link QueryFactory}
     */
    public QueryFactory query() {
        return new QueryFactory(this);
    }

    /**
     * Creates an instance of {@link TopicFactory} based on configuration of this
     * {@code ActorRequestFactory} instance.
     *
     * @return an instance of {@link TopicFactory}
     */
    public TopicFactory topic() {
        return new TopicFactory(this);
    }

    /**
     * Creates an instance of {@link CommandFactory} based on configuration of this
     * {@code ActorRequestFactory} instance.
     *
     * @return an instance of {@link CommandFactory}
     */
    public CommandFactory command() {
        return new CommandFactory(this);
    }

    /**
     * @see CommandFactory#createContext()
     */
    @VisibleForTesting
    protected CommandContext createCommandContext() {
        return command().createContext();
    }

    /**
     * Creates an {@linkplain ActorContext actor context}, based on the factory properties.
     *
     * <p>Sets the timestamp value to the
     * {@linkplain io.spine.base.Time#getCurrentTime() current time}.
     */
    @SuppressWarnings("CheckReturnValue") // calling builder
    ActorContext actorContext() {
        ActorContext.Builder builder =
                ActorContext.newBuilder()
                            .setActor(actor)
                            .setTimestamp(getCurrentTime())
                            .setZoneOffset(zoneOffset);
        if (tenantId != null) {
            builder.setTenantId(tenantId);
        }
        return builder.build();
    }

    /**
     * A builder for {@code ActorRequestFactory}.
     */
    public static class Builder {

        private UserId actor;

        private ZoneOffset zoneOffset;

        @Nullable
        private TenantId tenantId;

        public UserId getActor() {
            return actor;
        }

        /**
         * Sets the ID for the user generating commands.
         *
         * @param actor the ID of the user generating commands
         */
        public Builder setActor(UserId actor) {
            this.actor = checkNotNull(actor);
            return this;
        }

        @Nullable
        public ZoneOffset getZoneOffset() {
            return zoneOffset;
        }

        /**
         * Sets the time zone in which the user works.
         *
         * @param zoneOffset the offset of the timezone the user works in
         */
        public Builder setZoneOffset(ZoneOffset zoneOffset) {
            this.zoneOffset = checkNotNull(zoneOffset);
            return this;
        }

        @Nullable
        public TenantId getTenantId() {
            return tenantId;
        }

        /**
         * Sets the ID of a tenant in a multi-tenant application to which this user belongs.
         *
         * @param tenantId the ID of the tenant or null for single-tenant applications
         */
        public Builder setTenantId(@Nullable TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * Ensures that all the {@code Builder} parameters are set properly.
         *
         * <p>Returns {@code null}, as it is expected to be overridden by descendants.
         *
         * @return {@code null}
         */
        @SuppressWarnings("CheckReturnValue") // calling builder
        @CanIgnoreReturnValue
        public ActorRequestFactory build() {
            checkNotNull(actor, "`actor` must be defined");

            if (zoneOffset == null) {
                setZoneOffset(ZoneOffsets.getDefault());
            }

            return new ActorRequestFactory(this);
        }
    }
}
