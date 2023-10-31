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
package io.spine.server

import com.example.ForeignContextConfig
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth8
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.annotation.Internal
import io.spine.core.BoundedContextName
import io.spine.core.BoundedContextNames
import io.spine.logging.Level.Companion.DEBUG
import io.spine.logging.toJavaLogging
import io.spine.option.EntityOption
import io.spine.server.bc.given.AnotherProjectAggregate
import io.spine.server.bc.given.FinishedProjectProjection
import io.spine.server.bc.given.ProjectAggregate
import io.spine.server.bc.given.ProjectCreationRepository
import io.spine.server.bc.given.ProjectProcessManager
import io.spine.server.bc.given.ProjectProjection
import io.spine.server.bc.given.ProjectRemovalProcman
import io.spine.server.bc.given.ProjectReport
import io.spine.server.bc.given.SecretProjectRepository
import io.spine.server.bc.given.TestEventSubscriber
import io.spine.server.entity.Entity
import io.spine.server.entity.Repository
import io.spine.system.server.SystemClient
import io.spine.system.server.SystemContext
import io.spine.test.bc.Project
import io.spine.test.bc.SecretProject
import io.spine.testing.TestValues
import io.spine.testing.logging.Interceptor
import io.spine.testing.server.model.ModelTests
import java.util.function.BooleanSupplier
import java.util.logging.Level
import java.util.stream.Stream
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * Tests of [BoundedContext].
 *
 * Messages used in this test suite are defined in:
 *
 *  * `spine/test/bc/project.proto` - data types
 *  * `spine/test/bc/commands.proto` — commands
 *  * `spine/test/bc/events.proto` — events.
 */
@DisplayName("`BoundedContext` should")
internal class BoundedContextSpec {
    private val subscriber = TestEventSubscriber()

    private lateinit var context: BoundedContext

    private var handlersRegistered = false

    @BeforeEach
    fun setUp() {
        ModelTests.dropAllModels()
        context = BoundedContextBuilder.assumingTests(true).build()
    }

    @AfterEach
    fun tearDown() {
        if (handlersRegistered) {
            context.eventBus()
                .unregister(subscriber)
        }
        context.close()
    }

    /** Registers all test repositories, handlers, etc.  */
    private fun registerAll() {
        context.register(DefaultRepository.of(ProjectAggregate::class.java))
        context.eventBus().register(subscriber)
        handlersRegistered = true
    }

    @Nested
    @DisplayName("provide access to")
    @Suppress("TestFunctionName") // For readability of methods named after class names.
    internal inner class Return {

        @Test
        fun EventBus() {
            context.eventBus() shouldNotBe null
        }

        @Test
        fun IntegrationBroker() {
            context.internalAccess().broker() shouldNotBe null
        }

        @Test
        fun CommandDispatcher() {
            context.commandBus() shouldNotBe null
        }

        @Test
        fun `multitenancy state`() {
            BoundedContextBuilder.assumingTests(true).build().use {
                it.isMultitenant shouldBe true
            }
        }
    }

    @Nested
    @DisplayName("register")
    @Suppress("TestFunctionName") // For readability of methods named after class names.
    internal inner class Register {

        @Test
        fun AggregateRepository() =
            registerAndAssertRepository(ProjectAggregate::class.java)

        @Test
        fun ProcessManagerRepository() =
            registerAndAssertRepository(ProjectProcessManager::class.java)

        @Test
        fun ProjectionRepository() =
            registerAndAssertRepository(ProjectReport::class.java)

        /**
         * Registers a [DefaultRepository] for the given entity class and asserts that
         * the context has entities of the given type.
         */
        private fun <I, E : Entity<I, *>> registerAndAssertRepository(cls: Class<E>) {
            context.register(DefaultRepository.of(cls))
            context.hasEntitiesOfType(cls) shouldBe true
        }

        @Test
        fun `a 'DefaultRepository' via passed entity class`() {
            context.register(ProjectAggregate::class.java)
            context.hasEntitiesOfType<ProjectAggregate>() shouldBe true
        }
    }

