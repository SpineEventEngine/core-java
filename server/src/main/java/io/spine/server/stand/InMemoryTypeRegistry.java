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
package io.spine.server.stand;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.Repository;
import io.spine.server.entity.VersionableEntity;
import io.spine.type.TypeUrl;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Optional.ofNullable;

/**
 * The in-memory concurrency-friendly implementation of {
 * @linkplain TypeRegistry Stand type registry}.
 *
 * @author Alex Tymchenko
 */
class InMemoryTypeRegistry implements TypeRegistry {

    /**
     * The mapping between {@code TypeUrl} instances and repositories providing
     * the entities of this type.
     */
    private final ConcurrentMap<TypeUrl, RecordBasedRepository<?, ?, ?>> typeToRepositoryMap =
            new ConcurrentHashMap<>();

    /**
     * Stores  known {@code Aggregate} types in order to distinguish
     * them among all instances of {@code TypeUrl}.
     */
    private final Set<TypeUrl> knownAggregateTypes = Sets.newConcurrentHashSet();

    private InMemoryTypeRegistry() {
        // Prevent instantiation from the outside.
    }

    static TypeRegistry newInstance() {
        return new InMemoryTypeRegistry();
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    @Override
    public <I, E extends VersionableEntity<I, ?>> void register(Repository<I, E> repository) {
        TypeUrl entityType = repository.getEntityStateType();

        if (repository instanceof RecordBasedRepository) {
            typeToRepositoryMap.put(entityType,
                                    (RecordBasedRepository<I, E, ? extends Message>) repository);
        }
        if (repository instanceof AggregateRepository) {
            knownAggregateTypes.add(entityType);
        }
    }


    @Override
    public Optional<? extends RecordBasedRepository<?, ?, ?>> getRecordRepository(TypeUrl type) {
        RecordBasedRepository<?, ?, ? > repo = typeToRepositoryMap.get(type);
        Optional<? extends RecordBasedRepository<?, ?, ?>> result = ofNullable(repo);
        return result;
    }

    @Override
    public ImmutableSet<TypeUrl> getTypes() {
        ImmutableSet.Builder<TypeUrl> resultBuilder = ImmutableSet.builder();
        Set<TypeUrl> projectionTypes = typeToRepositoryMap.keySet();
        resultBuilder.addAll(projectionTypes)
                     .addAll(knownAggregateTypes);
        ImmutableSet<TypeUrl> result = resultBuilder.build();
        return result;
    }

    @Override
    public ImmutableSet<TypeUrl> getAggregateTypes() {
        ImmutableSet<TypeUrl> result = ImmutableSet.copyOf(knownAggregateTypes);
        return result;
    }

    @Override
    public boolean hasAggregateType(TypeUrl typeUrl) {
        boolean result = knownAggregateTypes.contains(typeUrl);
        return result;
    }

    @Override
    public void close() throws Exception {
        typeToRepositoryMap.clear();
        knownAggregateTypes.clear();
    }
}
