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

package io.spine.system.server;

import io.spine.annotation.Internal;
import io.spine.client.EntityId;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("OverlyCoupledClass") // OK for an Aggregate class.
@Internal
public final class EntityHistoryAggregate
        extends Aggregate<EntityId, EntityHistory, EntityHistoryVBuilder> {

    private EntityHistoryAggregate(EntityId id) {
        super(id);
    }

    @Assign
    EntityCreated handle(CreateEntity command) {
        return EntityCreated.newBuilder()
                            .setId(command.getId())
                            .setKind(command.getKind())
                            .build();
    }

    @Assign
    EventDispatchedToSubscriber handle(DispatchEventToSubscriber command) {
        return EventDispatchedToSubscriber.newBuilder()
                                          .setReceiver(command.getReceiver())
                                          .setPayload(command.getPayload())
                                          .build();
    }

    @Assign
    EventDispatchedToReactor handle(DispatchEventToReactor command) {
        return EventDispatchedToReactor.newBuilder()
                                       .setReceiver(command.getReceiver())
                                       .setPayload(command.getPayload())
                                       .build();
    }

    @Assign
    EventDispatchedToApplier handle(DispatchEventToApplier command) {
        return EventDispatchedToApplier.newBuilder()
                                       .setReceiver(command.getReceiver())
                                       .setPayload(command.getPayload())
                                       .build();
    }

    @Assign
    CommandDispatchedToHandler handle(DispatchCommandToHandler command) {
        return CommandDispatchedToHandler.newBuilder()
                                         .setReceiver(command.getReceiver())
                                         .setPayload(command.getPayload())
                                         .build();
    }

    @Assign
    EntityStateChanged handle(ChangeEntityState command) {
        return EntityStateChanged.newBuilder()
                                 .setId(command.getId())
                                 .setMessageId(command.getMessageId())
                                 .setNewState(command.getNewState())
                                 .build();
    }

    @Assign
    EntityArchived handle(ArchiveEntity command) {
        return EntityArchived.newBuilder()
                             .setId(command.getId())
                             .setMessageId(command.getMessageId())
                             .build();
    }

    @Assign
    EntityDeleted handle(DeleteEntity command) {
        return EntityDeleted.newBuilder()
                            .setId(command.getId())
                            .setMessageId(command.getMessageId())
                            .build();
    }

    @Assign
    EntityExtractedFromArchive handle(ExtractEntityFromArchive command) {
        return EntityExtractedFromArchive.newBuilder()
                                         .setId(command.getId())
                                         .setMessageId(command.getMessageId())
                                         .build();
    }

    @Assign
    EntityRestored handle(RestoreEntity command) {
        return EntityRestored.newBuilder()
                             .setId(command.getId())
                             .setMessageId(command.getMessageId())
                             .build();
    }

    @Apply
    private void on(EntityCreated event) {
        getBuilder().setId(event.getId());
    }

    @Apply
    private void on(EventDispatchedToSubscriber event) {
        getBuilder().addEvents(event.getPayload());
    }

    @Apply
    private void on(EventDispatchedToReactor event) {
        getBuilder().addEvents(event.getPayload());
    }

    @Apply
    private void on(EventDispatchedToApplier event) {
        getBuilder().addEvents(event.getPayload());
    }

    @Apply
    private void on(CommandDispatchedToHandler event) {
        getBuilder().addCommands(event.getPayload());
    }

    @Apply
    private void on(EntityStateChanged event) {
        // NOP.
    }

    @Apply
    private void on(EntityArchived event) {
        // NOP.
    }

    @Apply
    private void on(EntityDeleted event) {
        // NOP.
    }

    @Apply
    private void on(EntityExtractedFromArchive event) {
        // NOP.
    }

    @Apply
    private void on(EntityRestored event) {
        // NOP.
    }
}
