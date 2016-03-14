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
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.google.protobuf.Message;
import org.spine3.validation.options.ValidationProto;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;

/**
 * TODO:2015-12-08:alexander.litus: docs
 *
 * @author Alexander Litus
 */
/* package */ abstract class FieldValidator {

    private boolean isFieldInvalid = false;

    private final List<String> errorMessages = newLinkedList();

    private final FieldDescriptor fieldDescriptor;

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     */
    protected FieldValidator(FieldDescriptor descriptor) {
        this.fieldDescriptor = descriptor;
    }

    // TODO:2016-03-11:alexander.litus: implement and check field types
    /* package */ static FieldValidator newInstance(FieldDescriptor descriptor, Object value) {
        final FieldValidator validator;
        if (value instanceof Message) {
            validator = new MessageFieldValidator(descriptor, (Message) value);
        } else if (value instanceof List) {
            // repeated field
            validator = null;
        } else if (value instanceof Number) {
            validator = new NumberFieldValidator(descriptor, (Number) value);
        } else if (value instanceof String) {
            validator = null;
        } else if (value instanceof ByteString) {
            validator = null;
        } else if (value instanceof Boolean) {
            throw new IllegalArgumentException();
        } else {
            throw new IllegalArgumentException();
        }
        validator.validate();
        return validator;
    }

    protected abstract void validate();

    protected boolean isFieldInvalid() {
        return isFieldInvalid;
    }

    protected void setIsFieldInvalid(boolean isFieldInvalid) {
        this.isFieldInvalid = isFieldInvalid;
    }

    protected List<String> getErrorMessages() {
        return ImmutableList.copyOf(errorMessages);
    }

    protected void addErrorMessage(Message option, String customMsg) {
        final String defaultMsg = option.getDescriptorForType().getOptions().getExtension(ValidationProto.defaultMessage);
        final String msg = customMsg.isEmpty() ? defaultMsg : customMsg;
        final String formattedMsg = format(msg, fieldDescriptor.getFullName());
        errorMessages.add(formattedMsg);
    }

    protected <Option extends Message> Option getOption(GeneratedExtension<FieldOptions, Option> key) {
        final Option option = fieldDescriptor.getOptions().getExtension(key);
        return option;
    }
}
