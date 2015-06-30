/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.lang;

import com.google.protobuf.Message;

/**
 * Exception that is thrown when unsupported command is obtained
 * or in case there is no class for given Protobuf command message.
 *
 * @author Mikhail Melnik
 */
public class UnsupportedCommandException extends RuntimeException {

    public UnsupportedCommandException(Message command) {
        super("There is no registered handler for the command: " + command.getClass().getName());
    }

    private static final long serialVersionUID = 0L;

}
