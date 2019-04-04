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

package io.spine.testing.server.aggregate;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.protobuf.Message;
import io.spine.server.aggregate.Aggregate;
import io.spine.testing.server.entity.EntitySubject;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.truth.Truth.assertAbout;

/**
 * Assertions for Aggregates.
 *
 * @param <S> the type of the aggregate state
 * @param <A> the type of the aggregate
 */
public final class AggregateSubject<S extends Message, A extends Aggregate<?, S, ?>>
        extends EntitySubject<AggregateSubject<S, A>, S, A> {

    private AggregateSubject(FailureMetadata metadata, @NullableDecl A actual) {
        super(metadata, actual);
    }

    /**
     * Creates a subject for asserting the passed Aggregate instance.
     */
    public static <I, A extends Aggregate<I, S, ?>, S extends Message>
    AggregateSubject<S, A> assertAggregate(@Nullable A instance) {
        return assertAbout(AggregateSubject.<S, A>aggregates()).that(instance);
    }

    private static <S extends Message, A extends Aggregate<?, S, ?>>
    Subject.Factory<AggregateSubject<S, A>, A> aggregates() {
        return AggregateSubject::new;
    }
}
