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

package io.spine.server.model.declare;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import io.spine.annotation.Internal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The mismatch of a signature.
 */
@Internal
public final class SignatureMismatch {

    /**
     * The criterion, which requirements were not met.
     */
    private final MatchCriterion unmetCriterion;

    /**
     * The severity of the mismatch.
     *
     * <p>Users of this class should consider building their handling logic depending
     * on the value of this field. E.g. {@link Severity#ERROR ERROR} severity should probably
     * be handled by raising an exception, such as {@link SignatureMismatchException}.
     */
    private final Severity severity;

    /**
     * The message telling what the mismatch is.
     *
     * <p>This field exists to avoid formatting the message each time from the message template.
     */
    private final String message;

    private SignatureMismatch(MatchCriterion criterion, Object[] values) {
        unmetCriterion = criterion;
        severity = criterion.getSeverity();
        message = criterion.formatMsg(values);
    }

    /** Returns whether this mismatch is of {@code ERROR} severity. */
    boolean isError() {
        return severity == Severity.ERROR;
    }

    /** Returns whether this mismatch is of {@code WARN} severity. */
    boolean isWarning() {
        return severity == Severity.WARN;
    }

    /**
     * Returns the match criterion, which requirements were violated.
     */
    @VisibleForTesting
    public MatchCriterion getUnmetCriterion() {
        return unmetCriterion;
    }

    /**
     * Creates a new mismatch from the criterion and the values, which violated the criterion.
     *
     * @param criterion
     *         the criterion
     * @param values
     *         the values, which did not met the criterion requirements
     * @return a new {@code SignatureMismatch} instance
     */
    static SignatureMismatch create(MatchCriterion criterion, Object... values) {
        checkNotNull(criterion);
        checkNotNull(values);

        SignatureMismatch result = new SignatureMismatch(criterion, values);
        return result;
    }

    /**
     * The severity level of the mismatch.
     */
    enum Severity {
        /**
         * The mismatch of {@code ERROR} level means that the violation is a show-stopper.
         */
        ERROR,

        /**
         * The mismatch of {@code WARN} level means that the recommended criterion was not met,
         * however the application execution may proceed.
         */
        WARN
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // `message` is a common term.
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("unmetCriterion", unmetCriterion)
                          .add("message", message)
                          .toString();
    }
}
