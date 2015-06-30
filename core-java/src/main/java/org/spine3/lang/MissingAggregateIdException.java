/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.lang;

import com.google.protobuf.Message;
import org.spine3.util.Commands;

/**
 * Exception is thrown if a command, which is intended to be used for an aggregate
 * does not have {@code getAggregateId()} method.
 * <p/>
 * To have this method in Java, corresponding Protobuf message definition must have
 * the property called {@code aggregate_id}.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class MissingAggregateIdException extends RuntimeException {

    public MissingAggregateIdException(Message command, String methodName, Throwable cause) {
        super(createMessage(command, methodName), cause);
    }

    private static String createMessage(Message command, String methodName) {
        return "Unable to call ID accessor method " + methodName + " from the command of class "
                + command.getClass().getName();
    }

    public MissingAggregateIdException(String commandClassName, String propertyName) {
        super("The first property of the aggregate command " + commandClassName +
                " must define aggregate ID with a name ending with '" + Commands.ID_PROPERTY_SUFFIX + "'. Found: " + propertyName);
    }

    private static final long serialVersionUID = 0L;
}
