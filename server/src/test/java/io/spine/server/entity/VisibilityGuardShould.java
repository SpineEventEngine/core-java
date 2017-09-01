/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.entity;

import com.google.common.collect.Lists;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import io.spine.option.EntityOption.Visibility;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.entity.storage.Column;
import io.spine.test.entity.FullAccessAggregate;
import io.spine.test.entity.FullAccessAggregateVBuilder;
import io.spine.test.entity.HiddenAggregate;
import io.spine.test.entity.HiddenAggregateVBuilder;
import io.spine.test.entity.SubscribableAggregate;
import io.spine.test.entity.SubscribableAggregateVBuilder;
import io.spine.type.TypeName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * See `client/spine/test/option/entity_options_should.proto` for definition of messages
 * used for aggregates and repositories in this test.
 *
 * @author Alexander Yevsyukov
 */
public class VisibilityGuardShould {

    private VisibilityGuard guard;
    private List<Repository> repositories;
    private BoundedContext boundedContext;

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        repositories = Lists.newArrayList();

        guard = VisibilityGuard.newInstance();
        register(new ExposedRepository());
        register(new SubscribableRepository());
        register(new HiddenRepository());
    }

    private void register(Repository repository) {
        guard.register(repository);
        repositories.add(repository);
    }

    @After
    public void shutDown() throws Exception {
        boundedContext.close();
        repositories.clear();
    }

    @Test
    public void give_access_to_visible_repos() {
        assertTrue(guard.getRepository(FullAccessAggregate.class)
                        .isPresent());
        assertTrue(guard.getRepository(SubscribableAggregate.class)
                        .isPresent());
    }

    @Test
    public void deny_access_to_invisible_repos() {
        assertFalse(guard.getRepository(HiddenAggregate.class)
                         .isPresent());
    }

    @Test
    public void obtain_repos_by_visibility() {
        final Set<TypeName> full = guard.getEntityTypes(Visibility.FULL);
        assertEquals(1, full.size());
        assertTrue(full.contains(TypeName.of(FullAccessAggregate.class)));

        final Set<TypeName> subscribable = guard.getEntityTypes(Visibility.SUBSCRIBE);
        assertEquals(1, subscribable.size());
        assertTrue(subscribable.contains(TypeName.of(SubscribableAggregate.class)));

        final Set<TypeName> hidden = guard.getEntityTypes(Visibility.NONE);
        assertEquals(1, hidden.size());
        assertTrue(hidden.contains(TypeName.of(HiddenAggregate.class)));
    }

    @Test
    public void shut_down_repositories() {
        guard.shutDownRepositories();

        for (Repository repository : repositories) {
            assertFalse(repository.isOpen());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void do_not_allow_double_registration() {
        register(new ExposedRepository());
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_unregistered_state_class() {
        guard.getRepository(Empty.class);
    }

    @Test(expected = IllegalStateException.class)
    public void cache_entity_columns_on_registration() {
        guard.register(new RepositoryForInvalidEntity());
    }

    @Test
    public void do_not_allow_null_inputs() {
        new NullPointerTester()
                .setDefault(Repository.class, new ExposedRepository())
                .setDefault(Class.class, FullAccessAggregate.class)
                .setDefault(Visibility.class, Visibility.NONE)
                .testAllPublicInstanceMethods(guard);
    }

    private static class Exposed
            extends Aggregate<Long, FullAccessAggregate, FullAccessAggregateVBuilder> {
        private Exposed(Long id) {
            super(id);
        }
    }

    private static class ExposedRepository extends AggregateRepository<Long, Exposed> {
        private ExposedRepository() {
            super();
        }
    }

    private static class Subscribable
            extends Aggregate<Long, SubscribableAggregate, SubscribableAggregateVBuilder> {
        protected Subscribable(Long id) {
            super(id);
        }
    }

    private static class SubscribableRepository extends AggregateRepository<Long, Subscribable> {
        private SubscribableRepository() {
            super();
        }
    }

    private static class Hidden
                   extends Aggregate<String, HiddenAggregate, HiddenAggregateVBuilder> {
        private Hidden(String id) {
            super(id);
        }
    }

    private static class HiddenRepository extends AggregateRepository<String, Hidden> {
        private HiddenRepository() {
            super();
        }
    }

    private static class EntityWithInvalidColumns extends AbstractVersionableEntity<String, Any> {

        private static final String COLUMN_NAME = "columnNameFromAnnotation";

        private EntityWithInvalidColumns(String id) {
            super(id);
        }

        @Column(name = COLUMN_NAME)
        public int getInt() {
            return 0;
        }

        @Column(name = COLUMN_NAME)
        public long getLong() {
            return 0L;
        }
    }

    private static class RepositoryForInvalidEntity
            extends DefaultRecordBasedRepository<String, EntityWithInvalidColumns, Any> {
    }
}
