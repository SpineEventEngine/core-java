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

@Immutable
@Internal
public final class EntityVisibility implements Serializable {

    private static final long serialVersionUID = 0L;

    private static final ImmutableGraph<Visibility> ALLOWED_VISIBILITIES;

    static {
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

        mutableGraph.nodes().forEach(visibility -> mutableGraph.putEdge(visibility, visibility));
        ALLOWED_VISIBILITIES = ImmutableGraph.copyOf(mutableGraph);
    }

    private final Visibility value;

    private EntityVisibility(Visibility value) {
        this.value = value;
    }

    public static EntityVisibility of(Class<? extends Message> stateClass) {
        checkNotNull(stateClass);
        Descriptor descriptor = TypeName.of(stateClass).messageDescriptor();
        EntityOption entityOption = descriptor.getOptions()
                                              .getExtension(OptionsProto.entity);
        Visibility visibility = entityOption.getVisibility();
        if (visibility == DEFAULT) {
            Kind kind = entityOption.getKind();
            visibility = kind == PROJECTION ? FULL : NONE;
        }
        return new EntityVisibility(visibility);
    }

    public boolean canSubscribe() {
        return isAsLeast(SUBSCRIBE);
    }

    public boolean canQuery() {
        return isAsLeast(QUERY);
    }

    public boolean isNotNone() {
        return value != NONE;
    }

    public boolean is(Visibility visibility) {
        checkNotNull(visibility);
        checkNotDefault(visibility);
        return value == visibility;
    }

    public boolean isAsLeast(Visibility visibility) {
        checkNotNull(visibility);
        checkNotDefault(visibility);

        return ALLOWED_VISIBILITIES.hasEdgeConnecting(value, visibility);
    }

    private static void checkNotDefault(Visibility visibility) {
        checkArgument(visibility != DEFAULT, "Unexpected visibility level: %s.", DEFAULT);
    }

    @Override
    public String toString() {
        return value.name();
    }
}
