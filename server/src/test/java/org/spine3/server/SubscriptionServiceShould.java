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

package org.spine3.server;

import org.junit.Test;
import org.spine3.server.stand.Stand;
import org.spine3.server.storage.memory.InMemoryStandStorage;
import org.spine3.server.storage.memory.InMemoryStorageFactory;

import static org.junit.Assert.assertNotNull;

/**
 * @author Dmytro Dashenkov
 */
public class SubscriptionServiceShould {

    /*
     * Creation tests
     * --------------
     */

    @Test
    public void initialize_properly_with_one_bounded_context() {
        final BoundedContext singleBoundedContext = newBoundedContext("Single");

        final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                           .addBoundedContext(singleBoundedContext)
                                                                           .build();

        assertNotNull(subscriptionService);
    }

    @Test
    public void initialize_properly_with_several_bounded_contexts() {
        final BoundedContext firstBoundedContext = newBoundedContext("First");
        final BoundedContext secondBoundedContext = newBoundedContext("Second");
        final BoundedContext thirdBoundedContext = newBoundedContext("Third");


        final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                           .addBoundedContext(firstBoundedContext)
                                                                           .addBoundedContext(secondBoundedContext)
                                                                           .addBoundedContext(thirdBoundedContext)
                                                                           .build();

        assertNotNull(subscriptionService);
    }

    @Test
    public void be_able_to_remove_bounded_context_from_builder() {
        final BoundedContext firstBoundedContext = newBoundedContext("Removed");
        final BoundedContext secondBoundedContext = newBoundedContext("Also removed");
        final BoundedContext thirdBoundedContext = newBoundedContext("The one to stay");


        final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                           .addBoundedContext(firstBoundedContext)
                                                                           .addBoundedContext(secondBoundedContext)
                                                                           .addBoundedContext(thirdBoundedContext)
                                                                           .removeBoundedContext(secondBoundedContext)
                                                                           .removeBoundedContext(firstBoundedContext)
                                                                           .build();

        assertNotNull(subscriptionService);
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_initialize_from_empty_builder() {
        SubscriptionService.newBuilder().build();
    }

    private static BoundedContext newBoundedContext(String name) {
        final Stand stand = Stand.newBuilder().setStorage(InMemoryStandStorage.newBuilder().build()).build();

        return BoundedContext.newBuilder()
                             .setStand(stand)
                             .setName(name)
                             .setStorageFactory(InMemoryStorageFactory.getInstance())
                             .build();

    }
}
