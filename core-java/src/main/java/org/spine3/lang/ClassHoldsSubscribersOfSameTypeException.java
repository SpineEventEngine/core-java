/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.lang;

import com.google.protobuf.Message;

/**
 * Indicates that more than one subscriber for message of the same type
 * are present in the one class.
 *
 * @author Mikhail Melnik
 */
public class ClassHoldsSubscribersOfSameTypeException extends RuntimeException {

    public ClassHoldsSubscribersOfSameTypeException(
            Object subscribersHolder, Class<? extends Message> messageClass) {

        super("The " + subscribersHolder.getClass().getName() + " class"
                + " defines more than one subscriber method"
                + " for the message of type " + messageClass.getName() + '.');
    }

    private static final long serialVersionUID = -5347395549242861960L;

}
