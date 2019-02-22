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

package io.spine.model.verify;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.code.proto.EntityLifecycleOption;
import io.spine.code.proto.EntityStateOption;
import io.spine.code.proto.MessageType;
import io.spine.code.proto.ref.TypeRef;
import io.spine.option.EntityOption;
import io.spine.option.EntityOption.Kind;
import io.spine.server.model.EntityKindMismatchError;
import io.spine.server.model.TypeMismatchError;
import io.spine.type.KnownTypes;

import java.util.Optional;
import java.util.function.Predicate;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.spine.option.EntityOption.Kind.KIND_UNKNOWN;
import static io.spine.option.EntityOption.Kind.PROCESS_MANAGER;

/**
 * The checker of lifecycle {@linkplain io.spine.option.LifecycleOption declarations} of the Spine
 * Model entities.
 */
final class EntitiesLifecycle {

    /**
     * A list of entity types that have lifecycle-impacting events and rejections.
     */
    private final ImmutableSet<MessageType> entitiesWithLifecycle;

    @VisibleForTesting
    EntitiesLifecycle(ImmutableSet<MessageType> entityTypes) {
        this.entitiesWithLifecycle = entityTypes;
    }

    /**
     * Collects all entities with lifecycle options from {@link KnownTypes}.
     */
    static EntitiesLifecycle ofKnownTypes() {
        ImmutableSet<MessageType> entityTypes =
                KnownTypes.instance()
                          .asTypeSet()
                          .messageTypes()
                          .stream()
                          .filter(EntitiesLifecycle::hasLifecycle)
                          .collect(toImmutableSet());
        return new EntitiesLifecycle(entityTypes);
    }

    private static boolean hasLifecycle(MessageType type) {
        EntityLifecycleOption option = new EntityLifecycleOption();
        return option.hasLifecycle(type);
    }

    /**
     * Checks entity lifecycle declarations correctness.
     *
     * <p>Checks that:
     * <ol>
     *     <li>The lifecycle target is an entity of a process manager
     *         {@linkplain Kind#PROCESS_MANAGER kind}.
     *     <li>All types stated as lifecycle triggers are valid {@linkplain EventMessage events} or
     *         {@linkplain RejectionMessage rejections}.
     * </ol>
     *
     * @throws EntityKindMismatchError
     *         if the lifecycle target is not a process manager
     * @throws io.spine.type.UnresolvedReferenceException
     *         if one or more lifecycle trigger types cannot be resolved
     * @throws TypeMismatchError
     *         if one or more lifecycle trigger types are not valid events or entities
     */
    void checkLifecycleDeclarations() {
        checkTargets();
        checkLifecycleTriggers();
    }

    private void checkTargets() {
        entitiesWithLifecycle.forEach(EntitiesLifecycle::checkIsProcessManager);
    }

    private static void checkIsProcessManager(MessageType type) {
        Optional<Kind> entityKind = EntityStateOption.valueOf(type.descriptor())
                                                     .map(EntityOption::getKind);
        Kind actual = entityKind.orElse(KIND_UNKNOWN);
        if (actual != PROCESS_MANAGER) {
            throw new EntityKindMismatchError(actual, PROCESS_MANAGER);
        }
    }

    private void checkLifecycleTriggers() {
        entitiesWithLifecycle.forEach(EntitiesLifecycle::checkLifecycleTriggers);
    }

    private static void checkLifecycleTriggers(MessageType messageType) {
        EntityLifecycleOption option = new EntityLifecycleOption();
        Optional<TypeRef> archiveUpon = option.archiveUpon(messageType);
        archiveUpon.ifPresent(EntitiesLifecycle::checkLifecycleTrigger);

        Optional<TypeRef> deleteUpon = option.deleteUpon(messageType);
        deleteUpon.ifPresent(EntitiesLifecycle::checkLifecycleTrigger);
    }

    private static void checkLifecycleTrigger(TypeRef typeRef) {
        ImmutableSet<MessageType> referenced =
                KnownTypes.instance()
                          .resolveAndValidate(typeRef);
        Predicate<MessageType> isEvent = MessageType::isEvent;
        Predicate<MessageType> predicate = isEvent.or(MessageType::isRejection);
        referenced.forEach(
                type -> {
                    if (!predicate.test(type)) {
                        throw new TypeMismatchError(type,
                                                    EventMessage.class, RejectionMessage.class);
                    }
                }
        );
    }
}
