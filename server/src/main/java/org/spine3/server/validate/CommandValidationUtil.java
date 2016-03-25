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

import com.google.protobuf.Descriptors.FieldDescriptor;
import org.spine3.server.entity.EntityPackagesMap;

/**
 * A utility class for command message fields validation.
 *
 * @author Alexander Litus
 */
/* package */ class CommandValidationUtil {

    private CommandValidationUtil() {}

    /**
     * Returns {@code true} if the field must be an entity ID
     * (if it is the first in a message and the current Protobuf package is for an entity);
     * {@code false} otherwise.
     *
     * @param fieldDescriptor a descriptor of the field to check
     */
    /* package */ static boolean isRequiredEntityIdField(FieldDescriptor fieldDescriptor) {
        final int index = fieldDescriptor.getIndex();
        final boolean isFirst = index == 0;
        final String protoPackage = fieldDescriptor.getFile().getPackage();
        final boolean isCommandForEntity = EntityPackagesMap.contains(protoPackage);
        final boolean result = isFirst && isCommandForEntity;
        return result;
    }
}
