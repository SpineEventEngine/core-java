/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.validate;

import com.google.protobuf.Descriptors.Descriptor;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.FieldPath;
import org.spine3.test.validate.msg.altfields.PersonName;
import org.spine3.validate.ConstraintViolation;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AlternativeFieldValidatorShould {

    private final FieldPath rootFieldPath = FieldPath.getDefaultInstance();

    private AlternativeFieldValidator validator;

    @Before
    public void setUp() {
        final Descriptor descriptor = PersonName.getDescriptor();
        validator = new AlternativeFieldValidator(descriptor, rootFieldPath);
    }

    @Test
    public void pass_if_one_field_populated() {
        final PersonName fieldPopulated = PersonName.newBuilder()
                                                    .setFirstName("Alexander")
                                                    .build();
        final List<? extends ConstraintViolation> violations = validator.validate(fieldPopulated);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void pass_if_combination_defined() {
        final PersonName combinationDefined = PersonName.newBuilder()
                .setHonorificPrefix("Mr.")
                .setLastName("Yevsyukov")
                .build();
        final List<? extends ConstraintViolation> violations = validator.validate(combinationDefined);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void fail_if_nothing_defined() {
        final PersonName empty = PersonName.getDefaultInstance();
        final List<? extends ConstraintViolation> violations = validator.validate(empty);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void fail_if_defined_not_required() {
        final PersonName notRequiredPopulated = PersonName.newBuilder()
                                                          .setHonorificSuffix("I")
                                                          .build();
        final List<? extends ConstraintViolation> violations = validator.validate(notRequiredPopulated);
        assertFalse(violations.isEmpty());
    }
}
