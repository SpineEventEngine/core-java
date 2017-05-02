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
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.spine3.annotations.Internal;
import org.spine3.base.ActorContext;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.base.Identifiers;
import org.spine3.json.Json;
import org.spine3.protobuf.ProtoJavaMapper;
import org.spine3.time.ZoneOffset;
import org.spine3.time.ZoneOffsets;
import org.spine3.users.TenantId;
import org.spine3.users.UserId;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static org.spine3.client.Queries.queryBuilderFor;
import static org.spine3.client.Targets.composeTarget;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.time.Time.getCurrentTime;

/**
 * A factory for the various requests fired from the client-side by an actor.
 *
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 */
public class ActorRequestFactory {

    /**
     * The format of all {@linkplain QueryId query identifiers}.
     */
    private static final String QUERY_ID_FORMAT = "query-%s";

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

    public ForQuery query() {
        return new ForQuery();
    }

    public ForTopic topic() {
        return new ForTopic();
    }

    public ForCommand command() {
        return new ForCommand();
    }

    /**
     * @see ForCommand#createContext()
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

    private Query composeQuery(Class<? extends Message> entityClass,
                               @Nullable Set<? extends Message> ids,
                               @Nullable Map<String, Any> columnFilters,
                               @Nullable FieldMask fieldMask) {
        checkNotNull(entityClass, "The class of Entity must be specified for a Query");

        final Query.Builder builder = queryBuilderFor(entityClass,
                                                      ids,
                                                      columnFilters,
                                                      fieldMask);

        builder.setId(newQueryId());
        builder.setContext(actorContext());
        return builder.build();
    }

    private static QueryId newQueryId() {
        final String formattedId = format(QUERY_ID_FORMAT, Identifiers.newUuid());
        return QueryId.newBuilder()
                      .setValue(formattedId)
                      .build();
    }

    /**
     * Public API for creating {@link Query} instances, using the {@code ActorRequestFactory}
     * configuration.
     */
    public final class ForQuery {

        private static final String ENTITY_IDS_EMPTY_MSG = "Entity ID set must not be empty";

        private ForQuery() {
            // Prevent instantiation from the outside.
        }

        /**
         * Creates a new instance of {@link QueryBuilder} for the further {@linkplain Query}
         * construction.
         *
         * @param targetType the {@linkplain Query query} target type
         * @return new instance of {@link QueryBuilder}
         */
        public QueryBuilder select(Class<? extends Message> targetType) {
            checkNotNull(targetType);
            final QueryBuilder queryBuilder = new QueryBuilder(targetType);
            return queryBuilder;
        }

        /**
         * Creates a {@link Query} to read certain entity states by IDs with the {@link FieldMask}
         * applied to each of the results.
         *
         * <p>Allows to specify a set of identifiers to be used during the {@code Query} processing.
         * The processing results will contain only the entities, which IDs are present among
         * the {@code ids}.
         *
         * <p>Allows to set property paths for a {@link FieldMask}, applied to each of the query
         * results. This processing is performed according to the
         * <a href="https://goo.gl/tW5wIU">FieldMask specs</a>.
         *
         * <p>In case the {@code paths} array contains entries inapplicable to the resulting entity
         * (e.g. a {@code path} references a missing field),
         * such invalid paths are silently ignored.
         *
         * @param entityClass the class of a target entity
         * @param ids         the entity IDs of interest
         * @param maskPaths   the property paths for the {@code FieldMask} applied
         *                    to each of results
         * @return an instance of {@code Query} formed according to the passed parameters
         */
        public Query byIdsWithMask(Class<? extends Message> entityClass,
                                   Set<? extends Message> ids,
                                   String... maskPaths) {
            checkNotNull(ids);
            checkArgument(!ids.isEmpty(), ENTITY_IDS_EMPTY_MSG);

            final FieldMask fieldMask = FieldMask.newBuilder()
                                                 .addAllPaths(Arrays.asList(maskPaths))
                                                 .build();
            final Query result = composeQuery(entityClass, ids, null, fieldMask);
            return result;
        }

