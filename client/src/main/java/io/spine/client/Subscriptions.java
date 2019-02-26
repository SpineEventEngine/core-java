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
package io.spine.client;

import io.spine.annotation.Internal;
import io.spine.base.Identifier;

import static java.lang.String.format;

/**
 * Utility class for working with {@linkplain Subscription subscriptions}.
 */
@Internal
public final class Subscriptions {

    /**
     * The format of all {@linkplain SubscriptionId Subscription identifiers}.
     */
    private static final String SUBSCRIPTION_ID_FORMAT = "s-%s";

    /**
     * The format for convenient subscription printing in logs and error messages.
     */
    private static final String SUBSCRIPTION_PRINT_FORMAT = "(ID: %s, target: %s)";

    /** Prevents the utility class instantiation. */
    private Subscriptions() {
    }

    /**
     * Generates a new subscription identifier.
     *
     * <p>The result is based upon UUID generation.
     *
     * @return new subscription identifier.
     */
    public static SubscriptionId generateId() {
        String formattedId = format(SUBSCRIPTION_ID_FORMAT, Identifier.newUuid());
        return newId(formattedId);
    }

    /**
     * Wraps a given {@code String} as a subscription identifier.
     *
     * <p>Should not be used in production. Use {@linkplain #generateId() automatic generation}
     * instead.
     *
     * @return new subscription identifier.
     */
    public static SubscriptionId newId(String value) {
        return SubscriptionId.newBuilder()
                             .setValue(value)
                             .build();
    }

    /**
     * Obtains a short printable form of subscription.
     *
     * <p>Standard {@link Subscription#toString()} includes all subscription data and thus its
     * output is too huge to use in short log messages and stack traces.
     *
     * @return a printable {@code String} with core subscription data
     */
    public static String toShortString(Subscription subscription) {
        String id = subscription.getId()
                                .getValue();
        String type = subscription.getTopic()
                                  .getTarget()
                                  .getType();
        String result = format(SUBSCRIPTION_PRINT_FORMAT, id, type);
        return result;
    }
}
