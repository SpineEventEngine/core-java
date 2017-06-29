/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package io.spine.validate;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import io.spine.string.Stringifiers;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;
import static java.util.Collections.unmodifiableList;

/**
 * An exception, thrown if a {@code Message} does not pass the validation.
 *
 * @author Illia Shepilov
 * @author Alex Tymchenko
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 0L;

    private static final Function<ConstraintViolation, String> TO_STRING_FN = new ToStringFn();

    /**
     * List of the constraint violations, that were found during the validation.
     */
    private final List<ConstraintViolation> constraintViolations;

    public ValidationException(List<ConstraintViolation> constraintViolations) {
        super();
        this.constraintViolations = constraintViolations;
    }

    public List<ConstraintViolation> getConstraintViolations() {
        return unmodifiableList(constraintViolations);
    }

    @Override
    public String toString() {
        final ToStringHelper helper = MoreObjects.toStringHelper(this);

        final String violationContent = constraintViolations.isEmpty()
                ? "[]"
                : on(", ").join(transform(constraintViolations, TO_STRING_FN));

        return helper.add("constraintViolations", violationContent)
                     .toString();
    }

    /**
     * A function, transforming a {@linkplain ConstraintViolation constraint violation}
     * into a {@code String}.
     */
    private static final class ToStringFn implements Function<ConstraintViolation, String> {
        @Nullable
        @Override
        public String apply(@Nullable ConstraintViolation input) {
            checkNotNull(input);
            return Stringifiers.toString(input);
        }
    }
}
