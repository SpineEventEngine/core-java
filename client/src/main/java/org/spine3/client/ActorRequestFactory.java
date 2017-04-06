/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package org.spine3.client;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.spine3.time.ZoneOffset;
import org.spine3.time.ZoneOffsets;
import org.spine3.users.TenantId;
import org.spine3.users.UserId;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for factories, creating the requests fired from the client-side by an actor.
 *
 * @author Alex Tymchenko
 */
abstract class ActorRequestFactory<F extends ActorRequestFactory> {

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

    protected ActorRequestFactory(AbstractBuilder<F, ?> builder) {
        this.actor = builder.actor;
        this.zoneOffset = builder.zoneOffset;
        this.tenantId = builder.tenantId;
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
     * Creates an {@linkplain ActorContext actor context}, based on the factory properties.
     */
    protected ActorContext actorContext() {
        final ActorContext.Builder builder = ActorContext.newBuilder()
                                                         .setActor(actor)
                                                         .setZoneOffset(zoneOffset);
        if(tenantId != null) {
            builder.setTenantId(tenantId);
        }
        return builder.build();
    }

    /**
     * Creates new factory with the same user and bounded context name and new time zone offset.
     *
     * @param zoneOffset the offset of the time zone
     * @return new command factory at new time zone
     */
    <B extends AbstractBuilder<F, B>>
        F switchTimezone(ZoneOffset zoneOffset, B builder) {
        return builder.setActor(getActor())
                      .setZoneOffset(zoneOffset)
                      .setTenantId(getTenantId())
                      .build();
    }

    public abstract F switchTimezone(ZoneOffset zoneOffset);


    /**
     * An abstract base for {@code Builder}s in {@link ActorRequestFactory} descendants.
     *
     * <p>Descendant classes should override the {@linkplain AbstractBuilder#build() build} method
     * to construct the factory of a desired type.
     *
     * @param <F> the type of the factory, which the {@code Builder} creates
     * @param <B> the type of the factory builder.
     */
    protected abstract static class AbstractBuilder<F extends ActorRequestFactory,
                                                    B extends AbstractBuilder> {

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
        public B setActor(UserId actor) {
            this.actor = checkNotNull(actor);
            return thisInstance();
        }

        public ZoneOffset getZoneOffset() {
            return zoneOffset;
        }

        /**
         * Sets the time zone in which the user works.
         *
         * @param zoneOffset the offset of the timezone the user works in
         */
        public B setZoneOffset(ZoneOffset zoneOffset) {
            this.zoneOffset = checkNotNull(zoneOffset);
            return thisInstance();
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
        public B setTenantId(@Nullable TenantId tenantId) {
            this.tenantId = tenantId;
            return thisInstance();
        }

        /**
         * Returns the instance of a descendant {@code Builder} for the type covariance
         * to make the method chaining possible.
         */
        protected abstract B thisInstance();

        /**
         * Ensures that all the {@code Builder} parameters are set properly.
         *
         * <p>Returns {@code null}, as it is expected to be overridden by descendants.
         *
         * @return {@code null}
         */
        @SuppressWarnings("ReturnOfNull")   // It's fine for an abstract Builder.
        @CanIgnoreReturnValue
        protected F build() {
            checkNotNull(actor, "`actor` must be defined");

            if (zoneOffset == null) {
                setZoneOffset(ZoneOffsets.getDefault());
            }
            return null;
        }
    }
}
