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

import com.google.common.collect.ImmutableList;
import com.google.common.testing.NullPointerTester;
import com.google.common.truth.Subject;
import io.spine.core.Version;
import io.spine.testing.SubjectTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.ExpectFailure.assertThat;
import static io.spine.core.Versions.zero;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.server.entity.IterableEntityVersionSubject.assertEntityVersions;
import static io.spine.testing.server.entity.IterableEntityVersionSubject.entityVersions;
import static io.spine.testing.server.entity.given.GivenEntityVersion.newerVersion;
import static io.spine.testing.server.entity.given.GivenEntityVersion.olderVersion;
import static io.spine.testing.server.entity.given.GivenEntityVersion.version;

@DisplayName("IterableEntityVersionSubject should")
class IterableEntityVersionSubjectTest
        extends SubjectTest<IterableEntityVersionSubject, Iterable<Version>> {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicInstanceMethods(assertEntityVersions(currentAndNewer()));
    }

    @Nested
    @DisplayName("check that version iterable contains all versions that are")
    class CheckVersions {

        @Test
        @DisplayName("newer than the specified version")
        void newer() {
            assertWithSubjectThat(currentAndNewer())
                    .containsAllNewerThan(olderVersion());

            expectSomeFailure(whenTesting -> whenTesting.that(currentAndNewer())
                                                        .containsAllNewerThan(version()));
        }

        @Test
        @DisplayName("newer or equal to the specified version")
        void newerOrEqual() {
            assertWithSubjectThat(currentAndNewer())
                    .containsAllNewerOrEqualTo(version());

            expectSomeFailure(whenTesting -> whenTesting.that(currentAndNewer())
                                                        .containsAllNewerOrEqualTo(newerVersion()));
        }

        @Test
        @DisplayName("older than the specified version")
        void older() {
            assertWithSubjectThat(currentAndOlder())
                    .containsAllOlderThan(newerVersion());

            expectSomeFailure(whenTesting -> whenTesting.that(currentAndOlder())
                                                        .containsAllOlderThan(version()));
        }

        @Test
        @DisplayName("older or equal to the specified version")
        void olderOrEqual() {
            assertWithSubjectThat(currentAndOlder())
                    .containsAllOlderOrEqualTo(version());

            expectSomeFailure(whenTesting -> whenTesting.that(currentAndOlder())
                                                        .containsAllOlderOrEqualTo(olderVersion()));
        }
    }

    @Nested
    @DisplayName("if actual is null")
    class IfNull {

        @Test
        @DisplayName("not allow to containsAllNewerThan(...)")
        void containsAllNewerThan() {
            AssertionError error = expectFailure(
                    whenTesting -> whenTesting.that(null)
                                              .containsAllNewerThan(zero())
            );
            assertDetectedNull(error);
        }

        @Test
        @DisplayName("not allow to containsAllNewerOrEqualTo(...)")
        void containsAllNewerOrEqualTo() {
            AssertionError error = expectFailure(
                    whenTesting -> whenTesting.that(null)
                                              .containsAllNewerOrEqualTo(zero())
            );
            assertDetectedNull(error);
        }

        @Test
        @DisplayName("not allow to containsAllOlderThan(...)")
        void containsAllOlderThan() {
            AssertionError error = expectFailure(
                    whenTesting -> whenTesting.that(null)
                                              .containsAllOlderThan(zero())
            );
            assertDetectedNull(error);
        }

        @Test
        @DisplayName("not allow to containsAllOlderOrEqualTo(...)")
        void containsAllOlderOrEqualTo() {
            AssertionError error = expectFailure(
                    whenTesting -> whenTesting.that(null)
                                              .containsAllOlderOrEqualTo(zero())
            );
            assertDetectedNull(error);
        }

        @Test
        @DisplayName("not allow to containsSingleEntityVersionThat()")
        void containsSingleEntityVersionThat() {
            AssertionError error = expectFailure(
                    whenTesting -> whenTesting.that(null)
                                              .containsSingleEntityVersionThat()
            );
            assertDetectedNull(error);
        }

        private void assertDetectedNull(AssertionError error) {
            assertThat(error).factValue("expected not to be")
                             .isEqualTo("null");
        }
    }

    @SuppressWarnings("CheckReturnValue") // Method called to raise an error.
    @Test
    @DisplayName("return a subject for a single stored entity version")
    void returnEntityVersionSubject() {
        assertWithSubjectThat(singleVersion())
                .containsSingleEntityVersionThat()
                .isNewerThan(olderVersion());

        expectSomeFailure(whenTesting -> whenTesting.that(currentAndOlder())
                                                    .containsSingleEntityVersionThat());
    }

    @Override
    protected Subject.Factory<IterableEntityVersionSubject, Iterable<Version>> subjectFactory() {
        return entityVersions();
    }

    private static Iterable<Version> singleVersion() {
        return ImmutableList.of(version());
    }

    private static Iterable<Version> currentAndNewer() {
        return ImmutableList.of(version(), newerVersion());
    }

    private static Iterable<Version> currentAndOlder() {
        return ImmutableList.of(version(), olderVersion());
    }
}
