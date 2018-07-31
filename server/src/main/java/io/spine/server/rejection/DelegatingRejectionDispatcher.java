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

import com.google.common.base.MoreObjects;
import io.spine.annotation.Internal;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.logging.Logging;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.string.Stringifiers;
import io.spine.type.MessageClass;
import org.slf4j.Logger;

import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;
import static java.lang.String.format;

/**
 * A {@link RejectionDispatcher}, that delegates the responsibilities to an aggregated
 * {@linkplain RejectionDispatcherDelegate delegate instance}.
 *
 * @param <I> the type of entity IDs to which dispatch rejections
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 * @see RejectionDispatcherDelegate
 */
@Deprecated
@Internal
public final class DelegatingRejectionDispatcher<I> implements RejectionDispatcher<I> {

    /** A target delegate. */
    private final RejectionDispatcherDelegate<I> delegate;

    /** Lazily initialized logger. */
    private final Supplier<Logger> loggerSupplier = Logging.supplyFor(getClass());

    private DelegatingRejectionDispatcher(RejectionDispatcherDelegate<I> delegate) {
        this.delegate = delegate;
    }

    public static <I> DelegatingRejectionDispatcher<I> of(RejectionDispatcherDelegate<I> delegate) {
        checkNotNull(delegate);
        return new DelegatingRejectionDispatcher<>(delegate);
    }

    @Override
    public Set<RejectionClass> getMessageClasses() {
        return delegate.getRejectionClasses();
    }

    @Override
    public Set<I> dispatch(RejectionEnvelope envelope) {
        checkNotNull(envelope);
        return delegate.dispatchRejection(envelope);
    }

    @Override
    public void onError(RejectionEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);
        delegate.onError(envelope, exception);
    }

    /**
     * Returns the string representation of this dispatcher.
     *
     * <p>Includes an FQN of the {@code delegate} in order to allow distinguish
     * {@code DelegatingRejectionDispatcher} instances with different delegates.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("rejectionDelegate", delegate.getClass())
                          .toString();
    }

    /**
     * Wraps this dispatcher to an external rejection dispatcher.
     *
     * @return the external rejection dispatcher proxying calls to the underlying instance
     */
    public ExternalMessageDispatcher<I> getExternalDispatcher() {
        return new ExternalMessageDispatcher<I>() {
            @Override
            public Set<ExternalMessageClass> getMessageClasses() {
                Set<RejectionClass> rejectionClasses = delegate.getExternalRejectionClasses();
                return ExternalMessageClass.fromRejectionClasses(rejectionClasses);
            }

            @Override
            public Set<I> dispatch(ExternalMessageEnvelope envelope) {
                ExternalMessage externalMessage = envelope.getOuterObject();
                Rejection rejection = unpack(externalMessage.getOriginalMessage());
                Set<I> ids = delegate.dispatchRejection(RejectionEnvelope.of(rejection));
                return ids;
            }

            @Override
            public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
                MessageClass messageClass = envelope.getMessageClass();
                String messageId = Stringifiers.toString(envelope.getId());
                String errorMessage =
                        format("Error dispatching external rejection (class: %s, id: %s)",
                               messageClass, messageId);
                log().error(errorMessage, exception);
            }
        };
    }

    private Logger log() {
        return loggerSupplier.get();
    }
}