    @Nested
    @DisplayName("test presence of entities by")
    internal inner class EntityTypePresence {

        @Nested
        @DisplayName("entity state class for")
        internal inner class ByEntityStateClass {

            @Test
            fun `visible entities`() {
                context.register(ProjectAggregate::class.java)
                context.hasEntitiesWithState<Project>() shouldBe true
            }

            @Test
            @DisplayName("invisible entities")
            fun invisible() {
                context.register(SecretProjectRepository())
                context.hasEntitiesWithState<SecretProject>() shouldBe true
            }
        }

        @Nested
        @DisplayName("entity class for")
        internal inner class ByEntityClass {

            @Test
            fun `visible entities`() {
                context.register(ProjectAggregate::class.java)
                context.hasEntitiesOfType<ProjectAggregate>() shouldBe true
            }

            @Test
            fun `invisible entities`() {
                // Process Managers are invisible by default.
                context.register(ProjectProcessManager::class.java)

                context.hasEntitiesOfType<ProjectProcessManager>() shouldBe true
            }
        }
    }

    @Test
    fun `propagate registered repositories to 'Stand'`() {
        val context = BoundedContextBuilder.assumingTests().build()
        val stand = context.stand()

        val repo = DefaultRepository.of(ProjectAggregate::class.java)
        val stateType = repo.entityStateType()

        // No type until registration at the context.
        stand.exposedTypes() shouldNotContain stateType

        context.register(repo)

        // After registration, the type is exposed.
        stand.exposedTypes() shouldContain stateType
    }

    @ParameterizedTest
    @MethodSource("sameStateRepositories")
    fun `not allow two entity repositories with entities of same state`(
        firstRepo: Repository<*, *>, secondRepo: Repository<*, *>
    ) {
        context.register(firstRepo)
        assertThrows<IllegalStateException> {
            context.register(secondRepo)
        }
    }

    @Test
    fun `assign storage during registration if repository does not have one`() {
        val repository = DefaultRepository.of(ProjectAggregate::class.java)
        context.register(repository)
        repository.storageAssigned() shouldBe true
    }

    @Nested
    @DisplayName("assign own multitenancy state to")
    @Suppress("TestFunctionName") // for readability of methods named after class names.
    internal inner class AssignMultitenancyState {

        private lateinit var context: BoundedContext

        @Test
        fun CommandBus() {
            context = multiTenant()
            assertMultitenancyEqual(
                { context.isMultitenant },
                { context.commandBus().isMultitenant })

            context = singleTenant()
            assertMultitenancyEqual(
                { context.isMultitenant },
                { context.commandBus().isMultitenant })
        }

        @Test
        fun Stand() {
            context = multiTenant()

            assertMultitenancyEqual(
                { context.isMultitenant },
                { context.stand().isMultitenant })

            context = singleTenant()

            assertMultitenancyEqual(
                { context.isMultitenant },
                { context.stand().isMultitenant })
        }

        private fun assertMultitenancyEqual(s1: BooleanSupplier, s2: BooleanSupplier) {
            s1.asBoolean shouldBe s2.asBoolean
        }

        private fun multiTenant(): BoundedContext =
            BoundedContextBuilder.assumingTests(true).build()

        private fun singleTenant(): BoundedContext =
            BoundedContextBuilder.assumingTests(false).build()
    }

    /**
     * Simply checks that the result isn't empty to cover the integration with
     * [VisibilityGuard].
     *
     * See [tests of VisibilityGuard][io.spine.server.entity.VisibilityGuardTest]
     * for how visibility filtering works.
     */
    @Test
    fun `obtain entity types by visibility`() {
        context.stateTypes(EntityOption.Visibility.FULL).shouldBeEmpty()
        registerAll()
        context.stateTypes(EntityOption.Visibility.FULL).shouldNotBeEmpty()
    }

    @Test
    fun `throw 'ISE' when obtaining a repository for non-registered entity state class`() {
        // Attempt to get a repository without registering.
        assertThrows<IllegalStateException> {
            context.internalAccess().findRepository(Project::class.java)
        }
    }

    @Test
    fun `not expose invisible aggregates`() {
        ModelTests.dropAllModels()
        context.register(SecretProjectRepository())
        context.internalAccess().findRepository(SecretProject::class.java).shouldBeEmpty()
    }

    @Test
    @DisplayName("prohibit 3rd-party descendants")
    fun `prohibit 3rd-party descendants`() {
        assertThrows<IllegalStateException> {
            object : BoundedContext(BoundedContextBuilder.assumingTests()) {
                @Internal
                override fun systemClient(): SystemClient = TestValues.nullRef()
            }
        }
    }

