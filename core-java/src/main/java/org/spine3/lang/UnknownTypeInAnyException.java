/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.lang;

/**
 * Exception that is thrown when unsupported message is obtained
 * or in case there is no class for given Protobuf message.
 *
 * @author Mikhail Melnik
 */
public class UnknownTypeInAnyException extends RuntimeException {

    public UnknownTypeInAnyException(String typeUrl) {
        super("There is no appropriate Java class for the Protobuf message: " + typeUrl);
    }

    private static final long serialVersionUID = 0L;

}
