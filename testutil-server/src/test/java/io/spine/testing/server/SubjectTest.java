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

package io.spine.testing.server;

import com.google.common.truth.ExpectFailure.SimpleSubjectBuilderCallback;
import com.google.common.truth.Subject;

import static com.google.common.truth.ExpectFailure.expectFailureAbout;

/**
 * An abstract base for custom {@link Subject} test suites.
 *
 * @param <S>
 *         the type of the {@code Subject}
 * @param <T>
 *         the target type tested by the {@code Subject}
 */
public abstract class SubjectTest<S extends Subject<S, T>, T> {

    protected abstract Subject.Factory<S, T> subjectFactory();

    protected AssertionError
    expectFailure(SimpleSubjectBuilderCallback<S, T> assertionCallback) {
        return expectFailureAbout(subjectFactory(), assertionCallback);
    }

    @SuppressWarnings({"ThrowableNotThrown", "CheckReturnValue"}) // Ignore the AssertionError.
    protected void
    expectSomeFailure(SimpleSubjectBuilderCallback<S, T> assertionCallback) {
        expectFailureAbout(subjectFactory(), assertionCallback);
    }
}
