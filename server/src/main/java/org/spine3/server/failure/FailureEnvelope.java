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
import org.spine3.base.AbstractMessageEnvelope;
import org.spine3.base.Failure;
import org.spine3.base.Failures;

/**
 * Wraps the business failure into a transferable parcel for
 * transportation along the {@linkplain FailureBus}.
 *
 * @author Alex Tymchenko
 */
public class FailureEnvelope extends AbstractMessageEnvelope<Failure> {

    private final Message failureMessage;

    protected FailureEnvelope(Failure failure) {
        super(failure);
        this.failureMessage = Failures.getMessage(failure);
    }

    /**
     * Creates instance for the passed failure.
     */
    public static FailureEnvelope of(Failure failure) {
        return new FailureEnvelope(failure);
    }

    @Override
    public Message getMessage() {
        return failureMessage;
    }
}
