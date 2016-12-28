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
package org.spine3.server.aggregate.error;

import com.google.protobuf.Message;
import org.spine3.base.Identifiers;

/**
 * Exception is thrown if a command, which is intended to be used for an aggregate,
 * does not have {@code getAggregateId()} method.
 *
 * <p>To have this method in Java, corresponding Protobuf message definition must have
 * the property called {@code aggregate_id}.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class MissingAggregateIdException extends RuntimeException {

    private static final long serialVersionUID = 0L;

    public MissingAggregateIdException(Message command, String methodName, Throwable cause) {
        super(createMessage(command, methodName), cause);
    }

    private static String createMessage(Message command, String methodName) {
        return "Unable to call ID accessor method " + methodName + " from the command of class "
                + command.getClass().getName();
    }

    public MissingAggregateIdException(String commandClassName, String propertyName) {
        super("The first property of the aggregate command " + commandClassName +
                " must define aggregate ID with a name ending with '" + Identifiers.ID_PROPERTY_SUFFIX + "'. Found: " + propertyName);
    }
}
