/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.lang;

import com.google.protobuf.Message;
import org.spine3.engine.MessageSubscriber;

/**
 * Exception that is thrown when more than one handler
 * of the same command type were found in the command dispatcher instance.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class CommandHandlerAlreadyRegisteredException extends RuntimeException {

    /**
     * Accepts event type and both old and new handlers.
     *
     * @param commandType type of the command
     * @param currentSubscriber a method currently registered for the given message type
     * @param discoveredSubscriber a new subscriber for the command type
     */
    public CommandHandlerAlreadyRegisteredException(
            Class<? extends Message> commandType,
            MessageSubscriber currentSubscriber,
            MessageSubscriber discoveredSubscriber) {

        super("The command " + commandType + " already has associated handler method" + currentSubscriber.getFullName() + '.'
                + " There can be only one handler per command type. "
                + " You attempt to register handler " + discoveredSubscriber.getFullName() + '.'
                + " If this is an intended operation, consider un-registering the current handler first.");
    }

    private static final long serialVersionUID = 0L;
}
