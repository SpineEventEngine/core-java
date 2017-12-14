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

package io.spine.server.event;

import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;
import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.string.Stringifiers;
import io.spine.type.MessageClass;
import io.spine.util.Logging;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * A {@link EventDispatcher} which delegates the responsibilities to an aggregated {@link
 * EventDispatcherDelegate delegate instance}.
 *
 * @param <I> the type of entity IDs
 * @author Alexander Yevsyukov
 * @see EventDispatcherDelegate
 */
@Internal
public final class DelegatingEventDispatcher<I> implements EventDispatcher<I> {

    /**
     * A target delegate.
     */
    private final EventDispatcherDelegate<I> delegate;

    /** Lazily initialized logger. */
    private final Supplier<Logger> loggerSupplier = Logging.supplyFor(getClass());

    /**
     * Creates a new instance of {@code DelegatingCommandDispatcher}, proxying the calls
     * to the passed {@code delegate}.
     *
     * @param delegate a delegate to pass the dispatching duties to
     * @return new instance
     */
    public static <I> DelegatingEventDispatcher<I> of(EventDispatcherDelegate<I> delegate) {
        checkNotNull(delegate);
        return new DelegatingEventDispatcher<>(delegate);
    }

    private DelegatingEventDispatcher(EventDispatcherDelegate<I> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<EventClass> getMessageClasses() {
        return delegate.getEventClasses();
    }

    @Override
    public Set<I> dispatch(EventEnvelope envelope) {
        return delegate.dispatchEvent(envelope);
    }

    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        delegate.onError(envelope, exception);
    }

    /**
     * Wraps this dispatcher to an external event dispatcher.
     *
     * @return the external rejection dispatcher proxying calls to the underlying instance
     */
    public ExternalMessageDispatcher<I> getExternalDispatcher() {
        return new ExternalMessageDispatcher<I>() {
            @Override
            public Set<ExternalMessageClass> getMessageClasses() {
                final Set<EventClass> eventClasses = delegate.getExternalEventClasses();
                return ExternalMessageClass.fromEventClasses(eventClasses);
            }

            @Override
            public Set<I> dispatch(ExternalMessageEnvelope envelope) {
                final ExternalMessage externalMessage = envelope.getOuterObject();
                final Event event = unpack(externalMessage.getOriginalMessage());
                final Set<I> ids = delegate.dispatchEvent(EventEnvelope.of(event));
                return ids;
            }

            @Override
            public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
                final MessageClass messageClass = envelope.getMessageClass();
                final String messageId = Stringifiers.toString(envelope.getId());
                final String errorMessage =
                        Error.DISPATCHING_EXTERNAL_EVENT.format(messageClass, messageId);
                log().error(errorMessage, exception);
            }
        };
    }

    private Logger log() {
        return loggerSupplier.get();
    }

    /**
     * Returns the string representation of this dispatcher.
     *
     * <p>Includes an FQN of the {@code delegate} in order to allow distinguish
     * {@code DelegatingEventDispatcher} instances with different delegates.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("eventDelegate", delegate.getClass())
                          .toString();
    }
}