    @Nested
    @DisplayName("when closing")
    internal inner class ClosingContext {
        private val contextName: BoundedContextName = BoundedContextNames.newName("TestDomain")
        private val systemContextName: BoundedContextName = contextName.toSystem()
        private val debugLevel: Level = DEBUG.toJavaLogging()

        private lateinit var domainInterceptor: Interceptor
        private lateinit var systemInterceptor: Interceptor

        @BeforeEach
        fun closeContext() {
            val context = BoundedContext.singleTenant(contextName.value).build()
            domainInterceptor = Interceptor(DomainContext::class.java, debugLevel)
            domainInterceptor.intercept()
            systemInterceptor = Interceptor(SystemContext::class.java, debugLevel)
            systemInterceptor.intercept()

            context.close()
        }

        @AfterEach
        fun releaseInterceptors() {
            domainInterceptor.release()
            systemInterceptor.release()
        }

        @Test
        fun `log its closing`() {
            domainInterceptor.assertLog().record().run {
                hasLevelThat().isEqualTo(debugLevel)
                hasMessageThat().contains(contextName.value)
            }
        }

        @Test
        fun `close its System context`() {
            systemInterceptor.assertLog().record().run {
                hasLevelThat().isEqualTo(debugLevel)
                hasMessageThat().contains(systemContextName.value)
            }
        }
    }

    @Test
    fun `return its name in 'toString()'`() {
        val name = TestValues.randomString()
        BoundedContext.singleTenant(name).build().use {
            it.toString() shouldBe name
        }
    }

    @Nested
    @DisplayName("do not allow registration calls from outside the `io.spine.server` package for")
    @Suppress("TestFunctionName") // For readability of methods named after class names.
    internal inner class RestrictRegistrationCalls {

        @Test
        fun Repository() = assertThrowsOn {
            ForeignContextConfig.repositoryRegistration()
        }

        @Test
        fun CommandDispatcher() = assertThrowsOn {
            ForeignContextConfig.commandDispatcherRegistration()
        }

        @Test
        fun EventDispatcher() = assertThrowsOn {
            ForeignContextConfig.eventDispatcherRegistration()
        }

        private fun assertThrowsOn(executable: Executable) {
            Assertions.assertThrows(SecurityException::class.java, executable)
        }
    }

    @Test
    fun `be equal to another context by its name`() {
        val c1 = BoundedContext.singleTenant("One").build()
        val c2 = BoundedContext.singleTenant("Two").build()
        val c1m = BoundedContext.multitenant("One").build()
        val c2m = BoundedContext.multitenant("Two").build()

        EqualsTester()
            .addEqualityGroup(c1, c1m)
            .addEqualityGroup(c2, c2m)
            .testEquals()
    }

    @Test
    fun `be comparable by its name`() {
        val c1 = BoundedContext.singleTenant("1").build()
        val c2 = BoundedContext.singleTenant("2").build()

        assertThat(c1).isLessThan(c2)
        assertThat(c2).isGreaterThan(c1)
        assertThat(c1).isEqualTo(BoundedContext.multitenant("1").build())
    }

    companion object {

        /**
         * Returns all combinations of repositories that manage entities of the same state.
         *
         *
         * To check whether a [io.spine.server.BoundedContext] really throws
         * an `IllegalStateException` upon an attempt to register a repository that manages an
         * entity of a state that a registered entity repository is already managing,
         * all combinations of entities that take state as a type parameter need to be checked.
         *
         * This method returns a stream of pairs of all such combinations, which is a Cartesian
         * product of:
         *
         *  * [Process Manager][io.spine.server.procman.ProcessManagerRepository];
         *  * [Aggregate][io.spine.server.aggregate.AggregateRepository];
         *  * [Projection][io.spine.server.projection.ProjectionRepository].
         *
         * All the returned repositories manage entities of the same state type.
         */
        @Suppress("unused") /* A method source. */
        @JvmStatic
        fun sameStateRepositories(): Stream<Arguments> {
            val repositories: Set<Repository<*, *>> =
                ImmutableSet.of<Repository<*, *>>(
                    DefaultRepository.of(ProjectAggregate::class.java),
                    DefaultRepository.of(ProjectProjection::class.java),
                    ProjectCreationRepository()
                )

            val sameStateRepositories: Set<Repository<*, *>> =
                ImmutableSet.of<Repository<*, *>>(
                    DefaultRepository.of(AnotherProjectAggregate::class.java),
                    DefaultRepository.of(FinishedProjectProjection::class.java),
                    DefaultRepository.of(ProjectRemovalProcman::class.java)
                )

            val cartesianProduct = Sets.cartesianProduct(repositories, sameStateRepositories)
            val result = cartesianProduct.stream()
                .map { repos: List<Repository<*, *>> ->
                    Arguments.of(repos[0], repos[1])
                }
            return result
        }
    }
}
