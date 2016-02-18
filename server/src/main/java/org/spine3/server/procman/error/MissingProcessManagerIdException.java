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
package org.spine3.server.procman.error;

import static org.spine3.base.Identifiers.ID_PROPERTY_SUFFIX;

/**
 * Exception is thrown if a command/event, which is intended to be used for a process manager,
 * does not have {@code getProcessManagerId()} method.
 *
 * <p>To have this method in Java, corresponding Protobuf message definition must have
 * the property called {@code process_manager_id}.
 *
 * @author Alexander Litus
 */
public class MissingProcessManagerIdException extends RuntimeException {

    public MissingProcessManagerIdException(String messageClassName, String propertyName, int fieldIndex) {
        super(createMessage(messageClassName, propertyName, fieldIndex));
    }

    private static String createMessage(String messageClassName, String propertyName, int fieldIndex) {
        return "The property with the index '" + fieldIndex + "' of the process manager message " + messageClassName +
                " must define a process manager ID with a name ending with '" + ID_PROPERTY_SUFFIX +
                "'. Found property: " + propertyName;
    }

    private static final long serialVersionUID = 348L;
}
