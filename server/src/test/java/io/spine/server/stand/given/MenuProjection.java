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

package io.spine.server.stand.given;

import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.test.stand.Dish;
import io.spine.test.stand.DishAdded;
import io.spine.test.stand.DishRemoved;
import io.spine.test.stand.Menu;
import io.spine.test.stand.MenuId;
import io.spine.test.stand.MenuWithColumns;

import java.util.List;

public final class MenuProjection
        extends Projection<MenuId, Menu, Menu.Builder>
        implements MenuWithColumns {

    public static final String UUID_COLUMN = "uuid";

    @Subscribe
    void on(DishAdded event) {
        builder().addDish(event.getDish());
    }

    @Subscribe
    void on(DishRemoved event) {
        List<Dish> dishes = builder().getDishList();
        for (int i = 0; i < dishes.size(); i++) {
            Dish dish = dishes.get(i);
            if (event.getDish().equals(dish)) {
                builder().removeDish(i);
                return;
            }
        }
    }

    @Override
    public String getUuid() {
        return id().getUuid();
    }
}
