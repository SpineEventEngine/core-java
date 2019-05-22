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

package io.spine.server.bus;

import com.google.common.collect.ImmutableList;
import io.spine.server.DefaultRepository;
import io.spine.server.bus.given.stock.JowDonsIndex;
import io.spine.server.bus.given.stock.ShareAggregate;
import io.spine.test.bus.Buy;
import io.spine.test.bus.Sell;
import io.spine.test.bus.ShareId;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static io.spine.base.Identifier.newUuid;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("When posting commands in parallel")
class DispatchingQueueSynchronisationTest {

    @Test
    @DisplayName("Bus should not lock with its system counterpart")
    void deadlock() throws InterruptedException {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) newFixedThreadPool(10);
        SingleTenantBlackBoxContext context = BlackBoxBoundedContext
                .singleTenant()
                .with(DefaultRepository.of(ShareAggregate.class),
                      new JowDonsIndex.Repository());
        int taskCount = 10;
        ImmutableList<ShareId> shares =
                Stream.generate(() -> ShareId
                        .newBuilder()
                        .setValue(newUuid())
                        .vBuild())
                      .limit(taskCount)
                      .collect(toImmutableList());
        shares.forEach(share -> executor.execute(() -> {
            Buy buy = Buy
                    .newBuilder()
                    .setShare(share)
                    .setAmount(42)
                    .vBuild();
            context.receivesCommand(buy);
            sleepUninterruptibly(1, SECONDS);
            Sell sell = Sell
                    .newBuilder()
                    .setShare(share)
                    .setAmount(12)
                    .vBuild();
            context.receivesCommand(sell);
        }));
        executor.awaitTermination(20, SECONDS);
        assertEquals(shares.size(), executor.getCompletedTaskCount(),
                     "Not all tasks have been executed. Most likely, a dead lock is reached.");
    }
}
