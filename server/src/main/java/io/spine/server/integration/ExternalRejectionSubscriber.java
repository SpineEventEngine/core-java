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
package io.spine.server.integration;

import com.google.protobuf.Message;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.logging.Logging;
import io.spine.protobuf.AnyPacker;
import io.spine.server.model.Model;
import io.spine.server.rejection.RejectionSubscriber;
import io.spine.server.rejection.model.RejectionSubscriberClass;
import io.spine.string.Stringifiers;
import io.spine.type.MessageClass;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Rejections.isExternal;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;

/**
 * A wrapper class, which exposes a {@link RejectionSubscriber}
 * as an {@link ExternalMessageDispatcher}.
 *
 * <p>Allows to register {@code RejectionSubscriber}s as dispatchers of
 * {@code IntegrationBus}.
 *
 * @author Alex Tymchenko
 */
final class ExternalRejectionSubscriber implements ExternalMessageDispatcher<String> {

    /** Lazily initialized logger. */
    private final Supplier<Logger> loggerSupplier = Logging.supplyFor(getClass());

    private final RejectionSubscriber delegate;

    ExternalRejectionSubscriber(RejectionSubscriber delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<ExternalMessageClass> getMessageClasses() {
        RejectionSubscriberClass<?> subscriberClass = Model.getInstance()
                                                           .asRejectionSubscriber(
                                                                   delegate.getClass());
        Set<RejectionClass> extSubscriptions = subscriberClass.getExternalRejectionSubscriptions();
        return ExternalMessageClass.fromRejectionClasses(extSubscriptions);
    }

    @Override
    public Set<String> dispatch(ExternalMessageEnvelope envelope) {
        ExternalMessage externalMessage = envelope.getOuterObject();
        Message unpacked = AnyPacker.unpack(externalMessage.getOriginalMessage());
        if (!(unpacked instanceof Rejection)) {
            throw newIllegalStateException("Unexpected object %s while dispatching the external " +
                                                   "rejection to the rejection subscriber.",
                                           Stringifiers.toString(unpacked));
        }
        Rejection rejection = (Rejection) unpacked;
        checkArgument(isExternal(rejection.getContext()),
                      "External rejection expected, but got %s",
                      Stringifiers.toString(rejection));
        return delegate.dispatch(RejectionEnvelope.of(rejection));
    }

    @Override
    public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);

        MessageClass messageClass = envelope.getMessageClass();
        String messageId = Stringifiers.toString(envelope.getId());
        String errorMessage =
                format("Error handling external rejection subscription (class: %s id: %s).",
                       messageClass, messageId);
        log().error(errorMessage, exception);
    }

    private Logger log() {
        return loggerSupplier.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ExternalRejectionSubscriber that = (ExternalRejectionSubscriber) o;
        return Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }
}
