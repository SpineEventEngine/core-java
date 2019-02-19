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

import com.google.common.collect.ImmutableMap;
import io.spine.code.proto.Lifecycle;
import io.spine.code.proto.MessageType;
import io.spine.code.proto.ref.TypeRef;
import io.spine.option.LifecycleOption;
import io.spine.type.KnownTypes;

import static io.spine.code.proto.Lifecycle.lifecycleOf;

final class EntityLifecycle {

    private final ImmutableMap<MessageType, LifecycleOption> entityLifecycle;

    private EntityLifecycle(ImmutableMap<MessageType, LifecycleOption> entityLifecycle) {
        this.entityLifecycle = entityLifecycle;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // Checked logically.
    static EntityLifecycle ofKnownTypes() {
        ImmutableMap.Builder<MessageType, LifecycleOption> map = new ImmutableMap.Builder<>();
        KnownTypes.instance()
                  .asTypeSet()
                  .messageTypes()
                  .stream()
                  .filter(Lifecycle::hasOption)
                  .forEach(type -> map.put(type, lifecycleOf(type).get()));
        return new EntityLifecycle(map.build());
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

    }

    /**
     * Verifies that all specified entity lifecycle triggers are valid Protobuf types.
     */
    private void verifyTriggerTypes() {
        entityLifecycle.values()
                       .forEach(this::checkLifecycleTriggers);
    }

    private void checkLifecycleTriggers(LifecycleOption lifecycleOption) {
        String archiveUpon = lifecycleOption.getArchiveUpon();
        TypeRef parse = TypeRef.parse(archiveUpon);
    }
}
