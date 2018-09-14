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
import io.spine.system.server.AssignTargetToCommand;
import io.spine.system.server.AssignTargetToCommandVBuilder;
import io.spine.system.server.CommandHandled;
import io.spine.system.server.CommandHandledVBuilder;
import io.spine.system.server.CommandRejected;
import io.spine.system.server.CommandRejectedVBuilder;
import io.spine.system.server.CommandTarget;
import io.spine.system.server.DispatchCommandToHandler;
import io.spine.system.server.DispatchCommandToHandlerVBuilder;
import io.spine.system.server.DispatchEventToReactor;
import io.spine.system.server.DispatchEventToSubscriber;
import io.spine.system.server.DispatchEventToSubscriberVBuilder;
import io.spine.system.server.DispatchedMessageId;
import io.spine.system.server.DispatchedMessageIdVBuilder;
import io.spine.system.server.EntityArchived;
import io.spine.system.server.EntityArchivedVBuilder;
import io.spine.system.server.EntityCreated;
import io.spine.system.server.EntityCreatedVBuilder;
import io.spine.system.server.EntityDeleted;
import io.spine.system.server.EntityDeletedVBuilder;
import io.spine.system.server.EntityExtractedFromArchive;
import io.spine.system.server.EntityExtractedFromArchiveVBuilder;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.EntityHistoryIdVBuilder;
import io.spine.system.server.EntityRestored;
import io.spine.system.server.EntityRestoredVBuilder;
import io.spine.system.server.EntityStateChanged;
import io.spine.system.server.EntityStateChangedVBuilder;
import io.spine.system.server.EventImported;
import io.spine.system.server.EventImportedVBuilder;
import io.spine.system.server.SystemGateway;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.copyOf;
import static io.spine.base.Identifier.pack;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.server.entity.EventFilter.allowAll;
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
     * The {@link EventFilter} to apply to the system events emitted by this lifecycle.
     */
    private final EventFilter eventFilter;

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
     * @see Builder
     */
    @VisibleForTesting
    protected EntityLifecycle(Object entityId,
                              TypeUrl entityType,
                              SystemGateway gateway,
                              EventFilter eventFilter) {
        this.systemGateway = gateway;
        this.historyId = historyId(entityId, entityType);
        this.eventFilter = eventFilter;
    }

    private EntityLifecycle(Builder builder) {
        this(builder.entityId, builder.entityType, builder.gateway, builder.eventFilter);
    }

    /**
     * Posts the {@link EntityCreated} system event.
     *
     * @param entityKind
     *         the {@link EntityOption.Kind} of the created entity
     */
    public void onEntityCreated(EntityOption.Kind entityKind) {
        EntityCreated event = EntityCreatedVBuilder
                .newBuilder()
                .setId(historyId)
                .setKind(entityKind)
                .build();
        systemGateway.postEvent(event);
    }

    /**
     * Posts the {@link io.spine.system.server.AssignTargetToCommand AssignTargetToCommand}
     * system command.
     *
     * @param commandId
     *         the ID of the command which should be handled by the entity
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
     * @param command
     *         the dispatched command
     */
    public void onDispatchCommand(Command command) {
        DispatchCommandToHandler systemCommand = DispatchCommandToHandlerVBuilder
                .newBuilder()
                .setReceiver(historyId)
                .setCommand(command)
                .build();
        systemGateway.postCommand(systemCommand);
    }

    /**
     * Posts the {@link CommandHandled} system event.
     *
     * @param command
     *         the handled command
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
     * @param commandId
     *         the ID of the rejected command
     * @param rejection
     *         the rejection event
     */
    public void onCommandRejected(CommandId commandId, Event rejection) {
        CommandRejected systemEvent = CommandRejectedVBuilder
                .newBuilder()
                .setId(commandId)
                .setRejectionEvent(rejection)
                .build();
        systemGateway.postEvent(systemEvent);
    }

    /**
     * Posts the {@link DispatchEventToSubscriber} system command.
     *
     * @param event
     *         the dispatched event
     */
    public void onDispatchEventToSubscriber(Event event) {
        DispatchEventToSubscriber systemCommand = DispatchEventToSubscriberVBuilder
                .newBuilder()
                .setReceiver(historyId)
                .setEvent(event)
                .build();
        systemGateway.postCommand(systemCommand);
    }

    public void onImportTargetSet(EventId id) {
        //TODO:2018-08-22:alexander.yevsyukov: Post a system event when EventLifecycleAggregate is available.
    }

    public void onEventImported(Event event) {
        EventImported systemEvent = EventImportedVBuilder
                .newBuilder()
                .setReceiver(historyId)
                .setEventId(event.getId())
                .setWhenImported(getCurrentTime())
                .build();
        systemGateway.postEvent(systemEvent);
    }

    /**
     * Posts the {@link DispatchEventToReactor} system command.
     *
     * @param event
     *         the dispatched event
     */
    public void onDispatchEventToReactor(Event event) {
        DispatchEventToReactor systemCommand = DispatchEventToReactor
                .newBuilder()
                .setReceiver(historyId)
                .setEvent(event)
                .build();
        systemGateway.postCommand(systemCommand);
    }

    /**
     * Posts the {@link EntityStateChanged} system event and the event related to
     * the lifecycle flags.
     *
     * <p>Only the actual changes in the entity attributes result into system events.
     * If the previous and new values are equal, then no events are posted.
     *
     * @param change
     *         the change in the entity state and attributes
     * @param messageIds
     *         the IDs of the messages which caused the {@code change}; typically,
     *         {@link io.spine.core.EventId EventId}s or {@link CommandId}s
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
            EntityStateChanged event = EntityStateChangedVBuilder
                    .newBuilder()
                    .setId(historyId)
                    .setNewState(newState)
                    .addAllMessageId(copyOf(messageIds))
                    .build();
            systemGateway.postEvent(event);
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
            EntityArchived event = EntityArchivedVBuilder
                    .newBuilder()
                    .setId(historyId)
                    .addAllMessageId(copyOf(messageIds))
                    .build();
            systemGateway.postEvent(event);
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
            EntityDeleted event = EntityDeletedVBuilder
                    .newBuilder()
                    .setId(historyId)
                    .addAllMessageId(copyOf(messageIds))
                    .build();
            systemGateway.postEvent(event);
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
            EntityExtractedFromArchive event = EntityExtractedFromArchiveVBuilder
                    .newBuilder()
                    .setId(historyId)
                    .addAllMessageId(copyOf(messageIds))
                    .build();
            systemGateway.postEvent(event);
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
            EntityRestored event = EntityRestoredVBuilder
                    .newBuilder()
                    .setId(historyId)
                    .addAllMessageId(copyOf(messageIds))
                    .build();
            systemGateway.postEvent(event);
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

    /**
     * Creates a new instance of {@code Builder} for {@code EntityLifecycle} instances.
     *
     * @return new instance of {@code Builder}
     */
    static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for the {@code EntityLifecycle} instances.
     */
    static final class Builder {

        private Object entityId;
        private TypeUrl entityType;
        private SystemGateway gateway;
        private EventFilter eventFilter;

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        Builder setEntityId(Object entityId) {
            this.entityId = checkNotNull(entityId);
            return this;
        }

        Builder setEntityType(TypeUrl entityType) {
            this.entityType = checkNotNull(entityType);
            return this;
        }

        Builder setGateway(SystemGateway gateway) {
            this.gateway = checkNotNull(gateway);
            return this;
        }

        Builder setEventFilter(EventFilter eventFilter) {
            this.eventFilter = checkNotNull(eventFilter);
            return this;
        }

        /**
         * Creates a new instance of {@code EntityLifecycle}.
         *
         * @return new instance of {@code EntityLifecycle}
         */
        EntityLifecycle build() {
            checkState(entityId != null);
            checkState(entityType != null);
            checkState(gateway != null);
            if (eventFilter == null) {
                eventFilter = allowAll();
            }
            return new EntityLifecycle(this);
        }
    }
}
