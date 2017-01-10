/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.money;

/**
 * The utility class containing convenience methods for working with {@link Money}.
 *
 * @author Alexander Litus
 * @see Money
 */
public class MoneyUtil {

    private MoneyUtil() {
    }

    /**
     * Creates a new {@code Money} instance.
     *
     * @param amount   the amount of minor currency units (for currencies whose minor units are used, e.g. "cents")
     *                 or the amount of major currency units (for currencies whose minor currency units are unused
     *                 due to negligible value or do not exist at all)
     * @param currency the currency of the amount of money
     */
    public static Money newMoney(long amount, Currency currency) {
        final Money.Builder result = Money.newBuilder()
                                          .setAmount(amount)
                                          .setCurrency(currency);
        return result.build();
    }
}
