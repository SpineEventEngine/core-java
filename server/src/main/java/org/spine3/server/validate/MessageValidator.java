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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.spine3.Internal;
import org.spine3.base.FieldPath;
import org.spine3.validate.ConstraintViolation;

import java.util.List;

/**
 * Validates messages according to Spine custom protobuf options and provides constraint violations found.
 *
 * @author Alexander Litus
 */
@Internal
public class MessageValidator {

    private final FieldValidatorFactory fieldValidatorFactory = FieldValidatorFactory.newInstance();

    private final FieldPath rootFieldPath;

    /** Creates a new validator instance. */
    public static MessageValidator newInstance() {
        return new MessageValidator(FieldPath.getDefaultInstance());
    }

    /**
     * Creates a new validator instance.
     *
     * <p>Use this constructor for inner messages (which are marked with "valid" option in Protobuf).
     *
     * @param rootFieldPath the path to the message field which is the root for this message
     */
    /* package */ static MessageValidator newInstance(FieldPath rootFieldPath) {
        return new MessageValidator(rootFieldPath);
    }

    private MessageValidator(FieldPath rootFieldPath) {
        this.rootFieldPath = rootFieldPath;
    }

    /**
     * Validates messages according to Spine custom protobuf options and returns constraint violations found.
     *
     * @param message a message to validate
     */
    public List<ConstraintViolation> validate(Message message) {
        final ImmutableList.Builder<ConstraintViolation> result = ImmutableList.builder();
        final Descriptor msgDescriptor = message.getDescriptorForType();
        final List<FieldDescriptor> fields = msgDescriptor.getFields();
        for (FieldDescriptor field : fields) {
            final Object value = message.getField(field);
            final FieldValidator<?> validator = fieldValidatorFactory.create(field, value, rootFieldPath);
            final List<ConstraintViolation> violations = validator.validate();
            result.addAll(violations);
        }
        return result.build();
    }
}
