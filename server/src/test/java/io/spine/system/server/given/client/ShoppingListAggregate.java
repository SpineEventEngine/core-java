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

package io.spine.system.server.given.client;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.system.server.AddListItem;
import io.spine.system.server.CreateShoppingList;
import io.spine.system.server.HardCopyLost;
import io.spine.system.server.HardCopyPrinted;
import io.spine.system.server.ListItemAdded;
import io.spine.system.server.ShoppingListCreated;
import io.spine.test.system.server.ListId;
import io.spine.test.system.server.ShoppingList;

public class ShoppingListAggregate extends Aggregate<ListId, ShoppingList, ShoppingList.Builder> {

    private ShoppingListAggregate(ListId id) {
        super(id);
    }

    @Assign
    ShoppingListCreated handle(CreateShoppingList command) {
        return ShoppingListCreated
                .newBuilder()
                .setId(command.getId())
                .build();
    }

    @Assign
    ListItemAdded handle(AddListItem command) {
        return ListItemAdded
                .newBuilder()
                .setListId(command.getListId())
                .setItem(command.getItem())
                .build();
    }

    @Apply
    private void on(ShoppingListCreated event) {
        builder().setId(event.getId());
    }

    @Apply
    private void on(ListItemAdded event) {
        builder().addItem(event.getItem());
    }

    @Apply(allowImport = true)
    private void on(HardCopyPrinted event) {
        int newCount = builder().getHardCopiesCount() + 1;
        builder().setHardCopiesCount(newCount);
    }

    @Apply(allowImport = true)
    private void on(HardCopyLost event) {
        ShoppingList.Builder builder = builder();
        int newCount = builder.getHardCopiesCount() - 1;
        if (newCount >= 0) {
            builder.setHardCopiesCount(newCount);
        }
    }
}
