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

package io.spine.server.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import io.spine.annotation.Internal;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.EntityQualifier;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.MessageId;
import io.spine.core.MessageQualifier;
import io.spine.core.Version;
import io.spine.option.EntityOption;
import io.spine.system.server.CommandTarget;
import io.spine.system.server.ConstraintViolated;
import io.spine.system.server.DispatchedMessageId;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.SystemWriteSide;
import io.spine.system.server.command.AssignTargetToCommand;
import io.spine.system.server.command.DispatchCommandToHandler;
import io.spine.system.server.command.DispatchEventToReactor;
import io.spine.system.server.command.DispatchEventToSubscriber;
import io.spine.system.server.event.CommandHandled;
import io.spine.system.server.event.CommandRejected;
import io.spine.system.server.event.EntityArchived;
import io.spine.system.server.event.EntityCreated;
import io.spine.system.server.event.EntityDeleted;
import io.spine.system.server.event.EntityRestored;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.system.server.event.EntityUnarchived;
import io.spine.system.server.event.EventImported;
import io.spine.type.TypeUrl;
import io.spine.validate.ValidationError;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.base.Time.currentTime;
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
@Internal
@SuppressWarnings("OverlyCoupledClass") // Posts system messages in multiple cases.
public class EntityLifecycle {

    /**
     * The {@link SystemWriteSide} which the system messages are posted into.
     */
    private final SystemWriteSide systemWriteSide;

    /**
     * The {@link EventFilter} applied to system events before posting.
     */
    private final EventFilter eventFilter;

    /**
     * The ID of {@linkplain io.spine.system.server.EntityHistory history} of the associated
     * {@link Entity}.
     *
     * <p>Most commands posted by the {@code EntityLifecycle} are handled by
     * the {@code io.spine.system.server.EntityHistoryAggregate}.
     * Thus, storing an ID as a field is convenient.
     */
    private final EntityHistoryId historyId;

    /**
     * Creates a new instance.
     *
     * <p>Use this constructor for test purposes <b>only</b>.
     *
     * @see EntityLifecycle.Builder
     */
    @VisibleForTesting
    protected EntityLifecycle(Object entityId,
                              TypeUrl entityType,
                              SystemWriteSide writeSide,
                              EventFilter eventFilter) {
        this.systemWriteSide = checkNotNull(writeSide);
        this.eventFilter = checkNotNull(eventFilter);
        this.historyId = EntityHistoryIds.wrap(entityId, entityType);
    }

    private EntityLifecycle(Builder builder) {
        this(builder.entityId, builder.entityType, builder.writeSide, builder.eventFilter);
    }

    /**
     * Posts the {@link EntityCreated} system event.
     *
     * @param entityKind
     *         the kind of the created entity
     */
    public final void onEntityCreated(EntityOption.Kind entityKind) {
        EntityCreated event = EntityCreated
                .newBuilder()
                .setId(historyId)
                .setKind(entityKind)
                .vBuild();
        postEvent(event);
    }

    /**
     * Posts the {@link AssignTargetToCommand AssignTargetToCommand}
     * system command.
     *
     * @param commandId
     *         the ID of the command which should be handled by the entity
     */
    public final void onTargetAssignedToCommand(CommandId commandId) {
        CommandTarget target = CommandTarget
                .newBuilder()
                .setEntityId(historyId.getEntityId())
                .setTypeUrl(historyId.getTypeUrl())
                .vBuild();
        AssignTargetToCommand command = AssignTargetToCommand
                .newBuilder()
                .setId(commandId)
                .setTarget(target)
                .vBuild();
        postCommand(command);
    }

    /**
     * Posts the {@link DispatchCommandToHandler} system command.
     *
     * @param command
     *         the dispatched command
     */
    public final void onDispatchCommand(Command command) {
        DispatchCommandToHandler systemCommand = DispatchCommandToHandler
                .newBuilder()
                .setReceiver(historyId)
                .setCommand(command)
                .vBuild();
        postCommand(systemCommand);
    }

    /**
     * Posts the {@link CommandHandled} system event.
     *
     * @param command
     *         the handled command
     */
    public final void onCommandHandled(Command command) {
        CommandHandled systemEvent = CommandHandled
                .newBuilder()
                .setId(command.getId())
                .vBuild();
        postEvent(systemEvent);
    }

    /**
     * Posts the {@link CommandRejected} system event.
     *
     * @param commandId
     *         the ID of the rejected command
     * @param rejection
     *         the rejection event
     */
    public final void onCommandRejected(CommandId commandId, Event rejection) {
        CommandRejected systemEvent = CommandRejected
                .newBuilder()
                .setId(commandId)
                .setRejectionEvent(rejection)
                .vBuild();
        postEvent(systemEvent);
    }

    /**
     * Posts the {@link DispatchEventToSubscriber} system command.
     *
     * @param event
     *         the dispatched event
     */
    public final void onDispatchEventToSubscriber(Event event) {
        DispatchEventToSubscriber systemCommand = DispatchEventToSubscriber
                .newBuilder()
                .setReceiver(historyId)
                .setEvent(event)
                .vBuild();
        postCommand(systemCommand);
    }

