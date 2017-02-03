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

import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.time.ZoneOffset;
import org.spine3.time.ZoneOffsets;
import org.spine3.users.TenantId;
import org.spine3.users.UserId;

import javax.annotation.Nullable;

import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.time.ZoneOffsets.toZoneOffset;

/**
 * The factory to generate new {@link Command} instances.
 *
 * @author Alexander Yevsyukov
 */
public class CommandFactory {

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

    protected CommandFactory(Builder builder) {
        this.actor = builder.actor;
        this.zoneOffset = builder.zoneOffset;
        this.tenantId = builder.tenantId;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates new factory with the same user and bounded context name and new time zone offset.
     *
     * @param zoneOffset the offset of the time zone
     * @return new command factory at new time zone
     */
    public CommandFactory switchTimezone(ZoneOffset zoneOffset) {
        return newBuilder().setActor(getActor())
                           .setZoneOffset(zoneOffset)
                           .setTenantId(getTenantId())
                           .build();
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
     * Creates new {@code Command} with the passed message.
     *
     * <p>The command contains a {@code CommandContext} instance with the current time.
     *
     * @param message the command message
     * @return new command instance
     */
    public Command create(Message message) {
        checkNotNull(message);
        final CommandContext context = Commands.createContext(getTenantId(), getActor(), getZoneOffset());
        final Command result = Commands.create(message, context);
        return result;
    }

    public static class Builder {
        private UserId actor;
        private ZoneOffset zoneOffset;
        @Nullable
        private TenantId tenantId;

        private Builder() {
        }

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
         * Sets the ID of a tenant in a multitenant application to which this user belongs.
         *
         * @param tenantId the ID of the tenant or null for single-tenant applications
         */
        public Builder setTenantId(@Nullable TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public CommandFactory build() {
            checkNotNull(actor, "`actor` must be defined");

            if (zoneOffset == null) {
                setZoneOffset(toZoneOffset(TimeZone.getDefault()));
            }
            final CommandFactory result = new CommandFactory(this);
            return result;
        }
    }
}
