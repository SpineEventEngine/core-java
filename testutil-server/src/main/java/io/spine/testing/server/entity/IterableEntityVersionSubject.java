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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.extensions.proto.IterableOfProtosSubject;
import io.spine.core.Version;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.truth.Truth.assertAbout;
import static io.spine.testing.server.entity.EntityVersionSubject.assertEntityVersion;

@VisibleForTesting
public final class IterableEntityVersionSubject
        extends IterableOfProtosSubject<IterableEntityVersionSubject, Version, Iterable<Version>> {

    private IterableEntityVersionSubject(FailureMetadata failureMetadata,
                                         @Nullable Iterable<Version> versions) {
        super(failureMetadata, versions);
    }

    public static IterableEntityVersionSubject
    assertEntityVersions(@Nullable Iterable<Version> versions) {
        return assertAbout(entityVersions()).that(versions);
    }

    public EntityVersionSubject containsSingleEntityVersionThat() {
        assertContainsSingleItem();
        Version version = actual().iterator()
                                  .next();
        return assertEntityVersion(version);
    }

    private void assertContainsSingleItem() {
        isNotNull();
        hasSize(1);
    }

    static
    Subject.Factory<IterableEntityVersionSubject, Iterable<Version>> entityVersions() {
        return IterableEntityVersionSubject::new;
    }
}
