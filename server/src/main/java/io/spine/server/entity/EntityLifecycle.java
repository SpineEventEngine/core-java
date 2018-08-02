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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.client.EntityId;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.option.EntityOption;
import io.spine.system.server.ArchiveEntity;
import io.spine.system.server.AssignTargetToCommand;
import io.spine.system.server.ChangeEntityState;
import io.spine.system.server.CommandTarget;
import io.spine.system.server.CreateEntity;
import io.spine.system.server.DeleteEntity;
import io.spine.system.server.DispatchCommandToHandler;
import io.spine.system.server.DispatchEventToReactor;
import io.spine.system.server.DispatchEventToSubscriber;
import io.spine.system.server.DispatchedMessageId;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.ExtractEntityFromArchive;
import io.spine.system.server.MarkCommandAsHandled;
import io.spine.system.server.RestoreEntity;
import io.spine.system.server.SystemGateway;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Identifier.pack;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.util.stream.Collectors.toList;

/**
 * The lifecycle callbacks of an {@link Entity}.
 *
 * <p>On each call, posts from zero to few system commands. See the individual method descriptions
 * for more info about the posted commands.
 *
 * <p>An instance of {@code EntityLifecycle} is associated with a single instance of entity.
 *
 * @see Repository#lifecycleOf(Object) Repository.lifecycleOf(I)
 */
@SuppressWarnings("OverlyCoupledClass") // Posts system commands.
public class EntityLifecycle {

    private final SystemGateway systemGateway;
    private final EntityHistoryId id;

    /**
     * Creates a new instance.
     *
     * <p>Use this constructor for test purposes <b>only</b>.
     *
     * @see #create(SystemGateway, Object, TypeUrl) EntityLifecycle.create(...) to instantiate
     *                                              the class
     */
    @VisibleForTesting
    protected EntityLifecycle(SystemGateway gateway, Object id, TypeUrl entityType) {
        this.systemGateway = gateway;
        this.id = historyId(id, entityType);
    }

    /**
     * Creates a new instance of {@code EntityLifecycle}.
     *
     * @param gateway    the {@link SystemGateway} to post the system commands
     * @param id         the ID of the associated entity
     * @param entityType the type of the associated entity
     * @return new instance of {@code EntityLifecycle}
     */
    static EntityLifecycle create(SystemGateway gateway, Object id, TypeUrl entityType) {
        return new EntityLifecycle(gateway, id, entityType);
    }

    /**
     * Posts the {@link CreateEntity} system command.
     *
     * @param entityKind the {@link EntityOption.Kind} of the created entity
     */
    public void onEntityCreated(EntityOption.Kind entityKind) {
        CreateEntity command = CreateEntity
                .newBuilder()
                .setId(id)
                .setKind(entityKind)
                .build();
        systemGateway.postCommand(command);
    }

    /**
     * Posts the {@link io.spine.system.server.AssignTargetToCommand AssignTargetToCommand}
     * system command.
     *
     * @param commandId the ID of the command which should be handled by the entity
     */
    public void onTargetAssignedToCommand(CommandId commandId) {
        CommandTarget target = CommandTarget
                .newBuilder()
                .setEntityId(id.getEntityId())
                .setTypeUrl(id.getTypeUrl())
                .build();
        AssignTargetToCommand command = AssignTargetToCommand
                .newBuilder()
                .setId(commandId)
                .setTarget(target)
                .build();
        systemGateway.postCommand(command);
    }

    /**
     * Posts the {@link DispatchCommandToHandler} system command.
     *
     * @param command the dispatched command
     */
    public void onDispatchCommand(Command command) {
        DispatchCommandToHandler systemCommand = DispatchCommandToHandler
                .newBuilder()
                .setReceiver(id)
                .setCommandId(command.getId())
                .build();
        systemGateway.postCommand(systemCommand);
    }

    /**
     * Posts the {@link MarkCommandAsHandled} system command.
     *
     * @param command the handled command
     */
    public void onCommandHandled(Command command) {
        MarkCommandAsHandled systemCommand = MarkCommandAsHandled
                .newBuilder()
                .setId(command.getId())
                .build();
        systemGateway.postCommand(systemCommand);
    }

    /**
     * Posts the {@link DispatchEventToSubscriber} system command.
     *
     * @param event the dispatched event
     */
    public void onDispatchEventToSubscriber(Event event) {
        DispatchEventToSubscriber systemCommand = DispatchEventToSubscriber
                .newBuilder()
                .setReceiver(id)
                .setEventId(event.getId())
                .build();
        systemGateway.postCommand(systemCommand);
    }

    /**
     * Posts the {@link DispatchEventToReactor} system command.
     *
     * @param event the dispatched event
     */
    public void onDispatchEventToReactor(Event event) {
        DispatchEventToReactor systemCommand = DispatchEventToReactor
                .newBuilder()
                .setReceiver(id)
                .setEventId(event.getId())
                .build();
        systemGateway.postCommand(systemCommand);
    }