        /**
         * Creates a {@link Query} to read certain entity states by IDs.
         *
         * <p>Allows to specify a set of identifiers to be used during the {@code Query} processing.
         * The processing results will contain only the entities, which IDs are present among
         * the {@code ids}.
         *
         * <p>Unlike {@link #byIdsWithMask(Class, Set, String...)}, the {@code Query} processing
         * will not change the resulting entities.
         *
         * @param entityClass the class of a target entity
         * @param ids         the entity IDs of interest
         * @return an instance of {@code Query} formed according to the passed parameters
         */
        public Query byIds(Class<? extends Message> entityClass,
                           Set<? extends Message> ids) {
            checkNotNull(entityClass);
            checkNotNull(ids);

            return composeQuery(entityClass, ids, null, null);
        }

        /**
         * Creates a {@link Query} to read all entity states with the {@link FieldMask}
         * applied to each of the results.
         *
         * <p>Allows to set property paths for a {@link FieldMask}, applied to each of the query
         * results. This processing is performed according to the
         * <a href="https://goo.gl/tW5wIU">FieldMask specs</a>.
         *
         * <p>In case the {@code paths} array contains entries inapplicable to the resulting entity
         * (e.g. a {@code path} references a missing field), such invalid paths
         * are silently ignored.
         *
         * @param entityClass the class of a target entity
         * @param maskPaths   the property paths for the {@code FieldMask} applied
         *                    to each of results
         * @return an instance of {@code Query} formed according to the passed parameters
         */
        public Query allWithMask(Class<? extends Message> entityClass, String... maskPaths) {
            final FieldMask fieldMask = FieldMask.newBuilder()
                                                 .addAllPaths(Arrays.asList(maskPaths))
                                                 .build();
            final Query result = composeQuery(entityClass, null, null, fieldMask);
            return result;
        }

        /**
         * Creates a {@link Query} to read all states of a certain entity.
         *
         * <p>Unlike {@link #allWithMask(Class, String...)}, the {@code Query} processing will
         * not change the resulting entities.
         *
         * @param entityClass the class of a target entity
         * @return an instance of {@code Query} formed according to the passed parameters
         */
        public Query all(Class<? extends Message> entityClass) {
            checkNotNull(entityClass);

            return composeQuery(entityClass, null, null, null);
        }
    }

    /**
     * A builder for the {@link Query} instances.
     *
     * <p>The API of this class is insiped by the SQL syntax.
     *
     * <p>Calling any of the methods is optional. Call {@link #build() build()} to retrieve
     * the instance of {@link Query}.
     *
     * <p>Calling of any of the builder methods overrides the previous call of the given method of
     * any of its overloads.
     *
     * <p>Usage example:
     * <pre>
     *     {@code
     *     final Query query = factory().query()
     *                                  .select(Customer.class)
     *                                  .fields("name", "address", "email")
     *                                  .whereIdIn(getWestCostCustomersIds())
     *                                  .where({@link QueryParameter#eq(String, Object) eq}("type", "permanent"),
     *                                         eq("discountPercent", 10),
     *                                         eq("companySize", Company.Size.SMALL))
     *                                  .build();
     *     }
     * </pre>
     *
     * @see ForQuery#select(Class) for the intialization
     */
    public final class QueryBuilder {

        private final Class<? extends Message> targetType;

        // All the optional fields are initialized only when and if set

        @Nullable
        private Set<?> ids;

        @Nullable
        private Map<String, Any> columns;

        @Nullable
        private Set<String> fieldMask;

        private QueryBuilder(Class<? extends Message> targetType) {
            this.targetType = targetType;
        }

        /**
         * Sets the ID predicate to the {@linkplain Query}.
         *
         * <p>Though it's not prohibited in compile time, please make sure to pass instances of the
         * same type to the argument of this method. Moreover, the instances must be of the type of
         * the query target type identifier. This method or any of its overload do not check these
         * constrains an assume they are followed by the caller.
         *
         * <p>If there are no IDs (i.e. and empty {@link Iterable} is passed), the query will
         * retrieve all the records regardless their IDs.
         *
         * @param ids the values of the IDs to look up
         * @return self for method chaining
         */
        public QueryBuilder whereIdIn(Iterable<?> ids) {
            this.ids = ImmutableSet.builder()
                                   .add(ids)
                                   .build();
            return this;
        }

        /**
         * Sets the IP predicate to the {@linkplain Query}.
         *
         * @param ids the values of the IDs to look up
         * @return self for method chaining
         * @see #whereIdIn(Iterable)
         */
        public QueryBuilder whereIdIn(Message... ids) {
            this.ids = ImmutableSet.<Message>builder()
                    .add(ids)
                    .build();
            return this;
        }

