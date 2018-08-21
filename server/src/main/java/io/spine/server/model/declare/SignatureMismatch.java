/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.base.MoreObjects;
import io.spine.annotation.Internal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Alex Tymchenko
 */
@Internal
public final class SignatureMismatch {

    private final MatchCriterion unmetCriterion;
    private final String message;
    private final Severity severity;

    private SignatureMismatch(MatchCriterion criterion, Object[] values) {
        unmetCriterion = criterion;
        severity = criterion.getSeverity();
        message = criterion.formatMsg(values);
    }

    public String getMessage() {
        return message;
    }

    Severity getSeverity() {
        return severity;
    }

    public MatchCriterion getUnmetCriterion() {
        return unmetCriterion;
    }

    static SignatureMismatch create(MatchCriterion criterion, Object ...values) {
        checkNotNull(criterion);
        checkNotNull(values);

        SignatureMismatch result = new SignatureMismatch(criterion, values);
        return result;
    }

    enum Severity {
        ERROR,
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