    /**
     * Posts the {@link ChangeEntityState} system command and the commands related to
     * the lifecycle flags.
     *
     * <p>Only the actual changes in the entity attributes result into system commands.
     * If the previous and new values are equal, then no commands are posted.
     *
     * @param change     the change in the entity state and attributes
     * @param messageIds the IDs of the messages which caused the {@code change}; typically,
     *                   {@link io.spine.core.EventId EventId}s or {@link CommandId}s
     */
    protected void onStateChanged(EntityRecordChange change,
                        Set<? extends Message> messageIds) {
        Collection<DispatchedMessageId> dispatchedMessageIds = toDispatched(messageIds);

        postIfChanged(change, dispatchedMessageIds);
        postIfArchived(change, dispatchedMessageIds);
        postIfDeleted(change, dispatchedMessageIds);
        postIfExtracted(change, dispatchedMessageIds);
        postIfRestored(change, dispatchedMessageIds);
    }

    private void postIfChanged(EntityRecordChange change,
                               Collection<DispatchedMessageId> messageIds) {
        Any oldState = change.getPreviousValue()
                             .getState();
        Any newState = change.getNewValue()
                             .getState();

        if (!oldState.equals(newState)) {
            ChangeEntityState command = ChangeEntityState
                    .newBuilder()
                    .setId(id)
                    .setNewState(newState)
                    .addAllMessageId(messageIds)
                    .build();
            systemGateway.postCommand(command);
        }
    }

    private void postIfArchived(EntityRecordChange change,
                                Collection<DispatchedMessageId> messageIds) {
        boolean oldValue = change.getPreviousValue()
                                 .getLifecycleFlags()
                                 .getArchived();
        boolean newValue = change.getNewValue()
                                 .getLifecycleFlags()
                                 .getArchived();
        if (newValue && !oldValue) {
            ArchiveEntity command = ArchiveEntity
                    .newBuilder()
                    .setId(id)
                    .addAllMessageId(messageIds)
                    .build();
            systemGateway.postCommand(command);
        }
    }

    private void postIfDeleted(EntityRecordChange change,
                               Collection<DispatchedMessageId> messageIds) {
        boolean oldValue = change.getPreviousValue()
                                 .getLifecycleFlags()
                                 .getDeleted();
        boolean newValue = change.getNewValue()
                                 .getLifecycleFlags()
                                 .getDeleted();
        if (newValue && !oldValue) {
            DeleteEntity command = DeleteEntity
                    .newBuilder()
                    .setId(id)
                    .addAllMessageId(messageIds)
                    .build();
            systemGateway.postCommand(command);
        }
    }

    private void postIfExtracted(EntityRecordChange change,
                                 Collection<DispatchedMessageId> messageIds) {
        boolean oldValue = change.getPreviousValue()
                                 .getLifecycleFlags()
                                 .getArchived();
        boolean newValue = change.getNewValue()
                                 .getLifecycleFlags()
                                 .getArchived();
        if (!newValue && oldValue) {
            ExtractEntityFromArchive command = ExtractEntityFromArchive
                    .newBuilder()
                    .setId(id)
                    .addAllMessageId(messageIds)
                    .build();
            systemGateway.postCommand(command);
        }
    }

    private void postIfRestored(EntityRecordChange change,
                                Collection<DispatchedMessageId> messageIds) {
        boolean oldValue = change.getPreviousValue()
                                 .getLifecycleFlags()
                                 .getDeleted();
        boolean newValue = change.getNewValue()
                                 .getLifecycleFlags()
                                 .getDeleted();
        if (!newValue && oldValue) {
            RestoreEntity command = RestoreEntity
                    .newBuilder()
                    .setId(id)
                    .addAllMessageId(messageIds)
                    .build();
            systemGateway.postCommand(command);
        }
    }

    private static Collection<DispatchedMessageId>
    toDispatched(Collection<? extends Message> messageIds) {
        Collection<DispatchedMessageId> dispatchedMessageIds =
                messageIds.stream()
                          .map(EntityLifecycle::dispatchedMessageId)
                          .collect(toList());
        return dispatchedMessageIds;
    }

    private static EntityHistoryId historyId(Object id, TypeUrl entityType) {
        EntityId entityId = EntityId
                .newBuilder()
                .setId(pack(id))
                .build();
        EntityHistoryId historyId = EntityHistoryId
                .newBuilder()
                .setEntityId(entityId)
                .setTypeUrl(entityType.value())
                .build();
        return historyId;
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private static DispatchedMessageId dispatchedMessageId(Message messageId) {
        checkNotNull(messageId);
        if (messageId instanceof EventId) {
            EventId eventId = (EventId) messageId;
            return DispatchedMessageId.newBuilder()
                                      .setEventId(eventId)
                                      .build();
        } else if (messageId instanceof CommandId) {
            CommandId commandId = (CommandId) messageId;
            return DispatchedMessageId.newBuilder()
                                      .setCommandId(commandId)
                                      .build();
        } else {
            throw newIllegalArgumentException(
                    "Unexpected message ID of type %s. Expected EventId or CommandId.",
                    messageId.getClass()
            );
        }
    }
}
