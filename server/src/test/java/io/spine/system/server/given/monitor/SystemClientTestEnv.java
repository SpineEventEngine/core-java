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

package io.spine.system.server.given.monitor;

import io.spine.server.BoundedContext;
import io.spine.test.system.server.ListId;
import io.spine.test.system.server.ShoppingList;

import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static org.junit.jupiter.api.Assertions.fail;

public class SystemClientTestEnv {

    /**
     * Prevents the utility class instantiation.
     */
    private SystemClientTestEnv() {
    }

    public static BoundedContext contextWithSystemAggregate() {
        BoundedContext context = BoundedContext.newBuilder().build();
        BoundedContext systemContext = systemOf(context);
        systemContext.register(new ShoppingListRepository());
        return context;
    }

    public static ShoppingList findAggregate(ListId aggregateId, BoundedContext context) {
        ShoppingListRepository repository = (ShoppingListRepository)
                context.findRepository(ShoppingList.class)
                       .orElseGet(() -> fail("Repository should be registered."));
        ShoppingListAggregate aggregate =
                repository.find(aggregateId)
                          .orElseGet(() -> fail("Aggregate should be present."));
        return aggregate.getState();
    }
}
