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

package org.spine3.server.entity;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import org.spine3.annotation.Internal;
import org.spine3.option.EntityOption.Visibility;
import org.spine3.option.EntityOptions;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Maps.newHashMap;
import static org.spine3.util.Exceptions.newIllegalArgumentException;
import static org.spine3.util.Exceptions.newIllegalStateException;

/**
 * A registry of repositories that controls access to them depending on the visibility of
 * corresponding entity states.
 *
 * @author Alexander Yevsyukov
 * @see EntityOptions
 */
@Internal
public final class VisibilityGuard {

    private final Map<Class, RepositoryAccess> repositories = newHashMap();

    private VisibilityGuard() {
        // Prevent instantiation from outside.
    }

    public static VisibilityGuard newInstance() {
        return new VisibilityGuard();
    }

    public void register(Repository repository) {
        checkNotNull(repository);
        final Class stateClass = repository.getEntityStateClass();
        checkNotAlreadyRegistered(stateClass);
        repositories.put(stateClass, new RepositoryAccess(repository));
    }

    private void checkNotAlreadyRegistered(Class stateClass) {
        final RepositoryAccess alreadyRegistered = repositories.get(stateClass);
        if (alreadyRegistered != null) {
            throw newIllegalStateException(
                    "A repository for the state class %s already registered: %s",
                    stateClass, alreadyRegistered);
        }
    }

    public boolean hasRepository(Class<? extends Message> stateClass) {
        final boolean result = repositories.containsKey(stateClass);
        return result;
    }

    public Optional<Repository> getRepository(Class<? extends Message> stateClass) {
        final RepositoryAccess repositoryAccess = repositories.get(stateClass);
        if (repositoryAccess == null) {
            throw newIllegalArgumentException(
                    "A repository for the state class (%s) was not registered in VisibilityGuard",
                    stateClass.getName());
        }
        return repositoryAccess.get();
    }

    public Set<TypeName> getEntityTypes(final Visibility visibility) {
        checkNotNull(visibility);

        // Filter repositories of entities with this visibility.
        final Collection<RepositoryAccess> repos =
                filterValues(repositories,
                             new Predicate<RepositoryAccess>() {
                                 @Override
                                 public boolean apply(@Nullable RepositoryAccess input) {
                                     checkNotNull(input);
                                     return input.visibility == visibility;
                                 }
                             }).values();

        // Get type names for entities of the filtered repositories.
        final Iterable<TypeName> entityTypes =
                transform(repos,
                          new Function<RepositoryAccess, TypeName>() {
                              @Override
                              public TypeName apply(@Nullable RepositoryAccess input) {
                                  checkNotNull(input);
                                  @SuppressWarnings("unchecked")
                                        // Safe as it's bounded by Repository class definition.
                                  final Class<? extends Message> cls =
                                          input.repository.getEntityStateClass();
                                  final TypeName result = TypeName.of(cls);
                                  return result;
                              }
                          });
        return Sets.newHashSet(entityTypes);
    }

    /**
     * Allows to get a reference to repository if states of its entities are visible.
     */
    private static class RepositoryAccess {

        private final Repository repository;
        private final Visibility visibility;

        private RepositoryAccess(Repository repository) {
            this.repository = repository;
            @SuppressWarnings("unchecked") // Safe as it's bounded by Repository class definition.
            final Class<? extends Message> stateClass = repository.getEntityStateClass();
            this.visibility = EntityOptions.getVisibility(stateClass);
        }

        private Optional<Repository> get() {
            return (visibility == Visibility.NONE)
                    ? Optional.<Repository>absent()
                    : Optional.of(repository);
        }
    }
}
