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

package io.spine.server.bus.given.stock;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.tuple.Pair;
import io.spine.test.bus.Buy;
import io.spine.test.bus.Price;
import io.spine.test.bus.PriceDropped;
import io.spine.test.bus.PriceRaised;
import io.spine.test.bus.Sell;
import io.spine.test.bus.Share;
import io.spine.test.bus.ShareId;
import io.spine.test.bus.ShareTraded;

import java.math.BigDecimal;

import static java.lang.Math.max;
import static java.math.BigDecimal.ROUND_UP;

public final class ShareAggregate extends Aggregate<ShareId, Share, Share.Builder> {

    @Assign
    Pair<ShareTraded, PriceRaised> handle(Buy command) {
        int amount = command.getAmount();
        ShareTraded traded = ShareTraded
                .newBuilder()
                .setShare(id())
                .setAmount(amount)
                .vBuild();
        float percent = max(amount / 100.0f, 50.0f);
        PriceRaised raised = PriceRaised
                .newBuilder()
                .setShare(id())
                .setPercent(percent)
                .build();
        return Pair.of(traded, raised);
    }

    @Assign
    Pair<ShareTraded, PriceDropped> handle(Sell command) {
        int amount = command.getAmount();
        ShareTraded traded = ShareTraded
                .newBuilder()
                .setShare(id())
                .setAmount(amount)
                .vBuild();
        float percent = max(amount / 100.0f, 50.0f);
        PriceDropped raised = PriceDropped
                .newBuilder()
                .setShare(id())
                .setPercent(percent)
                .build();
        return Pair.of(traded, raised);
    }

    @Apply
    private void event(ShareTraded event) {
        builder().setId(event.getShare());
    }

    @Apply
    private void event(PriceRaised event) {
        Price.Builder priceBuilder = builder().getPriceBuilder();
        float oldPrice = priceBuilder.getUsd();
        float newPrice = oldPrice + percentage(oldPrice, event.getPercent());
        priceBuilder.setUsd(newPrice);
    }

    @Apply
    private void event(PriceDropped event) {
        Price.Builder priceBuilder = builder().getPriceBuilder();
        float oldPrice = priceBuilder.getUsd();
        float newPrice = oldPrice - percentage(oldPrice, event.getPercent());
        priceBuilder.setUsd(newPrice);
    }

    private static float percentage(float value, float percent) {
        float raise = (value * percent / 100.0f);
        BigDecimal decimalRaise = BigDecimal.valueOf(raise);
        return decimalRaise.setScale(2, ROUND_UP)
                           .floatValue();
    }


}
