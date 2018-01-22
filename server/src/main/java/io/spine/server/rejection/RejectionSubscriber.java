/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import io.spine.core.Command;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.server.model.Model;
import io.spine.server.tenant.CommandOperation;
import io.spine.string.Stringifiers;
import io.spine.type.MessageClass;
import io.spine.util.Logging;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * A base for objects subscribing to rejections from {@link RejectionBus}.
 *
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 * @see RejectionBus#register(io.spine.server.bus.MessageDispatcher)
 * @see io.spine.core.Subscribe
 */
public class RejectionSubscriber implements RejectionDispatcher<String> {

    private final RejectionSubscriberClass<?> thisClass = Model.getInstance()
                                                               .asRejectionSubscriber(getClass());

    /** Lazily initialized logger. */
    private final Supplier<Logger> loggerSupplier = Logging.supplyFor(getClass());

    /**
     * {@inheritDoc}
     *
     * @param envelope the envelope with the message
     * @return a one element set with the result of {@link #toString()} as the identity of
     * the subscriber, or empty set if dispatching failed
     */
    @Override
    public Set<String> dispatch(final RejectionEnvelope envelope) {
        final Command originCommand = envelope.getOuterObject()
                                              .getContext()
                                              .getCommand();
        final CommandOperation op = new CommandOperation(originCommand) {

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
        return Identity.of(this);
    }

    private void handle(RejectionEnvelope rejection) {
        final RejectionSubscriberMethod method =
                thisClass.getSubscriber(rejection.getMessageClass());
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
        final MessageClass messageClass = envelope.getMessageClass();
        final String messageId = Stringifiers.toString(envelope.getId());
        final String errorMessage =
                format("Rejection subscriber (%s) could not handle rejection (class: %s id: %s).",
                       this, messageClass, messageId);
        log().error(errorMessage, exception);
    }

    /**
     * Obtains the instance of logger associated with the class of the subscriber.
     */
    protected Logger log() {
        return loggerSupplier.get();
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // as we return an immutable collection.
    public Set<RejectionClass> getMessageClasses() {
        return thisClass.getRejectionSubscriptions();
    }
}
