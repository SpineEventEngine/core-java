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

package io.spine.server;

import com.google.common.collect.Lists;
import com.google.common.testing.NullPointerTester;
import io.spine.option.EntityOption.Visibility;
import io.spine.server.entity.Repository;
import io.spine.server.entity.given.VisibilityGuardTestEnv.ExposedRepository;
import io.spine.server.entity.given.VisibilityGuardTestEnv.HiddenRepository;
import io.spine.server.entity.given.VisibilityGuardTestEnv.SubscribableRepository;
import io.spine.system.server.Company;
import io.spine.test.entity.FullAccessAggregate;
import io.spine.test.entity.HiddenAggregate;
import io.spine.test.entity.SubscribableAggregate;
import io.spine.type.TypeName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * See `client/spine/test/option/entity_options_should.proto` for definition of messages
 * used for aggregates and repositories in this test.
 */
@DisplayName("`VisibilityGuard` should")
class VisibilityGuardTest {

    private VisibilityGuard guard;
    private List<Repository<?, ?>> repositories;
    private BoundedContext boundedContext;

    @BeforeEach
    void setUp() {
        boundedContext = BoundedContextBuilder.assumingTests().build();
        repositories = Lists.newArrayList();

        guard = VisibilityGuard.newInstance();
        register(new ExposedRepository());
        register(new SubscribableRepository());
        register(new HiddenRepository());
    }

    private void register(Repository<?, ?> repository) {
        guard.register(repository);
        repositories.add(repository);
    }

    @AfterEach
    void shutDown() throws Exception {
        boundedContext.close();
        repositories.clear();
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(Repository.class, new ExposedRepository())
                .setDefault(Class.class, FullAccessAggregate.class)
                .setDefault(Visibility.class, Visibility.NONE)
                .testAllPublicInstanceMethods(guard);
    }

    @Test
    @DisplayName("give access to visible repos")
    void giveAccessToVisible() {
        assertTrue(guard.repositoryFor(FullAccessAggregate.class)
                        .isPresent());
        assertTrue(guard.repositoryFor(SubscribableAggregate.class)
                        .isPresent());
    }

    @Test
    @DisplayName("deny access to invisible repos")
    void denyAccessToInvisible() {
        assertFalse(guard.repositoryFor(HiddenAggregate.class)
                         .isPresent());
    }

    @Test
    @DisplayName("obtain repos by visibility")
    void obtainByVisibility() {
        var full = guard.entityStateTypes(Visibility.FULL);
        assertEquals(1, full.size());
        assertTrue(full.contains(TypeName.of(FullAccessAggregate.class)));

        var subscribable = guard.entityStateTypes(Visibility.SUBSCRIBE);
        assertEquals(1, subscribable.size());
        assertTrue(subscribable.contains(TypeName.of(SubscribableAggregate.class)));

        var hidden = guard.entityStateTypes(Visibility.NONE);
        assertEquals(1, hidden.size());
        assertTrue(hidden.contains(TypeName.of(HiddenAggregate.class)));
    }

    @Test
    @DisplayName("shutdown repositories")
    void shutdownRepositories() {
        guard.shutDownRepositories();

        for (var repository : repositories) {
            assertFalse(repository.isOpen());
        }
    }

    @Test
    @DisplayName("not allow double registration")
    void forbidDoubleRegistration() {
        assertThrows(IllegalStateException.class, () -> register(new ExposedRepository()));
    }

    @Test
    @DisplayName("reject unregistered state class")
    void rejectUnregisteredStateClass() {
        assertThrows(IllegalStateException.class, () -> guard.repositoryFor(Company.class));
    }
}
