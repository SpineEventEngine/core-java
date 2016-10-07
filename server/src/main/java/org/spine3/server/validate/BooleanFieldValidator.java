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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.FieldPath;
import org.spine3.validate.ConstraintViolation;

import java.util.List;

/**
 * Validates fields of type {@link Boolean}.
 *
 * @author Dmitry Kashcheiev
 */
/* package */ class BooleanFieldValidator extends FieldValidator<Boolean>  {

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     * @param fieldValues values to validate
     * @param rootFieldPath a path to the root field (if present)
     */
    /*package*/ BooleanFieldValidator(Descriptors.FieldDescriptor descriptor, ImmutableList<Boolean> fieldValues, FieldPath rootFieldPath) {
        super(descriptor, fieldValues, rootFieldPath, false);
    }

    /**
     *  Boolean field can't be determined if it set or not, because protobuf 'false' value and 'no value' are the same
     *
     * @return false
     */
    @Override
    protected boolean isValueNotSet(Boolean value) {
        return false;
    }

    @Override
    protected List<ConstraintViolation> validate() {
        if (isRequiredField()) {
            log().warn("'required' option not allowed for boolean field");
        }
        return super.validate();
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(BooleanFieldValidator.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
