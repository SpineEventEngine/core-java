/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.delivery.given;

import io.spine.core.TenantId;
import io.spine.environment.Tests;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.DefaultRepository;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.Delivery;
import io.spine.server.delivery.DeliveryMonitor;
import io.spine.server.delivery.FailedReception;
import io.spine.server.delivery.InboxMessage;
import io.spine.server.delivery.ShardIndex;
import io.spine.server.delivery.ShardObserver;
import io.spine.server.entity.Repository;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.test.delivery.Receptionist;
import io.spine.testing.server.blackbox.BlackBoxContext;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.Optional;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

public final class ReceptionFailureTestEnv {

    private ReceptionFailureTestEnv() {
    }

    public static BlackBoxContext blackBox() {
        Repository<String, ReceptionistAggregate> repository =
                DefaultRepository.of(ReceptionistAggregate.class);
        BlackBoxContext context = BlackBoxContext.from(
                BoundedContextBuilder.assumingTests()
                                     .add(repository)
        );
        return context;
    }

    public static void configureDelivery(ObservingMonitor monitor) {
        Delivery delivery = Delivery.newBuilder()
                                    .setMonitor(monitor)
                                    .build();
        delivery.subscribe(new IgnoringObserver());
        ServerEnvironment.when(Tests.class)
                         .use(delivery);
    }

    public static void sleep() {
        sleepUninterruptibly(Duration.ofMillis(900));
    }

    public static Receptionist receptionist(String receptionistId, int cmdsHandled) {
        return Receptionist.newBuilder()
                           .setId(receptionistId)
                           .setHowManyCmdsHandled(cmdsHandled)
                           .vBuild();
    }

    /**
     * A shard observer which deliberately ignores any exceptions thrown when dispatching
     * inbox messages.
     */
    private static final class IgnoringObserver implements ShardObserver {

        @Override
        public void onMessage(InboxMessage message) {
            new Thread(() -> runDelivery(message)).start();

        }

        @SuppressWarnings("resource")
        private static void runDelivery(InboxMessage message) {
            TenantId tenant = message.tenant();
            ShardIndex index = message.shardIndex();
            Delivery delivery = ServerEnvironment.instance()
                                                 .delivery();
            try {
                TenantAwareRunner.with(tenant)
                                 .run(() -> delivery.deliverMessagesFrom(index));
            } catch (Exception ignored) {
                // Do nothing.
            }
        }
    }

    /**
     * A delivery monitor which remembers the last observed reception failure.
     */
    public static final class ObservingMonitor extends DeliveryMonitor {

        private @Nullable FailedReception lastFailure = null;

        @Override
        public FailedReception.Action onReceptionFailure(FailedReception reception) {
            lastFailure = reception;
            return super.onReceptionFailure(reception);
        }

        public Optional<FailedReception> lastFailure() {
            return Optional.ofNullable(lastFailure);
        }
    }
}
