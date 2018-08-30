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
import io.spine.client.EntityIdVBuilder;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.option.EntityOption;
import io.spine.system.server.ArchiveEntity;
import io.spine.system.server.AssignTargetToCommand;
import io.spine.system.server.AssignTargetToCommandVBuilder;
import io.spine.system.server.ChangeEntityState;
import io.spine.system.server.CommandHandled;
import io.spine.system.server.CommandHandledVBuilder;
import io.spine.system.server.CommandRejected;
import io.spine.system.server.CommandRejectedVBuilder;
import io.spine.system.server.CommandTarget;
import io.spine.system.server.CreateEntity;
import io.spine.system.server.CreateEntityVBuilder;
import io.spine.system.server.DeleteEntity;
import io.spine.system.server.DispatchCommandToHandler;
import io.spine.system.server.DispatchCommandToHandlerVBuilder;
import io.spine.system.server.DispatchEventToReactor;
import io.spine.system.server.DispatchEventToSubscriber;
import io.spine.system.server.DispatchEventToSubscriberVBuilder;
import io.spine.system.server.DispatchedEvent;
import io.spine.system.server.DispatchedEventVBuilder;
import io.spine.system.server.DispatchedMessageId;
import io.spine.system.server.DispatchedMessageIdVBuilder;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.EntityHistoryIdVBuilder;
import io.spine.system.server.EventImported;
import io.spine.system.server.EventImportedVBuilder;
import io.spine.system.server.ExtractEntityFromArchive;
import io.spine.system.server.RestoreEntity;
import io.spine.system.server.SystemGateway;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Identifier.pack;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.util.stream.Collectors.toList;

/**
 * The lifecycle callbacks of an {@link Entity}.
 *
 * <p>On each call, posts from zero to several system commands. See the individual method
 * descriptions for more info about the posted commands.
 *
 * <p>An instance of {@code EntityLifecycle} is associated with a single instance of entity.
 *
 * @see Repository#lifecycleOf(Object) Repository.lifecycleOf(I)
 */
@SuppressWarnings("OverlyCoupledClass") // Posts system commands.
public class EntityLifecycle {

    /**
     * The {@link SystemGateway} which the system commands are posted into.
     */
    private final SystemGateway systemGateway;

    /**
     * The ID of {@linkplain io.spine.system.server.EntityHistory history} of the associated
     * {@link Entity}.
     *
     * <p>Most commands posted by the {@code EntityLifecycle} are handled by
     * the {@link io.spine.system.server.EntityHistoryAggregate EntityHistoryAggregate}. Thus,
     * storing an ID as a field is convenient.
     */
    private final EntityHistoryId historyId;

    /**
     * Creates a new instance.
     *
     * <p>Use this constructor for test purposes <b>only</b>.
     *
     * @see #create(Object, TypeUrl, SystemGateway) EntityLifecycle.create(...) to instantiate
     *                                              the class
     */
    @VisibleForTesting
    protected EntityLifecycle(Object historyId, TypeUrl entityType, SystemGateway gateway) {
        this.systemGateway = gateway;
        this.historyId = historyId(historyId, entityType);
    }

    /**
     * Creates a new instance of {@code EntityLifecycle}.
     *
     * @param history    the ID of the associated entity history
     * @param entityType the type of the associated entity
     * @param gateway    the {@link SystemGateway} to post the system commands
     * @return new instance of {@code EntityLifecycle}
     */
    static EntityLifecycle create(Object history, TypeUrl entityType, SystemGateway gateway) {
        return new EntityLifecycle(history, entityType, gateway);
    }

    /**
     * Posts the {@link CreateEntity} system command.
     *
     * @param entityKind the {@link EntityOption.Kind} of the created entity
     */
    public void onEntityCreated(EntityOption.Kind entityKind) {
        CreateEntity command = CreateEntityVBuilder
                .newBuilder()
                .setId(historyId)
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
                .setEntityId(historyId.getEntityId())
                .setTypeUrl(historyId.getTypeUrl())
                .build();
        AssignTargetToCommand command = AssignTargetToCommandVBuilder
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
        DispatchCommandToHandler systemCommand = DispatchCommandToHandlerVBuilder
                .newBuilder()
                .setReceiver(historyId)
                .setCommandId(command.getId())
                .build();
        systemGateway.postCommand(systemCommand);
    }

    /**
     * Posts the {@link CommandHandled} system event.
     *
     * @param command the handled command
     */
    public void onCommandHandled(Command command) {
        CommandHandled systemEvent = CommandHandledVBuilder
                .newBuilder()
                .setId(command.getId())
                .build();
        systemGateway.postEvent(systemEvent);
    }

    /**
     * Posts the {@link CommandRejected} system event.
     *
     * @param commandId the ID of the rejected command
     * @param rejection the rejection event
     */
    public void onCommandRejected(CommandId commandId, Event rejection) {
        CommandRejected systemCommand = CommandRejectedVBuilder
                .newBuilder()
                .setId(commandId)
                .setRejectionEvent(rejection)
                .build();
        systemGateway.postEvent(systemCommand);
    }

    /**
     * Posts the {@link DispatchEventToSubscriber} system command.
     *
     * @param event the dispatched event
     */
    public void onDispatchEventToSubscriber(Event event) {
        DispatchEventToSubscriber systemCommand = DispatchEventToSubscriberVBuilder
                .newBuilder()
                .setReceiver(historyId)
                .setEventId(event.getId())
                .build();
        systemGateway.postCommand(systemCommand);
    }

    public void onImportTargetSet(EventId id) {
        //TODO:2018-08-22:alexander.yevsyukov: Implement as posting event when import is finished.
    }

    public void onEventImported(Event event) {
        DispatchedEvent dispatchedEvent = DispatchedEventVBuilder
                .newBuilder()
                .setEvent(event.getId())
                .setWhenDispatched(getCurrentTime())
                .build();
        EventImported systemEvent = EventImportedVBuilder
                .newBuilder()
                .setReceiver(historyId)
                .setPayload(dispatchedEvent)
                .build();
        systemGateway.postEvent(systemEvent);
    }

    /**
     * Posts the {@link DispatchEventToReactor} system command.
     *
     * @param event the dispatched event
     */
    public void onDispatchEventToReactor(Event event) {
        DispatchEventToReactor systemCommand = DispatchEventToReactor
                .newBuilder()
                .setReceiver(historyId)
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
                    .setId(historyId)
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
                    .setId(historyId)
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
                    .setId(historyId)
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
                    .setId(historyId)
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
                    .setId(historyId)
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
        EntityId entityId = EntityIdVBuilder
                .newBuilder()
                .setId(pack(id))
                .build();
        EntityHistoryId historyId = EntityHistoryIdVBuilder
                .newBuilder()
                .setEntityId(entityId)
                .setTypeUrl(entityType.value())
                .build();
        return historyId;
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private static DispatchedMessageId dispatchedMessageId(Message messageId) {
        checkNotNull(messageId);
        DispatchedMessageIdVBuilder builder = DispatchedMessageIdVBuilder.newBuilder();
        if (messageId instanceof EventId) {
            EventId eventId = (EventId) messageId;
            return builder.setEventId(eventId)
                          .build();
        } else if (messageId instanceof CommandId) {
            CommandId commandId = (CommandId) messageId;
            return builder.setCommandId(commandId)
                          .build();
        } else {
            throw newIllegalArgumentException(
                    "Unexpected message ID of type %s. Expected EventId or CommandId.",
                    messageId.getClass()
            );
        }
    }
}
