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
import io.spine.time.ZoneId;
import io.spine.time.ZoneIds;
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
     * The offset of a time zone from which a request is made.
     *
     * <p>In case the zone offset is not defined when {@linkplain #newBuilder() constructing
     * the factory}, the current time zone offset value is set by default.
     */
    private final ZoneOffset zoneOffset;

    /**
     * The ID of the time zone from which a request is made.
     *
     * <p>In case the zone ID is not defined when {@linkplain #newBuilder() constructing
     * the factory}, the system time zone ID will be set.
     */
    private final ZoneId zoneId;

    /**
     * The ID of the tenant in a multitenant application.
     *
     * <p>This field is null in a single tenant application.
     */
    private final @Nullable TenantId tenantId;

    protected ActorRequestFactory(Builder builder) {
        this.actor = builder.actor;
        this.zoneOffset = builder.zoneOffset;
        this.zoneId = builder.zoneId;
        this.tenantId = builder.tenantId;
    }

    /**
     * Creates a builder for a new request factory.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates an instance by the passed {@code ActorContext}.
     */
    public static ActorRequestFactory fromContext(ActorContext actorContext) {
        Builder builder = newBuilder()
                .setActor(actorContext.getActor())
                .setTenantId(actorContext.getTenantId())
                .setZoneOffset(actorContext.getZoneOffset())
                .setZoneId(actorContext.getZoneId());
        return builder.build();
    }

    /**
     * Obtains the ID of the user on behalf of whom the requests are created.
     */
    public UserId getActor() {
        return actor;
    }

    /**
     * Obtains the offset of the time zone in which the actor works.
     */
    public ZoneOffset getZoneOffset() {
        return zoneOffset;
    }

    /**
     * Obtains the ID of the time zone in which the actor works.
     */
    public ZoneId getZoneId() {
        return zoneId;
    }

    /**
     * Obtains the ID of the tenant to which the actor belongs, or {@code null}
     * for single-tenant execution context.
     */
    public @Nullable TenantId getTenantId() {
        return tenantId;
    }

    /**
     * Creates a copy of this factory placed at new time zone with the passed offset and ID.
     *
     * <p>Use this method for obtaining new request factory as the user moves to a new location.
     *
     * @param zoneOffset the offset of the new time zone
     * @param zoneId     the ID of the new time zone
     * @return new factory at the new time zone
     */
    public ActorRequestFactory switchTimeZone(ZoneOffset zoneOffset, ZoneId zoneId) {
        checkNotNull(zoneOffset);
        checkNotNull(zoneId);
        ActorRequestFactory result =
                newBuilder().setActor(getActor())
                            .setZoneOffset(zoneOffset)
                            .setZoneId(zoneId)
                            .setTenantId(getTenantId())
                            .build();
        return result;
    }

    /**
     * Creates a copy of this factory placed at new time zone with the passed ID.
     *
     * <p>The zone offset is calculated using the current time.
     *
     * @param zoneId the ID of the new time zone
     * @return new factory at the new time zone
     */
    public ActorRequestFactory switchTimeZone(ZoneId zoneId) {
        checkNotNull(zoneId);
        java.time.ZoneId id = java.time.ZoneId.of(zoneId.getValue());
        java.time.ZoneOffset offset = java.time.OffsetTime.now(id)
                                                          .getOffset();
        return switchTimeZone(ZoneOffsets.of(offset), zoneId);
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
        ActorContext.Builder builder = ActorContext
                .newBuilder()
                .setActor(actor)
                .setTimestamp(getCurrentTime())
                .setZoneOffset(zoneOffset)
                .setZoneId(zoneId);
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
        private ZoneId zoneId;

        private @Nullable TenantId tenantId;

        public UserId getActor() {
            return actor;
        }

        /**
         * Sets the ID for the user generating commands.
         *
         * @param actor the ID of the user generating commands
         */
        @CanIgnoreReturnValue
        public Builder setActor(UserId actor) {
            this.actor = checkNotNull(actor);
            return this;
        }

        /**
         * Obtains the zone offset set in the builder.
         *
         * @return the zone offset or {@code null} if the value was not set
         */
        public @Nullable ZoneOffset getZoneOffset() {
            return zoneOffset;
        }

        /**
         * Sets the offset of the time zone in which the user works.
         *
         * @param zoneOffset the offset of the time zone the user works in
         */
        @CanIgnoreReturnValue
        public Builder setZoneOffset(ZoneOffset zoneOffset) {
            this.zoneOffset = checkNotNull(zoneOffset);
            return this;
        }

        /**
         * Obtains the zone ID set in the builder.
         *
         * @return the zone ID or {@code null} if the value was not set
         */
        public @Nullable ZoneId getZoneId() {
            return zoneId;
        }

        /**
         * Sets the ID of the time zone in which the user works.
         *
         * @param zoneId the
         */
        @CanIgnoreReturnValue
        public Builder setZoneId(ZoneId zoneId) {
            this.zoneId = checkNotNull(zoneId);
            return this;
        }

        /**
         * Obtains the ID of the tenant in a multi-tenant application to which the actor belongs.
         *
         * @return the ID of the tenant or {@code null} for a single-tenant execution context
         */
        public @Nullable TenantId getTenantId() {
            return tenantId;
        }

        /**
         * Sets the ID of a tenant in a multi-tenant application to which the actor belongs.
         *
         * @param tenantId the ID of the tenant or {@code null} for single-tenant execution context
         */
        @CanIgnoreReturnValue
        public Builder setTenantId(@Nullable TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * Ensures that all the {@code Builder} parameters are set properly.
         *
         * <p>Initializes {@code zoneOffset} and {@code zoneId} with default values if they are
         * not defined.
         *
         * @return a new instance of the factory
         */
        public ActorRequestFactory build() {
            checkNotNull(actor, "`actor` must be defined");

            if (zoneOffset == null) {
                setZoneOffset(ZoneOffsets.getDefault());
            }

            if (zoneId == null) {
                setZoneId(ZoneIds.systemDefault());
            }

            return new ActorRequestFactory(this);
        }
    }
}
