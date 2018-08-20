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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Alex Tymchenko
 */
public final class SignatureMismatch {

    private final MatchCriterion unmetCriterion;

    private final Object[] values;

    private SignatureMismatch(MatchCriterion criterion, Object[] values) {
        unmetCriterion = criterion;
        this.values = values;
    }

    public String getMessage() {
        return unmetCriterion.formatMsg(values);
    }

    public Severity getSeverity() {
        return unmetCriterion.getSeverity();
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
}
