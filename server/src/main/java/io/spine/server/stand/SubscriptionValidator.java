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
package io.spine.server.stand;

import io.spine.base.Error;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionValidationError;
import io.spine.core.TenantId;
import io.spine.server.tenant.TenantAwareFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

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
        super();
        this.registry = registry;
    }

    @Override
    protected SubscriptionValidationError getInvalidMessageErrorCode() {
        return SubscriptionValidationError.INVALID_SUBSCRIPTION;
    }

    @Override
    protected InvalidSubscriptionException onInvalidMessage(String exceptionMsg,
                                                            Subscription subscription,
                                                            Error error) {
        return new InvalidSubscriptionException(exceptionMsg, subscription, error);
    }

    @Override
    protected Optional<RequestNotSupported<Subscription>> isSupported(Subscription request) {
        boolean includedInRegistry = checkInRegistry(request);

        if (includedInRegistry) {
            return Optional.empty();
        }

        return Optional.of(missingInRegistry());
    }

    private static RequestNotSupported<Subscription> missingInRegistry() {
        return new RequestNotSupported<Subscription>(
                SubscriptionValidationError.UNKNOWN_SUBSCRIPTION,
                "Cannot find the subscription in the registry") {

            @Override
            protected InvalidRequestException createException(String errorMessage,
                                                              Subscription request,
                                                              Error error) {
                return new InvalidSubscriptionException(errorMessage, request, error);
            }
        };
    }

    private boolean checkInRegistry(Subscription request) {
        TenantId tenantId = request.getTopic()
                                         .getContext()
                                         .getTenantId();
        Boolean result = new TenantAwareFunction<Subscription, Boolean>(tenantId) {

            @Override
            public Boolean apply(@Nullable Subscription input) {
                checkNotNull(input);
                boolean result = registry.containsId(input.getId());
                return result;
            }
        }.execute(request);

        checkNotNull(result);
        return result;
    }
}