    public final void onEventImported(Event event) {
        EventImported systemEvent = EventImported
                .newBuilder()
                .setReceiver(historyId)
                .setEventId(event.getId())
                .setWhenImported(currentTime())
                .vBuild();
        postEvent(systemEvent);
    }

    /**
     * Posts the {@link DispatchEventToReactor} system command.
     *
     * @param event
     *         the dispatched event
     */
    public final void onDispatchEventToReactor(Event event) {
        DispatchEventToReactor systemCommand = DispatchEventToReactor
                .newBuilder()
                .setReceiver(historyId)
                .setEvent(event)
                .vBuild();
        postCommand(systemCommand);
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
     *         {@link EventId EventId}s or {@link CommandId}s
     */
    public final void onStateChanged(EntityRecordChange change,
                                     Set<? extends MessageId> messageIds) {
        Collection<DispatchedMessageId> dispatchedMessageIds = toDispatched(messageIds);

        postIfChanged(change, dispatchedMessageIds);
        postIfArchived(change, dispatchedMessageIds);
        postIfDeleted(change, dispatchedMessageIds);
        postIfExtracted(change, dispatchedMessageIds);
        postIfRestored(change, dispatchedMessageIds);
    }

    public final void onInvalidEntity(MessageQualifier cause,
                                      MessageQualifier root,
                                      ValidationError error,
                                      Version version) {
        EntityQualifier qualifier = EntityQualifier
                .newBuilder()
                .setId(historyId.getEntityId().getId())
                .setTypeUrl(historyId.getTypeUrl())
                .setVersion(version)
                .buildPartial();
        ConstraintViolated event = ConstraintViolated
                .newBuilder()
                .setEntity(qualifier)
                .setLastMessage(cause)
                .setRootMessage(root)
                .addAllViolation(error.getConstraintViolationList())
                .vBuild();
        postNotification(event);
    }

    private void postIfChanged(EntityRecordChange change,
                               Collection<DispatchedMessageId> messageIds) {
        Any oldState = change.getPreviousValue()
                             .getState();
        Any newState = change.getNewValue()
                             .getState();
        if (!oldState.equals(newState)) {
            Version newVersion = change.getNewValue()
                                       .getVersion();
            EntityStateChanged event = EntityStateChanged
                    .newBuilder()
                    .setId(historyId)
                    .setNewState(newState)
                    .addAllMessageId(ImmutableList.copyOf(messageIds))
                    .setNewVersion(newVersion)
                    .vBuild();
            postEvent(event);
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
            Version version = change.getNewValue()
                                    .getVersion();
            EntityArchived event = EntityArchived
                    .newBuilder()
                    .setId(historyId)
                    .addAllMessageId(ImmutableList.copyOf(messageIds))
                    .setVersion(version)
                    .vBuild();
            postEvent(event);
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
            Version version = change.getNewValue()
                                    .getVersion();
            EntityDeleted event = EntityDeleted
                    .newBuilder()
                    .setId(historyId)
                    .addAllMessageId(ImmutableList.copyOf(messageIds))
                    .setVersion(version)
                    .vBuild();
            postEvent(event);
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
            Version version = change.getNewValue()
                                    .getVersion();
            EntityUnarchived event = EntityUnarchived
                    .newBuilder()
                    .setId(historyId)
                    .addAllMessageId(ImmutableList.copyOf(messageIds))
                    .setVersion(version)
                    .vBuild();
            postEvent(event);
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
            Version version = change.getNewValue()
                                    .getVersion();
            EntityRestored event = EntityRestored
                    .newBuilder()
                    .setId(historyId)
                    .addAllMessageId(ImmutableList.copyOf(messageIds))
                    .setVersion(version)
                    .vBuild();
            postEvent(event);
        }
    }

    protected void postEvent(EventMessage event) {
        Optional<? extends EventMessage> filtered = eventFilter.filter(event);
        filtered.ifPresent(systemWriteSide::postEvent);
    }

    protected void postNotification(EventMessage event) {
        Optional<? extends EventMessage> filtered = eventFilter.filter(event);
        filtered.ifPresent(systemWriteSide::notifySystem);
    }

    protected void postCommand(CommandMessage command) {
        systemWriteSide.postCommand(command);
    }

    private static Collection<DispatchedMessageId>
    toDispatched(Collection<? extends MessageId> messageIds) {
        Collection<DispatchedMessageId> dispatchedMessageIds =
                messageIds.stream()
                          .map(EntityLifecycle::dispatchedMessageId)
                          .collect(toList());
        return dispatchedMessageIds;
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private static DispatchedMessageId dispatchedMessageId(MessageId messageId) {
        checkNotNull(messageId);
        DispatchedMessageId.Builder builder = DispatchedMessageId.newBuilder();
        if (messageId instanceof EventId) {
            EventId eventId = (EventId) messageId;
            return builder.setEventId(eventId)
                          .vBuild();
        } else if (messageId instanceof CommandId) {
            CommandId commandId = (CommandId) messageId;
            return builder.setCommandId(commandId)
                          .vBuild();
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
        private SystemWriteSide writeSide;
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

        Builder setSystemWriteSide(SystemWriteSide writeSide) {
            this.writeSide = checkNotNull(writeSide);
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
            checkState(writeSide != null);
            if (eventFilter == null) {
                eventFilter = allowAll();
            }
            return new EntityLifecycle(this);
        }
    }
}
