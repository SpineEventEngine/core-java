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

package io.spine.testing.server.entity;

import com.google.common.truth.BooleanSubject;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.google.protobuf.Message;
import io.spine.server.entity.EntityWithLifecycle;
import io.spine.server.entity.LifecycleFlags;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import static com.google.common.truth.Truth.assertThat;

/**
 * Assertions for entities.
 *
 * @param <T> the type of entity subject for parameter covariance in {@code Subject.Factory}
 * @param <S> the type of the entity state message
 * @param <E> the type of the entity
 */
public class EntitySubject<T extends EntitySubject<T, S, E>,
                           S extends Message,
                           E extends EntityWithLifecycle<?, S>>
        extends Subject<T, E> {

    protected EntitySubject(FailureMetadata metadata, @NullableDecl E actual) {
        super(metadata, actual);
    }

    /**
     * Verifies if the entity exists.
     */
    public void exists() {
        isNotNull();
    }

    /**
     * Obtains the subject for the {@code archived} flag.
     */
    public BooleanSubject archivedFlag() {
        exists();
        return assertThat(flags().getArchived());
    }

    /**
     * Obtains the subject for the {@code deleted} flag.
     */
    public BooleanSubject deletedFlag() {
        exists();
        return assertThat(flags().getDeleted());
    }

    private LifecycleFlags flags() {
        return actual().getLifecycleFlags();
    }

    /**
     * Obtains the subject for the state of the entity.
     */
    public ProtoSubject<?, Message> hasStateThat() {
        exists();
        return ProtoTruth.assertThat(actual().getState());
    }
}