        /**
         * Sets the IP predicate to the {@linkplain Query}.
         *
         * @param ids the values of the IDs to look up
         * @return self for method chaining
         * @see #whereIdIn(Iterable)
         */
        public QueryBuilder whereIdIn(String... ids) {
            this.ids = ImmutableSet.<String>builder()
                    .add(ids)
                    .build();
            return this;
        }

        /**
         * Sets the IP predicate to the {@linkplain Query}.
         *
         * @param ids the values of the IDs to look up
         * @return self for method chaining
         * @see #whereIdIn(Iterable)
         */
        public QueryBuilder whereIdIn(Integer... ids) {
            this.ids = ImmutableSet.<Integer>builder()
                    .add(ids)
                    .build();
            return this;
        }

        /**
         * Sets the IP predicate to the {@linkplain Query}.
         *
         * @param ids the values of the IDs to look up
         * @return self for method chaining
         * @see #whereIdIn(Iterable)
         */
        public QueryBuilder whereIdIn(Long... ids) {
            this.ids = ImmutableSet.<Long>builder()
                    .add(ids)
                    .build();
            return this;
        }

        /**
         * Sets the Entity Column predicate to the {@linkplain Query}.
         *
         * <p>If there are no {@link QueryParameter}s (i.e. the passed array is empty), all
         * the records will be retrieved regardless the Entity Columns values.
         *
         * <p>The multiple parameters passed into this method are considered to be joined in
         * a conjunction ({@code AND} operator), i.e. a record matches this query only if it matches all of
         * these parameters.
         *
         * <p>The disjunctive filters currently are not supported.
         *
         * @param predicate the {@link QueryParameter}s to filter the requested entities by
         * @return self for method chaining
         * @see QueryParameter
         */
        public QueryBuilder where(QueryParameter... predicate) {
            final ImmutableMap.Builder<String, Any> mapBuilder = ImmutableMap.builder();
            for (QueryParameter param : predicate) {
                mapBuilder.put(param.getColumnName(), param.getValue());
            }
            columns = mapBuilder.build();
            return this;
        }

        /**
         * Sets the entity fields to retrieve.
         *
         * <p>The names of the fields must be formatted according to the {@link FieldMask}
         * specification.
         *
         * <p>If there are no fields (i.e. an empty {@link Iterable} is passed), all the fields will
         * be retrieved.
         *
         * @param fieldNames the fields to query
         * @return self for method chaining
         */
        public QueryBuilder fields(Iterable<String> fieldNames) {
            this.fieldMask = ImmutableSet.copyOf(fieldNames);
            return this;
        }

        /**
         * Sets the entity fields to retrieve.
         *
         * <p>The names of the fields must be formatted according to the {@link FieldMask}
         * specification.
         *
         * <p>If there are no fields (i.e. an empty array is passed), all the fields will
         * be retrieved.
         *
         * @param fieldNames the fields to query
         * @return self for method chaining
         */
        public QueryBuilder fields(String... fieldNames) {
            this.fieldMask = ImmutableSet.<String>builder()
                    .add(fieldNames)
                    .build();
            return this;
        }

        /**
         * Generates a new instance of {@link Query} regarding all the set parameters.
         *
         * @return the built {@link Query}
         */
        public Query build() {
            final FieldMask mask = composeMask();
            // Implying AnyPacker.pack to be idempotent
            final Set<Any> entityIds = composeIdPredicate();

            final Query result = composeQuery(targetType, entityIds, columns, mask);
            return result;
        }

        @Nullable
        private FieldMask composeMask() {
            if (fieldMask == null || fieldMask.isEmpty()) {
                return null;
            }
            final FieldMask mask = FieldMask.newBuilder()
                                            .addAllPaths(fieldMask)
                                            .build();
            return mask;
        }

        @Nullable
        private Set<Any> composeIdPredicate() {
            if (ids == null || ids.isEmpty()) {
                return null;
            }
            final Collection<Any> entityIds = transform(ids, new Function<Object, Any>() {
                @Nullable
                @Override
                public Any apply(@Nullable Object o) {
                    checkNotNull(o);
                    final Any id = Identifiers.idToAny(o);
                    return id;
                }
            });
            final Set<Any> result = newHashSet(entityIds);
            return result;
        }

