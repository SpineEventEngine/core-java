/*
 * Copyright 2018, TeamDev. All rights reserved.
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
package io.spine.server.rejection;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.MessageEnvelope;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.logging.Logging;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.rejection.model.RejectionSubscriberClass;
import io.spine.server.rejection.model.RejectionSubscriberMethod;
import io.spine.server.tenant.CommandOperation;
import io.spine.string.Stringifiers;
import io.spine.type.MessageClass;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.rejection.model.RejectionSubscriberClass.asRejectionSubscriber;
import static java.lang.String.format;

/**
 * A base for objects subscribing to rejections from {@link RejectionBus}.
 *
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 * @see RejectionBus#register(io.spine.server.bus.MessageDispatcher)
 * @see io.spine.core.Subscribe
 */
public class RejectionSubscriber implements RejectionDispatcher<String>, Logging {

    private final RejectionSubscriberClass<?> thisClass = asRejectionSubscriber(getClass());

    /**
     * {@inheritDoc}
     *
     * @param envelope the envelope with the message
     * @return a one element set with the result of {@link #toString()} as the identity of
     * the subscriber, or empty set if dispatching failed
     */
    @Override
    public Set<String> dispatch(RejectionEnvelope envelope) {
        Command originCommand = envelope.getOuterObject()
                                        .getContext()
                                        .getCommand();
        CommandOperation op = new CommandOperation(originCommand) {

            @Override
            public void run() {
                handle(envelope);
            }
        };
        try {
            op.execute();
        } catch (RuntimeException e) {
            onError(envelope, e);
            return ImmutableSet.of();
        }
        return identity();
    }

    private void handle(RejectionEnvelope rejection) {
        CommandClass commandClass = CommandClass.of(rejection.getCommandMessage());
        RejectionSubscriberMethod method =
                thisClass.getSubscriber(rejection.getMessageClass(), commandClass);
        method.invoke(this, rejection.getMessage(), rejection.getRejectionContext());
    }

    /**
     * Logs the error into the subscriber {@linkplain #log() log}.
     *
     * @param envelope  the message which caused the error
     * @param exception the error
     */
    @Override
    public void onError(RejectionEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);
        MessageClass messageClass = envelope.getMessageClass();
        String messageId = Stringifiers.toString(envelope.getId());
        String errorMessage =
                format("Rejection subscriber (%s) could not handle rejection (class: %s id: %s).",
                       this, messageClass, messageId);
        log().error(errorMessage, exception);
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // as we return an immutable collection.
    public Set<RejectionClass> getMessageClasses() {
        return thisClass.getRejectionClasses();
    }

    @Override
    public Set<RejectionClass> getExternalRejectionClasses() {
        return thisClass.getExternalRejectionClasses();
    }

    @Override
    public Optional<ExternalMessageDispatcher<String>> createExternalDispatcher() {
        if (!dispatchesExternalRejections()) {
            return Optional.empty();
        }
        return Optional.of(new ExternalDispatcher());
    }

    /**
     * Dispatches external events to this subscriber.
     */
    private final class ExternalDispatcher implements ExternalMessageDispatcher<String>, Logging {

        @Override
        public Set<ExternalMessageClass> getMessageClasses() {
            Set<ExternalMessageClass> result =
                    ExternalMessageClass.fromRejectionClasses(
                            thisClass.getExternalRejectionClasses()
                    );
            return result;
        }

        @CanIgnoreReturnValue
        @Override
        public Set<String> dispatch(ExternalMessageEnvelope envelope) {
            ExternalMessage externalMessage = envelope.getOuterObject();
            Rejection rejection = unpack(externalMessage.getOriginalMessage());
            RejectionEnvelope re = RejectionEnvelope.of(rejection);
            return RejectionSubscriber.this.dispatch(re);
        }

        @Override
        public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
            checkNotNull(envelope);
            checkNotNull(exception);
            logError("Error dispatching external rejection to subscriber " +
                             "(rejection class: %s, id: %s)",
                     envelope, exception);
        }

        private void logError(String msgFormat,
                              MessageEnvelope envelope,
                              RuntimeException exception) {
            MessageClass messageClass = envelope.getMessageClass();
            String messageId = Stringifiers.toString(envelope.getId());
            String errorMessage = format(msgFormat, messageClass, messageId);
            _error(errorMessage, exception);
        }
    }
}
