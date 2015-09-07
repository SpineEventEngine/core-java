/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
package org.spine3.server;

import com.google.protobuf.Message;

/**
 * Indicates that more than one subscriber for the same message class are present in a declaring class.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class DuplicateSubscriberException extends RuntimeException {

    public DuplicateSubscriberException(
            Class<? extends Message> messageClass,
            MessageSubscriber currentSubscriber,
            MessageSubscriber discoveredSubscriber) {

        super(String.format(
                "The %s class defines more than one subscriber method for the message class %s." +
                        " Subscribers encountered: %s, %s.",
                currentSubscriber.getTargetClass().getName(), messageClass.getName(),
                currentSubscriber.getShortName(), discoveredSubscriber.getShortName()));
    }

    private static final long serialVersionUID = 0L;

}
