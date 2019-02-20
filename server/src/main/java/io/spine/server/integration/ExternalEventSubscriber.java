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
package io.spine.server.integration;

import io.spine.logging.Logging;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.string.Stringifiers;
import io.spine.type.MessageClass;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * An internal wrapper class, which exposes an {@link AbstractEventSubscriber}
 * as an {@link ExternalMessageDispatcher}.
 *
 * <p>Allows to register {@code EventSubscriber}s as dispatchers of
 * {@code IntegrationBus}.
 *
 * @author Alex Tymchenko
 */
final class ExternalEventSubscriber implements ExternalMessageDispatcher<String>, Logging {

    private final AbstractEventSubscriber delegate;

    ExternalEventSubscriber(AbstractEventSubscriber delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<ExternalMessageClass> getMessageClasses() {
        Set<EventClass> extSubscriptions = delegate.getExternalEventClasses();
        return ExternalMessageClass.fromEventClasses(extSubscriptions);
    }

    @Override
    public Set<String> dispatch(ExternalMessageEnvelope envelope) {
        EventEnvelope eventEnvelope = envelope.toEventEnvelope();
        checkArgument(eventEnvelope.isExternal(),
                      "External event expected, but got %s",
                      Stringifiers.toString(eventEnvelope.getOuterObject()));
        return delegate.dispatch(eventEnvelope);
    }

    @Override
    public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);

        MessageClass messageClass = envelope.getMessageClass();
        String messageId = envelope.idAsString();
        String errorMessage =
                format("Error handling external event subscription (class: %s id: %s).",
                       messageClass, messageId);
        log().error(errorMessage, exception);
    }

    @Override
    public boolean canDispatch(ExternalMessageEnvelope envelope) {
        return delegate.canDispatch(envelope.toEventEnvelope());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ExternalEventSubscriber that = (ExternalEventSubscriber) o;
        return Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }
}
