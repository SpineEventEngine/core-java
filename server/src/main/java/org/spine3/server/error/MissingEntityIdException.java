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
package org.spine3.server.error;

import org.spine3.base.Identifiers;

import static org.spine3.base.Identifiers.ID_PROPERTY_SUFFIX;

/**
 * This exception is thrown if the corresponding Protobuf command/event message definition does not have
 * an entity ID field whose name ends with the {@link Identifiers#ID_PROPERTY_SUFFIX}.
 *
 * @author Alexander Litus
 */
public class MissingEntityIdException extends RuntimeException {

    private static final long serialVersionUID = 0L;

    public MissingEntityIdException(String messageClassName, String propertyName, int fieldIndex) {
        super(createMessage(messageClassName, propertyName, fieldIndex));
    }

    private static String createMessage(String messageClassName, String propertyName, int fieldIndex) {
        final String message = "The property with the index '" + fieldIndex + "' of the entity message " + messageClassName +
                " must define an entity ID with the name ending with '" + ID_PROPERTY_SUFFIX +
                "'. Found property with the name: " + propertyName;
        return message;
    }
}
