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

package io.spine.server.query

import io.grpc.stub.StreamObserver
import io.spine.base.EntityState
import io.spine.base.Identifier
import io.spine.util.theOnly
import io.spine.client.ActorRequestFactory
import io.spine.client.Query
import io.spine.client.QueryResponse
import io.spine.core.UserId
import io.spine.protobuf.AnyPacker
import io.spine.server.BoundedContext
import java.util.*

/**
 * A builder for queries to the states of entities.
 *
 * @param T the type of entity states to query.
 */
public class QueryingClient<T : EntityState<*>>
internal constructor(
    private val context: BoundedContext,
    private val type: Class<T>,
    actorName: String
) {

    private val actor = UserId.newBuilder()
        .setValue(actorName)
        .build()
    private val factory = ActorRequestFactory.newBuilder()
        .setActor(actor)
        .build()

    /**
     * Obtains a state of an entity by its ID.
     *
     * The value of the ID must be one of the [supported types][io.spine.base.Identifier].
     *
     * @return the state of the entity or empty `Optional` if the entity with the given ID
     *         was not found
     * @throws IllegalArgumentException
     *          if the given ID is not of one of the supported types
     */
    public fun withId(id: Any): Optional<T> {
        Identifier.checkSupported(id.javaClass)
        val query = buildQuery(id)
        val results = execute(query)
        return if (results.isEmpty()) {
            Optional.empty()
        } else {
            val value = results.theOnly()
            Optional.of(value)
        }
    }

    /**
     * Selects all entities of the given type.
     */
    public fun all(): Set<T> {
        val query = buildQuery()
        return execute(query)
    }

    private fun buildQuery(id: Any? = null): Query {
        val queries = factory.query()
        return if (id == null) {
            queries.all(type)
        } else {
            queries.byIds(type, setOf(id))
        }
    }

    /**
     * Executes the given [query] upon the given [context].
     */
    private fun execute(query: Query): Set<T> {
        val observer = Observer(type)
        context.stand().execute(query, observer)
        return observer.foundResult().toSet()
    }
}

/**
 * A [StreamObserver] which listens to a single [QueryResponse].
 *
 * The observer persists the [found result][foundResult] as a list of messages.
 */
private class Observer<T : EntityState<*>>(
    private val type: Class<T>
) : StreamObserver<QueryResponse> {

    private var result: List<T>? = null

    override fun onNext(response: QueryResponse) {
        result = response.messageList.map {
            AnyPacker.unpack(it.state, type)
        }
    }

    override fun onError(e: Throwable) {
        throw e
    }

    override fun onCompleted() {}

    /**
     * Obtains the found result or throws an `IllegalStateException` if
     * the result has not been received.
     */
    fun foundResult(): List<T> {
        return result ?: throw IllegalStateException("Query has not yielded any result yet.")
    }
}
