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

import com.google.common.testing.NullPointerTester;
import com.google.common.truth.Subject;
import com.google.protobuf.Message;
import io.spine.client.QueryResponse;
import io.spine.testing.SubjectTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.ExpectFailure.assertThat;
import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.core.Status.StatusCase.OK;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.server.blackbox.verify.query.QueryResultSubject.assertQueryResult;
import static io.spine.testing.server.blackbox.verify.query.QueryResultSubject.queryResult;
import static io.spine.testing.server.blackbox.verify.query.given.QueryResultSubjectTestEnv.responseWithMultipleEntities;
import static io.spine.testing.server.blackbox.verify.query.given.QueryResultSubjectTestEnv.responseWithSingleEntity;
import static io.spine.testing.server.blackbox.verify.query.given.QueryResultSubjectTestEnv.state2;
import static io.spine.testing.server.blackbox.verify.query.given.QueryResultSubjectTestEnv.version2;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The test for the {@link QueryResultSubject}.
 *
 * <p>This test uses own {@code static} versions for some of the methods provided in
 * {@link SubjectTest} due to the non-standard
 * {@linkplain QueryResultSubject#assertQueryResult(QueryResponse) way} of subject creation.
 */
@DisplayName("QueryResultSubject should")
class QueryResultSubjectTest extends SubjectTest<QueryResultSubject, Iterable<Message>> {

    private static final String EXPECTED_ASSERTION_ERROR_TO_BE_THROWN =
            "Expected `AssertionError` to be thrown.";

    @Override
    protected Subject.Factory<QueryResultSubject, Iterable<Message>> subjectFactory() {
        return queryResult();
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(QueryResultSubject.class);
        new NullPointerTester()
                .testAllPublicInstanceMethods(assertWithSubjectThat(responseWithSingleEntity()));
    }

    @Test
    @DisplayName("check the response status")
    void checkResponseStatus() {
        AssertionError error = expectFailure(
                () -> assertWithSubjectThat(responseWithSingleEntity())
                        .hasStatus(ERROR)
        );
        assertThat(error).factValue(EXPECTED)
                         .isEqualTo(ERROR.toString());
        assertThat(error).factValue(BUT_WAS)
                         .isEqualTo(OK.toString());
    }

    @Test
    @DisplayName("provide subject for the response status")
    void provideResponseStatusSubject() {
        AssertionError error = expectFailure(
                () -> assertWithSubjectThat(responseWithSingleEntity())
                        .hasStatusThat()
                        .isError()
        );
        assertThat(error).factValue(EXPECTED)
                         .isEqualTo(ERROR.toString());
        assertThat(error).factValue(BUT_WAS)
                         .isEqualTo(OK.toString());
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Method called to raise an error.
    @Test
    @DisplayName("check contains a single entity state")
    void checkSingleEntityState() {
        AssertionError error = expectFailure(
                () -> assertWithSubjectThat(responseWithMultipleEntities())
                        .containsSingleEntityStateThat()
        );
        assertThat(error).factValue(EXPECTED)
                         .contains("1");

        String actualCount = String.valueOf(responseWithMultipleEntities().size());
        assertThat(error).factValue(BUT_WAS)
                         .contains(actualCount);
    }

    @Test
    @DisplayName("provide a `ProtoSubject` for yielded entity state")
    void provideEntityStateProtoSubject() {
        expectSomeFailure(
                () -> assertWithSubjectThat(responseWithSingleEntity())
                        .containsSingleEntityStateThat()
                .isEqualTo(state2())
        );
    }

    @SuppressWarnings("CheckReturnValue") // Method called to raise an error.
    @Test
    @DisplayName("check contains a single entity version")
    void checkSingleEntityVersion() {
        AssertionError error = expectFailure(
                () -> assertWithSubjectThat(responseWithMultipleEntities())
                        .containsSingleEntityVersionThat()
        );
        assertThat(error).factValue(EXPECTED)
                         .contains("1");

        String actualCount = String.valueOf(responseWithMultipleEntities().size());
        assertThat(error).factValue(BUT_WAS)
                         .contains(actualCount);
    }

    @Test
    @DisplayName("provide an `EntityVersionSubject` for yielded entity version")
    void provideEntityVersionSubject() {
        expectSomeFailure(
                () -> assertWithSubjectThat(responseWithSingleEntity())
                        .containsSingleEntityVersionThat()
                        .isEqualTo(version2())
        );
    }

    @SuppressWarnings("CheckReturnValue") // Method called to raise an error.
    @Test
    @DisplayName("provide a subject for yielded entity versions when they are multiple")
    void provideIterableEntityVersionSubject() {
        int expectedSize = 3;
        AssertionError error = expectFailure(
                () -> assertWithSubjectThat(responseWithMultipleEntities())
                        .containsEntityVersionListThat()
                        .hasSize(expectedSize)
        );
        assertThat(error).factValue(EXPECTED)
                         .contains(String.valueOf(expectedSize));

        String actualCount = String.valueOf(responseWithMultipleEntities().size());
        assertThat(error).factValue(BUT_WAS)
                         .contains(actualCount);
    }

    private static QueryResultSubject assertWithSubjectThat(QueryResponse queryResponse) {
        return assertQueryResult(queryResponse);
    }

    @SuppressWarnings("ErrorNotRethrown") // Ignore the error.
    private static void expectSomeFailure(Runnable call) {
        try {
            call.run();
            fail(EXPECTED_ASSERTION_ERROR_TO_BE_THROWN);
        } catch (AssertionError ignored) {
        }
    }

    @SuppressWarnings({"ReturnOfNull" /* Not reachable. */,
            "ErrorNotRethrown" /* Error is expected. */})
    private static AssertionError expectFailure(Runnable call) {
        try {
            call.run();
            fail(EXPECTED_ASSERTION_ERROR_TO_BE_THROWN);
        } catch (AssertionError e) {
            return e;
        }
        return null;
    }
}
