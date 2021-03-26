/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.storage.system;

import com.google.common.testing.NullPointerTester;
import com.google.common.truth.Subject;
import io.spine.environment.Environment;
import io.spine.environment.Production;
import io.spine.environment.Tests;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.ContextSpec;
import io.spine.server.ServerEnvironment;
import io.spine.server.aggregate.AggregateStorage;
import io.spine.server.delivery.CatchUpStorage;
import io.spine.server.delivery.InboxStorage;
import io.spine.server.event.EventStore;
import io.spine.server.event.store.EmptyEventStore;
import io.spine.server.storage.MessageRecordSpec;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.storage.system.given.MemoizingStorageFactory;
import io.spine.server.storage.system.given.TestAggregate;
import io.spine.system.server.Company;
import io.spine.system.server.CompanyId;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SystemAwareStorageFactory should")
class SystemAwareStorageFactoryTest {

    private static final ContextSpec CONTEXT = ContextSpec.multitenant("foo");

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void nulls() {
        new NullPointerTester()
                .testAllPublicStaticMethods(SystemAwareStorageFactory.class);
    }

    @Test
    @DisplayName("wrap production storage")
    void wrapProdStorage() {
        Environment.instance()
                   .setTo(Production.class);

        ServerEnvironment serverEnv = ServerEnvironment.instance();
        StorageFactory productionStorage = new MemoizingStorageFactory();
        ServerEnvironment.when(Production.class)
                         .use(productionStorage);
        StorageFactory storageFactory = serverEnv.storageFactory();
        assertThat(storageFactory)
                .isInstanceOf(SystemAwareStorageFactory.class);
        SystemAwareStorageFactory systemAware = (SystemAwareStorageFactory) storageFactory;
        assertThat(systemAware.delegate())
                .isEqualTo(productionStorage);

        Environment.instance()
                   .reset();
    }

    @Test
    @DisplayName("wrap test storage")
    void wrapTestStorage() {
        ServerEnvironment serverEnv = ServerEnvironment.instance();
        StorageFactory testStorage = InMemoryStorageFactory.newInstance();
        ServerEnvironment.when(Tests.class)
                         .use(testStorage);
        StorageFactory storageFactory = serverEnv.storageFactory();
        assertThat(storageFactory).isInstanceOf(SystemAwareStorageFactory.class);
        SystemAwareStorageFactory systemAware = (SystemAwareStorageFactory) storageFactory;
        assertThat(systemAware.delegate())
                .isEqualTo(testStorage);
    }

    @Test
    @DisplayName("delegate aggregate storage creation to given factory")
    void delegateAggregateStorage() {
        MemoizingStorageFactory factory = new MemoizingStorageFactory();
        SystemAwareStorageFactory systemAware = SystemAwareStorageFactory.wrap(factory);
        Class<TestAggregate> aggregateClass = TestAggregate.class;
        AggregateStorage<CompanyId, Company> storage =
                systemAware.createAggregateStorage(CONTEXT, aggregateClass);
        assertThat(storage).isNull();
        assertThat(factory.requestedStorages())
                .containsExactly(aggregateClass);
    }

    @Test
    @DisplayName("delegate record storage creation to given factory")
    void delegateRecordStorage() {
        MemoizingStorageFactory factory = new MemoizingStorageFactory();
        SystemAwareStorageFactory systemAware = SystemAwareStorageFactory.wrap(factory);
        Class<Project> recordType = Project.class;
        MessageRecordSpec<ProjectId, Project> spec =
                new MessageRecordSpec<>(ProjectId.class, Project.class,
                                        i -> ProjectId.getDefaultInstance());
        RecordStorage<ProjectId, Project> storage = systemAware.createRecordStorage(CONTEXT, spec);
        assertThat(storage).isNull();
        assertThat(factory.requestedStorages())
                .containsExactly(recordType);
    }

    @Test
    @DisplayName("delegate inbox storage creation to given factory")
    void delegateInboxStorage() {
        MemoizingStorageFactory factory = new MemoizingStorageFactory();
        SystemAwareStorageFactory systemAware = SystemAwareStorageFactory.wrap(factory);
        InboxStorage storage = systemAware.createInboxStorage(CONTEXT.isMultitenant());
        assertThat(storage).isNull();
        assertTrue(factory.requestedInbox());
    }

    @Test
    @DisplayName("delegate catch-up storage creation to given factory")
    void delegateCatchUpStorage() {
        MemoizingStorageFactory factory = new MemoizingStorageFactory();
        SystemAwareStorageFactory systemAware = SystemAwareStorageFactory.wrap(factory);
        CatchUpStorage storage = systemAware.createCatchUpStorage(CONTEXT.isMultitenant());
        assertThat(storage).isNull();
        assertTrue(factory.requestedCatchUp());
    }

    @Test
    @DisplayName("delegate EventStore creation to given factory")
    void delegateNormalEventStore() {
        MemoizingStorageFactory factory = new MemoizingStorageFactory();
        SystemAwareStorageFactory systemAware = SystemAwareStorageFactory.wrap(factory);
        EventStore store = systemAware.createEventStore(CONTEXT);
        assertThat(store).isNull();
        assertTrue(factory.requestedEventStore());
    }

    @Test
    @DisplayName("create `EmptyEventStore` if event persistence is disabled")
    void createEmptyEventStore() {
        MemoizingStorageFactory factory = new MemoizingStorageFactory();
        SystemAwareStorageFactory systemAware = SystemAwareStorageFactory.wrap(factory);
        BoundedContextBuilder contextBuilder =
                BoundedContext.multitenant(CONTEXT.name()
                                                  .getValue());
        BoundedContext context = contextBuilder.build();
        BoundedContext systemContext = systemOf(context);
        ContextSpec systemSpec = systemContext.spec();
        assertFalse(systemSpec.storesEvents());
        EventStore store = systemAware.createEventStore(systemSpec);
        assertFalse(factory.requestedEventStore());
        Subject assertStore = assertThat(store);
        assertStore.isNotNull();
        assertStore.isInstanceOf(EmptyEventStore.class);
    }

    @Test
    @DisplayName("wrap other factories only once")
    void wrapIdempotently() {
        MemoizingStorageFactory factory = new MemoizingStorageFactory();
        SystemAwareStorageFactory wrapped = SystemAwareStorageFactory.wrap(factory);
        SystemAwareStorageFactory wrappedTwice = SystemAwareStorageFactory.wrap(wrapped);
        assertThat(wrappedTwice).isEqualTo(wrapped);
        assertThat(wrappedTwice.delegate()).isEqualTo(factory);
    }

    @Test
    @DisplayName("close delegate")
    void close() throws Exception {
        MemoizingStorageFactory factory = new MemoizingStorageFactory();
        SystemAwareStorageFactory wrapped = SystemAwareStorageFactory.wrap(factory);
        assertFalse(factory.isClosed());
        wrapped.close();
        assertTrue(factory.isClosed());
    }
}
