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

package io.spine.testing.server.blackbox.verify.query;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.extensions.proto.ProtoSubject;
import io.spine.core.Status;
import io.spine.core.Status.StatusCase;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertAbout;
import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.core.Status.StatusCase.OK;
import static io.spine.core.Status.StatusCase.REJECTION;

@VisibleForTesting
public final class ResponseStatusSubject extends ProtoSubject<ResponseStatusSubject, Status> {

    private static final String STATUS_CASE = "statusCase()";

    private ResponseStatusSubject(FailureMetadata failureMetadata,
                                  @Nullable Status message) {
        super(failureMetadata, message);
    }

    public static ResponseStatusSubject assertResponseStatus(@Nullable Status status) {
        return assertAbout(responseStatus()).that(status);
    }

    public void isOk() {
        assertExists();
        check(STATUS_CASE).that(statusCase())
                          .isEqualTo(OK);
    }

    public void isError() {
        assertExists();
        check(STATUS_CASE).that(statusCase())
                          .isEqualTo(ERROR);
    }

    public void isRejection() {
        assertExists();
        check(STATUS_CASE).that(statusCase())
                          .isEqualTo(REJECTION);
    }

    public void hasStatusCase(StatusCase statusCase) {
        checkNotNull(statusCase);
        assertExists();
        check(STATUS_CASE).that(statusCase())
                          .isEqualTo(statusCase);
    }

    private StatusCase statusCase() {
        return actual().getStatusCase();
    }

    private void assertExists() {
        isNotNull();
    }

    static Subject.Factory<ResponseStatusSubject, Status> responseStatus() {
        return ResponseStatusSubject::new;
    }
}
