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

import com.google.common.collect.ImmutableSet;
import io.spine.code.proto.EntityLifecycleOption;
import io.spine.code.proto.EntityStateOption;
import io.spine.code.proto.MessageType;
import io.spine.code.proto.ref.TypeRef;
import io.spine.option.EntityOption;
import io.spine.server.model.InsufficientMessageTypeError;
import io.spine.type.KnownTypes;

import java.util.Optional;
import java.util.function.Predicate;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.spine.option.EntityOption.Kind.PROCESS_MANAGER;

final class EntitiesLifecycle {

    private final ImmutableSet<MessageType> entitiesWithLifecycle;

    private EntitiesLifecycle(ImmutableSet<MessageType> entityTypes) {
        this.entitiesWithLifecycle = entityTypes;
    }

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

    void verify() {
        verifyMessageTypes();
        verifyTriggerTypes();
    }

    /**
     * Verifies that lifecycle option is specified only for entities of process manager
     * {@linkplain io.spine.option.EntityOption.Kind#PROCESS_MANAGER kind}.
     */
    private void verifyMessageTypes() {
        entitiesWithLifecycle.forEach(EntitiesLifecycle::checkIsProcessManager);
    }

    private static void checkIsProcessManager(MessageType type) {
        Optional<EntityOption> optionValue = EntityStateOption.valueOf(type.descriptor());
        boolean isProcessManager =
                optionValue.map(EntityOption::getKind)
                           .filter(kind -> kind == PROCESS_MANAGER)
                           .isPresent();
        if (!isProcessManager) {
            throw new InsufficientMessageTypeError(
                    "Only entities of process manager kind can have `lifecycle` option", type
            );
        }
    }

    /**
     * Verifies that all specified entity lifecycle triggers are valid event types.
     */
    private void verifyTriggerTypes() {
        entitiesWithLifecycle.forEach(EntitiesLifecycle::checkLifecycleTriggers);
    }

    private static void checkLifecycleTriggers(MessageType messageType) {
        EntityLifecycleOption option = new EntityLifecycleOption();
        Optional<TypeRef> archiveUpon = option.archiveUpon(messageType);
        archiveUpon.ifPresent(EntitiesLifecycle::checkLifecycleTrigger);
    }

    private static void checkLifecycleTrigger(TypeRef typeRef) {
        Predicate<MessageType> isEvent = MessageType::isEvent;
        Predicate<MessageType> predicate = isEvent.or(MessageType::isRejection);
        MessageTypeValidator typeValidator =
                new MessageTypeValidator(predicate,
                                         "Only event or rejection types can be referenced in " +
                                                 "the `lifecycle` option");
        TypeRefValidator validator = TypeRefValidator.withTypeValidator(typeRef, typeValidator);
        validator.validate();
    }
}
