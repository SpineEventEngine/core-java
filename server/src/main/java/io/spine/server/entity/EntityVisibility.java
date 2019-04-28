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

package io.spine.server.entity;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.option.EntityOption;
import io.spine.option.EntityOption.Kind;
import io.spine.option.EntityOption.Visibility;
import io.spine.option.OptionsProto;
import io.spine.type.TypeName;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.option.EntityOption.Kind.PROJECTION;
import static io.spine.option.EntityOption.Visibility.DEFAULT;
import static io.spine.option.EntityOption.Visibility.FULL;
import static io.spine.option.EntityOption.Visibility.NONE;
import static io.spine.option.EntityOption.Visibility.QUERY;
import static io.spine.option.EntityOption.Visibility.SUBSCRIBE;

/**
 * The visibility of an entity type.
 *
 * <p>An entity can have one of the following visibility levels:
 * <ol>
 *     <li>{@code NONE} - the entity is not visible to the clients for reading;
 *     <li>{@code QUERY} - the entity is visible for querying, but not for subscription;
 *     <li>{@code SUBSCRIBE} - the entity is visible for subscription, but not for querying;
 *     <li>{@code FULL} - the entity is visible for both subscription and querying.
 * </ol>
 *
 * <p>The visibility of an entity is defined by the {@code (entity)} option. By default, any entity
 * is has the {@code NONE} level except for projections, which have the {@code FULL} level.
 */
@Immutable
@Internal
public final class EntityVisibility implements Serializable {

    private static final long serialVersionUID = 0L;

    /**
     * An ordered immutable graph of the visibility levels.
     *
     * <p>This graph is used for checking if a certain level includes another level. For example,
     * {@code FULL} includes {@code QUERY}. To represent this, there is an edge from {@code FULL}
     * to {@code QUERY} in the graph.
     *
     * @see #isAsLeast(Visibility)
     */
    private static final ImmutableGraph<Visibility> VISIBILITIES = buildVisibilityGraph();

    private final Visibility value;

    private EntityVisibility(Visibility value) {
        this.value = value;
    }

    /**
     * Obtains an instance of {@code EntityVisibility} for the given entity state class.
     *
     * @param stateClass
     *         the entity state class
     * @return new instance
     */
    public static EntityVisibility of(Class<? extends Message> stateClass) {
        checkNotNull(stateClass);
        Descriptor descriptor = TypeName.of(stateClass)
                                        .messageDescriptor();
        EntityOption entityOption = descriptor.getOptions()
                                              .getExtension(OptionsProto.entity);
        Visibility visibility = entityOption.getVisibility();
        if (visibility == DEFAULT) {
            Kind kind = entityOption.getKind();
            visibility = kind == PROJECTION ? FULL : NONE;
        }
        return new EntityVisibility(visibility);
    }

    /**
     * Checks if the visibility level is sufficient for subscription.
     *
     * <p>If the level is sufficient, it is allowed to subscribe to the entity state updates via
     * {@link io.spine.core.Subscribe} or {@link io.spine.server.SubscriptionService}.
     *
     * @return {@code true} if the visibility is {@code SUBSCRIBE} or {@code FULL}, {@code false}
     *         otherwise
     */
    public boolean canSubscribe() {
        return isAsLeast(SUBSCRIBE);
    }

    /**
     * Checks if the visibility level is sufficient for querying.
     *
     * <p>If the level is sufficient, it is allowed to query the entity states via
     * {@link io.spine.server.QueryService}.
     *
     * @return {@code true} if the visibility is {@code QUERY} or {@code FULL}, {@code false}
     *         otherwise
     */
    public boolean canQuery() {
        return isAsLeast(QUERY);
    }

    /**
     * Checks if the visibility level is not {@code NONE}.
     *
     * @return {@code true} if the visibility is anything but {@code NONE}, {@code false} otherwise
     */
    public boolean isNotNone() {
        return value != NONE;
    }

    /**
     * Checks if the visibility is exactly of the given level.
     *
     * @param visibility
     *         the visibility to compare to
     * @return {@code true} if this visibility is equal to the given visibility
     * @throws IllegalArgumentException
     *         if the given visibility is {@code DEFAULT}
     */
    public boolean is(Visibility visibility) {
        checkNotNull(visibility);
        checkNotDefault(visibility);
        return value == visibility;
    }

    /**
     * Checks if the visibility is at least as allowing as the given one.
     *
     * <p>{@code isAsLeast(NONE)} always returns {@code true}. {@code isAtLeast(FULL)} returns
     * {@code true} only if this visibility is exactly {@code FULL}.
     *
     * @param visibility
     *         the visibility to compare to
     * @return {@code true} if this visibility is equal to the given visibility
     * @throws IllegalArgumentException
     *         if the given visibility is {@code DEFAULT}
     */
    public boolean isAsLeast(Visibility visibility) {
        checkNotNull(visibility);
        checkNotDefault(visibility);
        return VISIBILITIES.hasEdgeConnecting(value, visibility);
    }

    private static void checkNotDefault(Visibility visibility) {
        checkArgument(visibility != DEFAULT, "Unexpected visibility level: %s.", DEFAULT);
    }

    @Override
    public String toString() {
        return value.name();
    }

    /**
     * Builds the graph of the visibilities hierarchy.
     *
     * @see #VISIBILITIES
     */
    private static ImmutableGraph<Visibility> buildVisibilityGraph() {
        MutableGraph<Visibility> mutableGraph = GraphBuilder
                .directed()
                .expectedNodeCount(Visibility.values().length)
                .allowsSelfLoops(true)
                .build();
        mutableGraph.addNode(NONE);
        mutableGraph.addNode(QUERY);
        mutableGraph.addNode(SUBSCRIBE);
        mutableGraph.addNode(FULL);

        mutableGraph.putEdge(QUERY, NONE);
        mutableGraph.putEdge(SUBSCRIBE, NONE);
        mutableGraph.putEdge(FULL, SUBSCRIBE);
        mutableGraph.putEdge(FULL, QUERY);
        mutableGraph.putEdge(FULL, NONE);

        mutableGraph.nodes()
                    .forEach(visibility -> mutableGraph.putEdge(visibility, visibility));
        return ImmutableGraph.copyOf(mutableGraph);
    }
}
