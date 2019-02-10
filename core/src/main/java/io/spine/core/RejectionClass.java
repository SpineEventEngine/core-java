/*
 * Copyright 2019, TeamDev. All rights reserved.
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
package io.spine.core;

import com.google.protobuf.Message;
import io.spine.base.RejectionMessage;
import io.spine.type.MessageClass;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Events.ensureMessage;

/**
 * A value object holding a class of a business rejection.
 */
public class RejectionClass extends MessageClass<RejectionMessage> {

    private static final long serialVersionUID = 0L;

    protected RejectionClass(Class<? extends RejectionMessage> value) {
        super(value);
    }

    /**
     * Creates a new instance of the rejection class.
     *
     * @param value a value to hold
     * @return new instance
     */
    public static RejectionClass of(Class<? extends RejectionMessage> value) {
        return new RejectionClass(checkNotNull(value));
    }

    /**
     * Creates a new instance of the rejection class by passed rejection instance.
     *
     * <p>If an instance of {@link Event} (which implements {@code Message}) is
     * passed to this method, enclosing rejection message will be un-wrapped to determine
     * the class of the rejection.
     *
     * @param rejectionOrMessage a rejection instance
     * @return new instance
     */
    public static RejectionClass of(Message rejectionOrMessage) {
        RejectionMessage message = (RejectionMessage) ensureMessage(rejectionOrMessage);
        RejectionClass result = of(message.getClass());
        return result;
    }
}
