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
package org.spine3.server.stand;

import com.google.common.base.Optional;
import org.spine3.base.Error;
import org.spine3.client.Subscription;
import org.spine3.client.SubscriptionValidationError;
import org.spine3.server.tenant.TenantAwareFunction;
import org.spine3.users.TenantId;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.client.SubscriptionValidationError.INVALID_SUBSCRIPTION;
import static org.spine3.client.SubscriptionValidationError.UNKNOWN_SUBSCRIPTION;

/**
 * Validates the {@linkplain Subscription} instances submitted to {@linkplain Stand}.
 *
 * @author Alex Tymchenko
 */
class SubscriptionValidator extends RequestValidator<Subscription> {

    private final SubscriptionRegistry registry;

    /**
     * Creates an instance of {@code SubscriptionValidator} based on the subscription registry.
     *
     * @param registry the registry to validate the subscription against.
     */
    SubscriptionValidator(SubscriptionRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected SubscriptionValidationError getInvalidMessageErrorCode() {
        return INVALID_SUBSCRIPTION;
    }

    @Override
    protected InvalidSubscriptionException onInvalidMessage(String exceptionMsg,
                                                            Subscription subscription,
                                                            Error error) {
        return new InvalidSubscriptionException(exceptionMsg, subscription, error);
    }

    @Override
    protected Optional<RequestNotSupported<Subscription>> isSupported(Subscription request) {
        final boolean includedInRegistry = checkInRegistry(request);

        if (includedInRegistry) {
            return Optional.absent();
        }

        return Optional.of(missingInRegistry());
    }

    private static RequestNotSupported<Subscription> missingInRegistry() {
        return new RequestNotSupported<Subscription>(
                UNKNOWN_SUBSCRIPTION, "Cannot find the subscription in the registry") {

            @Override
            protected InvalidRequestException createException(String errorMessage,
                                                              Subscription request,
                                                              Error error) {
                return new InvalidSubscriptionException(errorMessage, request, error);
            }
        };
    }

    private boolean checkInRegistry(Subscription request) {
        final TenantId tenantId = request.getTopic()
                                         .getContext()
                                         .getTenantId();
        final Boolean result = new TenantAwareFunction<Subscription, Boolean>(tenantId) {

            @Nullable
            @Override
            public Boolean apply(@Nullable Subscription input) {
                checkNotNull(input);
                final boolean result = registry.contains(input.getId());
                return result;
            }
        }.apply(request);

        checkNotNull(result);
        return result;
    }
}
