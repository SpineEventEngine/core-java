/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.IterableSubject;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.client.QueryId;
import io.spine.client.Target;
import io.spine.server.BoundedContext;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.stand.given.MenuRepository;
import io.spine.system.server.Mirror;
import io.spine.test.stand.Dish;
import io.spine.test.stand.DishAdded;
import io.spine.test.stand.Menu;
import io.spine.test.stand.MenuId;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.blackbox.BlackBoxContext;
import io.spine.type.TypeName;
import io.spine.validate.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.client.OrderBy.Direction.DESCENDING;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.stand.given.MenuProjection.UUID_COLUMN;
import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("EntityQueryProcessor should")
class EntityQueryProcessorTest {

    private static final int MENU_COUNT = 16;
    private static final TestActorRequestFactory factory =
            new TestActorRequestFactory(EntityQueryProcessorTest.class);
    private static final QueryFactory queries = factory.query();

    private BlackBoxContext context;
    private EntityQueryProcessor processor;

    @BeforeEach
    void setUp() {
        ProjectionRepository<?, ?, ?> repository = new MenuRepository();
        context = BlackBoxContext
                .from(BoundedContext
                              .singleTenant("Cafeteria")
                              .add(repository));
        processor = new EntityQueryProcessor(repository);
        fill();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("read all entities")
    void readAll() {
        Query query = queries.all(Menu.class);
        ImmutableCollection<EntityStateWithVersion> records = processor.process(query);
        assertThat(records).hasSize(MENU_COUNT);
    }

    @Test
    @DisplayName("read one by ID")
    void readById() {
        DishAdded event = addDish();
        MenuId id = event.getId();
        Query query = queries.byIds(Menu.class, ImmutableSet.of(id));
        ImmutableCollection<EntityStateWithVersion> records = processor.process(query);
        assertThat(records).hasSize(1);
        EntityStateWithVersion record = records.asList().get(0);
        Menu menu = state(record);
        assertThat(menu.getId()).isEqualTo(id);
        assertThat(menu.getDishList()).containsExactly(event.getDish());
    }

    @Test
    @DisplayName("read all entities and sort")
    void readAllAndSort() {
        int limit = MENU_COUNT - 4;
        Query query = queries.select(Menu.class)
                             .orderBy(UUID_COLUMN, DESCENDING)
                             .limit(limit)
                             .build();
        ImmutableCollection<EntityStateWithVersion> records = processor.process(query);
        IterableSubject assertRecords = assertThat(records);
        assertRecords.hasSize(limit);
        Comparator<EntityStateWithVersion> uuidOrder = comparing(
                (EntityStateWithVersion record) -> state(record).getId().getUuid()
        );
        assertRecords.isInOrder(uuidOrder.reversed());
    }

    @Test
    @DisplayName("fail if the query does not specify filters and `include_all` is not set")
    void failOnInvalidQuery() {
        Target target = Target
                .newBuilder()
                .setType(TypeName.of(Mirror.class).value())
                .buildPartial();
        Query query = Query
                .newBuilder()
                .setId(QueryId.newBuilder().setValue(newUuid()))
                .setContext(factory.newActorContext())
                .setTarget(target)
                .buildPartial();
        assertThrows(ValidationException.class, () -> processor.process(query));
    }

    private void fill() {
        for (int i = 0; i < MENU_COUNT; i++) {
            addDish();
        }
    }

    @CanIgnoreReturnValue
    private DishAdded addDish() {
        Dish dish = Dish
                .newBuilder()
                .setTitle("Dead beef")
                .setPrice(42)
                .vBuild();
        MenuId id = MenuId.generate();
        DishAdded event = DishAdded
                .newBuilder()
                .setId(id)
                .setDish(dish)
                .vBuild();
        context.receivesEvent(event);
        return event;
    }

    private static Menu state(EntityStateWithVersion record) {
        Any state = record.getState();
        Menu menu = unpack(state, Menu.class);
        return menu;
    }
}
