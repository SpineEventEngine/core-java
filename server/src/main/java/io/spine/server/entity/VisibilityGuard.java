/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.option.EntityOption.Visibility;
import io.spine.option.EntityOptions;
import io.spine.type.TypeName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Maps.newHashMap;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A registry of repositories that controls access to them depending on the visibility of
 * corresponding entity states.
 *
 * @author Alexander Yevsyukov
 * @see EntityOptions
 */
@Internal
public final class VisibilityGuard {

    private final Map<Class<? extends Message>, RepositoryAccess> repositories = newHashMap();

    private VisibilityGuard() {
        // Prevent instantiation from outside.
    }

    /**
     * Creates a new instance of the guard.
     */
    public static VisibilityGuard newInstance() {
        return new VisibilityGuard();
    }

    /**
     * Registers the passed repository with the guard.
     */
    public void register(Repository<?, ?> repository) {
        checkNotNull(repository);
        EntityClass<?> entityClass = repository.entityClass();
        Class<? extends Message> stateClass = entityClass.getStateClass();
        checkNotAlreadyRegistered(stateClass);
        repositories.put(stateClass, new RepositoryAccess(repository));
    }

    private void checkNotAlreadyRegistered(Class<? extends Message> stateClass) {
        RepositoryAccess alreadyRegistered = repositories.get(stateClass);
        if (alreadyRegistered != null) {
            throw newIllegalStateException(
                    "A repository for the state class %s already registered: %s",
                    stateClass, alreadyRegistered);
        }
    }

    /**
     * Verifies if there is a registered repository for the passed entity state class.
     */
    public boolean hasRepository(Class<? extends Message> stateClass) {
        checkNotNull(stateClass);
        boolean result = repositories.containsKey(stateClass);
        return result;
    }

    /**
     * Obtains the repository for the passed entity state class.
     *
     * @param stateClass the class of the state of entities managed by the repository
     * @return the repository wrapped into {@code Optional} or {@code Optional#empty()} if the
     * entity state is {@linkplain Visibility#NONE not visible}
     * @throws IllegalArgumentException if the repository for the passed state class was not
     *                                  {@linkplain #register(Repository) registered} with the guard
     *                                  prior to this call, or if all repositories were
     *                                  {@linkplain #shutDownRepositories() shut down}
     */
    public Optional<Repository> getRepository(Class<? extends Message> stateClass) {
        checkNotNull(stateClass);
        RepositoryAccess repositoryAccess = repositories.get(stateClass);
        if (repositoryAccess == null) {
            throw newIllegalArgumentException(
                    "A repository for the state class (%s) was not registered in VisibilityGuard",
                    stateClass.getName());
        }
        return repositoryAccess.get();
    }

    /**
     * Obtains a set of entity type names by their visibility.
     */
    public Set<TypeName> getEntityTypes(Visibility visibility) {
        checkNotNull(visibility);

        // Filter repositories of entities with this visibility.
        Collection<RepositoryAccess> repos =
                filterValues(repositories,
                             new Predicate<RepositoryAccess>() {
                                 @Override
                                 public boolean apply(@Nullable RepositoryAccess input) {
                                     checkNotNull(input);
                                     return input.visibility == visibility;
                                 }
                             }).values();

        // Get type names for entities of the filtered repositories.
        Iterable<TypeName> entityTypes =
                transform(repos,
                          new Function<RepositoryAccess, TypeName>() {
                              @Override
                              public TypeName apply(@Nullable RepositoryAccess input) {
                                  checkNotNull(input);
                                  @SuppressWarnings("unchecked")
                                  // Safe as it's bounded by Repository class definition.
                                  Class<? extends Message> cls =
                                          input.repository.entityClass()
                                                          .getStateClass();
                                  TypeName result = TypeName.of(cls);
                                  return result;
                              }
                          });
        return Sets.newHashSet(entityTypes);
    }

    /**
     * Closes all registered repositories and clears the registration list.
     */
    public void shutDownRepositories() {
        for (RepositoryAccess repositoryAccess : repositories.values()) {
            repositoryAccess.repository.close();
        }
        repositories.clear();
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
            Class<? extends Message> stateClass = repository.entityClass()
                                                                  .getStateClass();
            this.visibility = EntityOptions.getVisibility(stateClass);
        }

        private Optional<Repository> get() {
            return (visibility == Visibility.NONE)
                   ? Optional.empty()
                   : Optional.of(repository);
        }
    }
}
