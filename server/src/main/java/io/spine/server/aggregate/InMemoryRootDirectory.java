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

package io.spine.server.aggregate;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.type.TypeUrl;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Multimaps.synchronizedSetMultimap;

/**
 * An in-mem implementation of {@link AggregateRootDirectory}.
 */
@Internal
public final class InMemoryRootDirectory implements AggregateRootDirectory {

    private final SetMultimap<Class<? extends AggregateRoot<?>>, AggregatePartRepository<?, ?, ?>>
    repositories = synchronizedSetMultimap(HashMultimap.create());

    @Override
    public void register(AggregatePartRepository<?, ?, ?> repository) {
        checkNotNull(repository);

        Class<? extends AggregateRoot<?>> rootClass = repository.aggregatePartClass().rootClass();
        repositories.put(rootClass, repository);
    }

    @Override
    public Optional<? extends AggregatePartRepository<?, ?, ?>>
    findPart(Class<? extends AggregateRoot<?>> rootClass, Class<? extends Message> partStateClass) {
        Set<AggregatePartRepository<?, ?, ?>> parts = repositories.get(rootClass);
        if (parts.isEmpty()) {
            return Optional.empty();
        } else {
            TypeUrl targetType = TypeUrl.of(partStateClass);
            Optional<AggregatePartRepository<?, ?, ?>> repository = parts
                    .stream()
                    .filter(repo -> repo.entityStateType().equals(targetType))
                    .findAny();
            return repository;
        }
    }
}
