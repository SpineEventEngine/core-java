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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.ActorContext;
import org.spine3.base.CommandContext;
import org.spine3.protobuf.ProtoJavaMapper;
import org.spine3.time.ZoneOffset;
import org.spine3.time.ZoneOffsets;
import org.spine3.users.TenantId;
import org.spine3.users.UserId;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.time.Time.getCurrentTime;

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
        final ActorRequestFactory result = newBuilder().setActor(getActor())
                                                       .setZoneOffset(zoneOffset)
                                                       .setTenantId(getTenantId())
                                                       .build();
        return result;
    }

    public QueryFactory query() {
        return new QueryFactory(actorContext());
    }

    public TopicFactory topic() {
        return new TopicFactory(actorContext());
    }

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
     * {@linkplain org.spine3.time.Time#getCurrentTime() current time}.
     */
    @VisibleForTesting
    ActorContext actorContext() {
        final ActorContext.Builder builder = ActorContext.newBuilder()
                                                         .setActor(actor)
                                                         .setTimestamp(getCurrentTime())
                                                         .setZoneOffset(zoneOffset);
        if (tenantId != null) {
            builder.setTenantId(tenantId);
        }
        return builder.build();
    }

    /**
     * A parameter of a {@link Query}.
     *
     * <p>This class may be considered a filter for the query. An instance contains the name of
     * the Entity Column to filter by and the value of the Column.
     *
     * <p>The supported types for querying are {@link Message} and Protobuf primitives.
     */
    public static final class QueryParameter {

        private final String columnName;
        private final Any value;

        private QueryParameter(String columnName, Any value) {
            this.columnName = columnName;
            this.value = value;
        }

        /**
         * Creates new equality {@code QueryParameter}.
         *
         * @param columnName the name of the Entity Column to query by, expressed in a single field
         *                   name with no type info
         * @param value      the requested value of the Entity Column
         * @return new instance of the QueryParameter
         */
        public static QueryParameter eq(String columnName, Object value) {
            checkNotNull(columnName);
            checkNotNull(value);
            final Any wrappedValue = ProtoJavaMapper.map(value);
            final QueryParameter parameter = new QueryParameter(columnName, wrappedValue);
            return parameter;
        }

        /**
         * @return the name of the Entity Column to query by
         */
        public String getColumnName() {
            return columnName;
        }

        /**
         * @return the value of the Entity Column to look for
         */
        public Any getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            QueryParameter parameter = (QueryParameter) o;
            return Objects.equal(getColumnName(), parameter.getColumnName()) &&
                    Objects.equal(getValue(), parameter.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getColumnName(), getValue());
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append('(')
              .append(columnName)
              .append(" = ")
              .append(unpack(value))
              .append(')');
            return sb.toString();
        }
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
        @SuppressWarnings("ReturnOfNull")   // It's fine for an abstract Builder.
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
