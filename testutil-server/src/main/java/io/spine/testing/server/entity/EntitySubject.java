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
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.server.entity.Entity;
import io.spine.server.entity.LifecycleFlags;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.extensions.proto.ProtoTruth.protos;

/**
 * Assertions for entities.
 *
 * @param <S> the type of the entity state message
 * @param <E> the type of the entity
 */
public final class EntitySubject<S extends Message, E extends Entity<?, S>>
        extends Subject<EntitySubject<S, E>, E> {

    private EntitySubject(FailureMetadata metadata, @Nullable E actual) {
        super(metadata, actual);
    }

    /**
     * Creates a subject for asserting the passed Projection instance.
     */
    public static <I, E extends Entity<I, S>, S extends Message>
    EntitySubject<S, E> assertEntity(@Nullable E entity) {
        return assertAbout(EntitySubject.<S, E>entities()).that(entity);
    }

    /**
     * Verifies if the entity exists.
     */
    public void exists() {
        isNotNull();
    }

    /**
     * Verifies if the entity does not exist.
     */
    public void doesNotExist() {
        isNull();
    }

    /**
     * Obtains the subject for the {@code archived} flag.
     */
    public BooleanSubject archivedFlag() {
        if (actual() == null) {
            shouldExistButDoesNot();
            return ignoreCheck().that(false);
        } else {
            return check().that(flags().getArchived());
        }
    }

    /**
     * Obtains the subject for the {@code deleted} flag.
     */
    public BooleanSubject deletedFlag() {
        if (actual() == null) {
            shouldExistButDoesNot();
            return ignoreCheck().that(false);
        } else {
            return check().that(flags().getDeleted());
        }
    }

    private LifecycleFlags flags() {
        return actual().lifecycleFlags();
    }

    /**
     * Obtains the subject for the state of the entity.
     */
    public ProtoSubject<?, Message> hasStateThat() {
        E entity = actual();
        if (entity == null) {
            shouldExistButDoesNot();
            return ignoreCheck().about(protos())
                                .that(Empty.getDefaultInstance());
        } else {
            return check().about(protos())
                          .that(entity.state());
        }
    }

    private void shouldExistButDoesNot() {
        failWithoutActual(simpleFact("entity should exist"));
    }

    private static <S extends Message, E extends Entity<?, S>>
    Subject.Factory<EntitySubject<S, E>, E> entities() {
        return EntitySubject::new;
    }
}