        @SuppressWarnings("MethodWithMoreThanThreeNegations")
            // OK for this method as it's used primarily for debugging
        @Override
        public String toString() {
            final String valueSeparator = "; ";
            final StringBuilder sb = new StringBuilder();
            sb.append(QueryBuilder.class.getSimpleName())
              .append('(')
              .append("SELECT ");
            if (fieldMask == null || fieldMask.isEmpty()) {
                sb.append('*');
            } else {
                sb.append(fieldMask);
            }
            sb.append(" FROM ")
              .append(targetType.getSimpleName())
              .append(" WHERE (");
            if (ids != null && !ids.isEmpty()) {
                sb.append("id IN ")
                  .append(ids)
                  .append(valueSeparator);
            }
            if (columns != null && !columns.isEmpty()) {
                for (Map.Entry<String, Any> column : columns.entrySet()) {
                    sb.append(column.getKey())
                      .append('=')
                      .append(Json.toCompactJson(unpack(column.getValue())))
                      .append(valueSeparator);
                }
            }
            sb.append(");");
            return sb.toString();
        }
    }

    /**
     * A parameter of a {@link Query} to the read side.
     *
     * <p>This class may be considered as a filter for the query. An instance contains the name of
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
     * Public API for creating {@link Topic} instances, using the {@code ActorRequestFactory}
     * configuration.
     */
    public final class ForTopic {

        private ForTopic() {
            // Prevent instantiation from the outside.
        }

        /**
         * Creates a {@link Topic} for a subset of the entity states by specifying their IDs.
         *
         * @param entityClass the class of a target entity
         * @param ids         the IDs of interest
         * @return the instance of {@code Topic} assembled according to the parameters.
         */
        public Topic someOf(Class<? extends Message> entityClass, Set<? extends Message> ids) {
            checkNotNull(entityClass);
            checkNotNull(ids);

            final Target target = composeTarget(entityClass, ids, null);
            final Topic result = forTarget(target);
            return result;
        }

        /**
         * Creates a {@link Topic} for all of the specified entity states.
         *
         * @param entityClass the class of a target entity
         * @return the instance of {@code Topic} assembled according to the parameters.
         */
        public Topic allOf(Class<? extends Message> entityClass) {
            checkNotNull(entityClass);

            final Target target = composeTarget(entityClass, null, null);
            final Topic result = forTarget(target);
            return result;
        }

        /**
         * Creates a {@link Topic} for the specified {@linkplain Target}.
         *
         * <p>This method is intended for internal use only. To achieve the similar result,
         * {@linkplain #allOf(Class) allOf()} and {@linkplain #someOf(Class, Set) someOf()} methods
         * should be used.
         *
         * @param target the {@code} Target to create a topic for.
         * @return the instance of {@code Topic}.
         */
        @Internal
        public Topic forTarget(Target target) {
            checkNotNull(target);
            final TopicId id = Topics.generateId();
            return Topic.newBuilder()
                        .setId(id)
                        .setContext(actorContext())
                        .setTarget(target)
                        .build();
        }

    }

    /**
     * Public API for creating {@link Command} instances, using the {@code ActorRequestFactory}
     * configuration.
     */
    public final class ForCommand {

        private ForCommand() {
            // Prevent instantiation from the outside.
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
            final CommandContext context = createContext();
            final Command result = Commands.createCommand(message, context);
            return result;
        }

        /**
         * Creates new {@code Command} with the passed message and target entity version.
         *
         * <p>The command contains a {@code CommandContext} instance with the current time.
         *
         * @param message       the command message
         * @param targetVersion the ID of the entity for applying commands if {@code null}
         *                      the commands can be applied to any entity
         * @return new command instance
         */
        public Command create(Message message, int targetVersion) {
            checkNotNull(message);
            checkNotNull(targetVersion);

            final CommandContext context = createContext(targetVersion);
            final Command result = Commands.createCommand(message, context);
            return result;
        }

        /**
         * Creates command context for a new command with entity ID.
         */
        private CommandContext createContext(int targetVersion) {
            return Commands.createContext(getTenantId(), getActor(),
                                          getZoneOffset(), targetVersion);
        }

        /**
         * Creates command context for a new command.
         */
        private CommandContext createContext() {
            return Commands.createContext(getTenantId(),
                                          getActor(),
                                          getZoneOffset());
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
