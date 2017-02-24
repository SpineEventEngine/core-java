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
package org.spine3.server.failure;

import com.google.protobuf.Message;
import org.spine3.base.Failure;
import org.spine3.base.Failures;
import org.spine3.base.MessageClass;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value object holding a class of a business failure.
 *
 * @author Alex Tymchenko
 */
public class FailureClass extends MessageClass {

    protected FailureClass(Class<? extends Message> value) {
        super(value);
    }

    /**
     * Creates a new instance of the failure class.
     *
     * @param value a value to hold
     * @return new instance
     */
    public static FailureClass of(Class<? extends Message> value) {
        return new FailureClass(checkNotNull(value));
    }

    /**
     * Creates a new instance of the failure class by passed failure instance.
     *
     * <p>If an instance of {@link Failure} (which implements {@code Message}) is passed to this method,
     * enclosing failure message will be un-wrapped to determine the class of the failure.
     *
     * @param failure a failure instance
     * @return new instance
     */
    public static FailureClass of(Message failure) {
        final Message message = checkNotNull(failure);
        if (message instanceof Failure) {
            final Failure failureRecord = (Failure) failure;
            final Message enclosed = Failures.getMessage(failureRecord);
            return of(enclosed.getClass());
        }
        final FailureClass result = of(message.getClass());
        return result;
    }
}
