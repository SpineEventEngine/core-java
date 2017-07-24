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
package io.spine.core;

import com.google.protobuf.Message;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wraps the business failure into a transferable parcel which provides a convenient access to
 * its properties.
 *
 * @author Alex Tymchenko
 */
public class FailureEnvelope extends AbstractMessageEnvelope<FailureId, Failure> {

    /**
     * The failure message.
     */
    private final Message failureMessage;

    /**
     * The failure class.
     */
    private final FailureClass failureClass;

    /**
     * The message of a {@link Command}, which processing triggered the failure.
     */
    private final Message commandMessage;

    /**
     * The context of a {@link Command}, which processing triggered the failure.
     */
    private final CommandContext commandContext;

    private FailureEnvelope(Failure failure) {
        super(failure);
        this.failureMessage = Failures.getMessage(failure);
        this.failureClass = FailureClass.of(failureMessage);
        this.commandMessage = Commands.getMessage(failure.getContext()
                                                         .getCommand());
        this.commandContext = failure.getContext()
                                     .getCommand()
                                     .getContext();
    }

    /**
     * Creates instance for the passed failure.
     */
    public static FailureEnvelope of(Failure failure) {
        checkNotNull(failure);
        return new FailureEnvelope(failure);
    }

    /**
     * Obtains the Failure ID.
     */
    @Override
    public FailureId getId() {
        return getOuterObject().getId();
    }

    @Override
    public Message getMessage() {
        return failureMessage;
    }

    @Override
    public FailureClass getMessageClass() {
        return failureClass;
    }

    @Override
    public ActorContext getActorContext() {
        return getCommandContext().getActorContext();
    }

    /**
     * Sets the context of the failure as the context origin of the event being built.
     *
     * @param builder event context builder into which set the event origin context
     */
    @Override
    public void setOriginContext(EventContext.Builder builder) {
        builder.setFailureContext(getOuterObject().getContext());
    }

    public Message getCommandMessage() {
        return commandMessage;
    }

    public CommandContext getCommandContext() {
        return commandContext;
    }
}
