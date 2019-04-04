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

package io.spine.testing.server.projection;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.protobuf.Message;
import io.spine.server.projection.Projection;
import io.spine.testing.server.entity.EntitySubject;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.truth.Truth.assertAbout;

/**
 * Assertions for Projections.
 */
public final class ProjectionSubject<S extends Message, P extends Projection<?, S, ?>>
        extends EntitySubject<ProjectionSubject<S, P>, S, P> {

    private ProjectionSubject(FailureMetadata metadata, @NullableDecl P actual) {
        super(metadata, actual);
    }

    /**
     * Creates a subject for asserting the passed Projection instance.
     */
    public static <I, P extends Projection<I, S, ?>, S extends Message>
    ProjectionSubject<S, P> assertProjection(@Nullable P instance) {
        return assertAbout(ProjectionSubject.<S, P>projections()).that(instance);
    }

    private static <S extends Message, P extends Projection<?, S, ?>>
    Subject.Factory<ProjectionSubject<S, P>, P> projections() {
        return ProjectionSubject::new;
    }
}
