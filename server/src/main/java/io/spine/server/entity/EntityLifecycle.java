/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.base.EventMessage;
import io.spine.client.EntityId;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.CommandValidationError;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.EventValidationError;
import io.spine.core.MessageId;
import io.spine.core.Origin;
import io.spine.core.Version;
import io.spine.option.EntityOption;
import io.spine.server.Identity;
import io.spine.server.dispatch.BatchDispatchOutcome;
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.SignalEnvelope;
import io.spine.system.server.AggregateHistoryCorrupted;
import io.spine.system.server.CannotDispatchDuplicateCommand;
import io.spine.system.server.CannotDispatchDuplicateEvent;
import io.spine.system.server.CommandTarget;
import io.spine.system.server.ConstraintViolated;
import io.spine.system.server.EntityTypeName;
import io.spine.system.server.HandlerFailedUnexpectedly;
import io.spine.system.server.SystemWriteSide;
import io.spine.system.server.event.CommandDispatchedToHandler;
import io.spine.system.server.event.CommandHandled;
import io.spine.system.server.event.CommandRejected;
import io.spine.system.server.event.EntityArchived;
import io.spine.system.server.event.EntityCreated;
import io.spine.system.server.event.EntityDeleted;
import io.spine.system.server.event.EntityRestored;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.system.server.event.EntityUnarchived;
import io.spine.system.server.event.EventDispatchedToReactor;
import io.spine.system.server.event.EventDispatchedToSubscriber;
import io.spine.system.server.event.EventImported;
import io.spine.system.server.event.MigrationApplied;
import io.spine.system.server.event.TargetAssignedToCommand;
import io.spine.type.TypeUrl;
import io.spine.validate.ValidationError;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.base.Time.currentTime;
import static io.spine.core.CommandValidationError.DUPLICATE_COMMAND_VALUE;
import static io.spine.core.EventValidationError.DUPLICATE_EVENT_VALUE;
import static io.spine.server.entity.EventFilter.allowAll;

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
@SuppressWarnings({"OverlyCoupledClass", "ClassWithTooManyMethods"})
    // Posts system messages in multiple cases.
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
     * The message ID of of the associated {@link Entity} state.
     *
     * <p>Most commands posted by the {@code EntityLifecycle} are handled by
     * the {@code io.spine.system.server.EntityHistoryAggregate}.
     * Thus, storing an ID as a field is convenient.
     */
    private final MessageId entityId;

    private final EntityTypeName typeName;

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
                              EventFilter eventFilter,
                              EntityTypeName typeName) {
        this.systemWriteSide = checkNotNull(writeSide);
        this.eventFilter = checkNotNull(eventFilter);
        this.typeName = checkNotNull(typeName);
        this.entityId = Identity.ofEntity(entityId, entityType);
    }

    private EntityLifecycle(Builder builder) {
        this(builder.entityId,
             builder.entityType.stateTypeUrl(),
             builder.writeSide,
             builder.eventFilter,
             builder.entityType.typeName());
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
                .setEntity(entityId)
                .setKind(entityKind)
                .vBuild();
        postEvent(event);
    }

    /**
     * Posts the {@link TargetAssignedToCommand}
     * system command.
     *
     * @param commandId
     *         the ID of the command which should be handled by the entity
     */
    public final void onTargetAssignedToCommand(CommandId commandId) {
        EntityId entityId = EntityId
                .newBuilder()
                .setId(this.entityId.getId())
                .buildPartial();
        CommandTarget target = CommandTarget
                .newBuilder()
                .setEntityId(entityId)
                .setTypeUrl(this.entityId.getTypeUrl())
                .vBuild();
        TargetAssignedToCommand event = TargetAssignedToCommand
                .newBuilder()
                .setId(commandId)
                .setTarget(target)
                .vBuild();
        postEvent(event);
    }

    /**
     * Posts the {@link io.spine.system.server.event.CommandDispatchedToHandler} system command.
     *
     * @param command
     *         the dispatched command
     */
    public final void onDispatchCommand(Command command) {
        CommandDispatchedToHandler systemCommand = CommandDispatchedToHandler
                .newBuilder()
                .setReceiver(entityId)
                .setPayload(command)
                .setWhenDispatched(currentTime())
                .setEntityType(typeName)
                .vBuild();
        Origin systemEventOrigin = CommandEnvelope.of(command)
                                                  .asMessageOrigin();
        postEvent(systemCommand, systemEventOrigin);
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
        Origin systemEventOrigin = CommandEnvelope.of(command)
                                                  .asMessageOrigin();
        postEvent(systemEvent, systemEventOrigin);
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
        Origin systemEventOrigin = RejectionEnvelope.from(EventEnvelope.of(rejection))
                                                    .asMessageOrigin();
        postEvent(systemEvent, systemEventOrigin);
    }

    /**
     * Posts the {@link EventDispatchedToSubscriber} system event.
     *
     * @param event
     *         the dispatched event
     */
    public final void onDispatchEventToSubscriber(Event event) {
        EventDispatchedToSubscriber systemCommand = EventDispatchedToSubscriber
                .newBuilder()
                .setReceiver(entityId)
                .setPayload(event)
                .setWhenDispatched(currentTime())
                .setEntityType(typeName)
                .vBuild();
        Origin systemEventOrigin = EventEnvelope.of(event)
                                                .asMessageOrigin();
        postEvent(systemCommand, systemEventOrigin);
    }

    public final void onEventImported(Event event) {
        EventImported systemEvent = EventImported
                .newBuilder()
                .setReceiver(entityId)
                .setPayload(event)
                .setWhenImported(currentTime())
                .setEntityType(typeName)
                .vBuild();
        Origin systemEventOrigin = EventEnvelope.of(event)
                                                .asMessageOrigin();
        postEvent(systemEvent, systemEventOrigin);
    }

    /**
     * Posts the {@link EventDispatchedToReactor} system event.
     *
     * @param event
     *         the dispatched event
     */
    public final void onDispatchEventToReactor(Event event) {
        EventDispatchedToReactor systemCommand = EventDispatchedToReactor
                .newBuilder()
                .setReceiver(entityId)
                .setPayload(event)
                .setWhenDispatched(currentTime())
                .setEntityType(typeName)
                .vBuild();
        Origin systemEventOrigin = EventEnvelope.of(event)
                                                .asMessageOrigin();
        postEvent(systemCommand, systemEventOrigin);
    }

    /**
     * Posts the {@link EntityStateChanged} system event and the events related to
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
                                     Set<? extends MessageId> messageIds,
                                     Origin origin) {
        postIfChanged(change, messageIds, origin);
        postIfArchived(change, messageIds);
        postIfDeleted(change, messageIds);
        postIfExtracted(change, messageIds);
        postIfRestored(change, messageIds);
    }

    /**
     * Posts the {@link EntityDeleted} event signaling that the entity record was removed from the
     * storage.
     *
     * @param signalIds
     *         the IDs of handled messages that caused the deletion
     */
    public final void onRemovedFromStorage(Iterable<MessageId> signalIds) {
        EntityDeleted event = EntityDeleted
                .newBuilder()
                .setEntity(entityId)
                .addAllSignalId(ImmutableList.copyOf(signalIds))
                .setRemovedFromStorage(true)
                .vBuild();
        postEvent(event);
    }

    /**
     * Posts the {@link MigrationApplied} event.
     *
     * @return the event or an empty {@code Optional} if the posting was blocked by the
     *         {@link #eventFilter}
     */
    public final Optional<Event> onMigrationApplied() {
        MigrationApplied systemEvent = MigrationApplied
                .newBuilder()
                .setEntity(entityId)
                .setWhen(currentTime())
                .build();
        return postEvent(systemEvent);
    }

    /**
     * Posts the {@link ConstraintViolated} system event.
     *
     * @param lastMessage
     *         the last message handled by the entity
     * @param root
     *         the root message of the message chain which led to the violation
     * @param error
     *         the description of violation
     * @param version
     *         the version of the invalid entity
     */
    public final void onInvalidEntity(MessageId lastMessage,
                                      MessageId root,
                                      ValidationError error,
                                      Version version) {
        MessageId withNewVersion = entityId.withVersion(version);
        ConstraintViolated event = ConstraintViolated
                .newBuilder()
                .setEntity(withNewVersion)
                .setLastMessage(lastMessage)
                .setRootMessage(root)
                .addAllViolation(error.getConstraintViolationList())
                .vBuild();
        postEvent(event);
    }

    /**
     * Posts a diagnostic event on a dispatching error.
     *
     * <p>Depending on the error type and code, may emit {@link HandlerFailedUnexpectedly},
     * {@link CannotDispatchDuplicateEvent}, or {@link CannotDispatchDuplicateCommand}.
     *
     * @param signal
     *         the dispatched signal
     * @param error
     *         the dispatching error
     */
    public final void onDispatchingFailed(SignalEnvelope<?, ?, ?> signal, Error error) {
        checkNotNull(signal);
        checkNotNull(error);
        boolean duplicate = postIfDuplicate(signal, error);
        if (!duplicate) {
            postHandlerFailed(signal.messageId(), error);
        }
    }

    public void onDuplicateEvent(EventEnvelope event) {
        checkNotNull(event);
        @SuppressWarnings("deprecation") // Set the deprecated field for compatibility.
        CannotDispatchDuplicateEvent systemEvent = CannotDispatchDuplicateEvent
                .newBuilder()
                .setEntity(entityId)
                .setEvent(event.id())
                .setDuplicateEvent(event.messageId())
                .vBuild();
        postEvent(systemEvent);
    }

    public void onDuplicateCommand(CommandEnvelope command) {
        checkNotNull(command);
        @SuppressWarnings("deprecation") // Set the deprecated field for compatibility.
        CannotDispatchDuplicateCommand systemEvent = CannotDispatchDuplicateCommand
                .newBuilder()
                .setEntity(entityId)
                .setCommand(command.id())
                .setDuplicateCommand(command.messageId())
                .vBuild();
        postEvent(systemEvent);
    }

    public void onCorruptedState(BatchDispatchOutcome outcome) {
        List<DispatchOutcome> outcomes = outcome.getOutcomeList();
        MessageId lastSuccessful = MessageId.getDefaultInstance();
        MessageId erroneous = null;
        Error error = null;
        int interruptedCount = 0;
        for (DispatchOutcome dispatchOutcome : outcomes) {
            if (dispatchOutcome.hasSuccess()) {
                lastSuccessful = dispatchOutcome.getPropagatedSignal();
            } else if (dispatchOutcome.hasError()) {
                erroneous = dispatchOutcome.getPropagatedSignal();
                error = dispatchOutcome.getError();
            } else {
                interruptedCount++;
            }
        }
        if (error == null) {
            error = Error.getDefaultInstance();
        }
        if (erroneous == null) {
            erroneous = MessageId.getDefaultInstance();
        }
        AggregateHistoryCorrupted event = AggregateHistoryCorrupted
                .newBuilder()
                .setEntity(entityId)
                .setEntityType(typeName)
                .setLastSuccessfulEvent(lastSuccessful)
                .setErroneousEvent(erroneous)
                .setError(error)
                .setInterruptedEvents(interruptedCount)
                .vBuild();
        postEvent(event);
    }

    private void postIfChanged(EntityRecordChange change,
                               Collection<? extends MessageId> messageIds,
                               Origin origin) {
        Any oldState = change.getPreviousValue()
                             .getState();
        Any newState = change.getNewValue()
                             .getState();
        if (!oldState.equals(newState)) {
            Version newVersion = change.getNewValue()
                                       .getVersion();
            EntityStateChanged event = EntityStateChanged
                    .newBuilder()
                    .setEntity(entityId)
                    .setOldState(oldState)
                    .setNewState(newState)
                    .addAllSignalId(messageIds)
                    .setNewVersion(newVersion)
                    .vBuild();
            postEvent(event, origin);
        }
    }

    private void postIfArchived(EntityRecordChange change,
                                Collection<? extends MessageId> messageIds) {
        boolean oldValue = change.getPreviousValue()
                                 .getLifecycleFlags()
                                 .getArchived();
        boolean newValue = change.getNewValue()
                                 .getLifecycleFlags()
                                 .getArchived();
        if (newValue && !oldValue) {
            Version version = change.getNewValue()
                                    .getVersion();
            Any lastKnownState = change.getPreviousValue()
                                       .getState();
            EntityArchived event = EntityArchived
                    .newBuilder()
                    .setEntity(entityId)
                    .addAllSignalId(ImmutableList.copyOf(messageIds))
                    .setVersion(version)
                    .setLastState(lastKnownState)
                    .vBuild();
            postEvent(event);
        }
    }

    private void postIfDeleted(EntityRecordChange change,
                               Collection<? extends MessageId> messageIds) {
        boolean oldValue = change.getPreviousValue()
                                 .getLifecycleFlags()
                                 .getDeleted();
        boolean newValue = change.getNewValue()
                                 .getLifecycleFlags()
                                 .getDeleted();
        if (newValue && !oldValue) {
            Version version = change.getNewValue()
                                    .getVersion();
            Any lastKnownState = change.getPreviousValue()
                                       .getState();
            EntityDeleted event = EntityDeleted
                    .newBuilder()
                    .setEntity(entityId)
                    .addAllSignalId(ImmutableList.copyOf(messageIds))
                    .setVersion(version)
                    .setMarkedAsDeleted(true)
                    .setLastState(lastKnownState)
                    .vBuild();
            postEvent(event);
        }
    }

    private void postIfExtracted(EntityRecordChange change,
                                 Collection<? extends MessageId> messageIds) {
        boolean oldValue = change.getPreviousValue()
                                 .getLifecycleFlags()
                                 .getArchived();
        boolean newValue = change.getNewValue()
                                 .getLifecycleFlags()
                                 .getArchived();
        if (!newValue && oldValue) {
            EntityRecord newRecord = change.getNewValue();
            Version version = newRecord.getVersion();
            EntityUnarchived event = EntityUnarchived
                    .newBuilder()
                    .setEntity(entityId)
                    .addAllSignalId(ImmutableList.copyOf(messageIds))
                    .setVersion(version)
                    .setState(newRecord.getState())
                    .vBuild();
            postEvent(event);
        }
    }

    private void postIfRestored(EntityRecordChange change,
                                Collection<? extends MessageId> messageIds) {
        boolean oldValue = change.getPreviousValue()
                                 .getLifecycleFlags()
                                 .getDeleted();
        boolean newValue = change.getNewValue()
                                 .getLifecycleFlags()
                                 .getDeleted();
        if (!newValue && oldValue) {
            EntityRecord newRecord = change.getNewValue();
            Version version = newRecord.getVersion();
            EntityRestored event = EntityRestored
                    .newBuilder()
                    .setEntity(entityId)
                    .addAllSignalId(ImmutableList.copyOf(messageIds))
                    .setVersion(version)
                    .setState(newRecord.getState())
                    .vBuild();
            postEvent(event);
        }
    }

    private boolean postIfDuplicate(SignalEnvelope<?, ?, ?> handledSignal, Error error) {
        return postIfDuplicateCommand(handledSignal, error)
            || postIfDuplicateEvent(handledSignal, error);
    }

    private boolean postIfDuplicateCommand(SignalEnvelope<?, ?, ?> handledSignal, Error error) {
        String errorType = error.getType();
        int errorCode = error.getCode();
        boolean duplicateCommand =
                errorType.equals(CommandValidationError.class.getSimpleName())
                        && errorCode == DUPLICATE_COMMAND_VALUE;
        if (duplicateCommand) {
            CommandEnvelope asCommand = (CommandEnvelope) handledSignal;
            onDuplicateCommand(asCommand);
        }
        return duplicateCommand;
    }

    private boolean postIfDuplicateEvent(SignalEnvelope<?, ?, ?> handledSignal, Error error) {
        String errorType = error.getType();
        int errorCode = error.getCode();
        boolean duplicateEvent =
                errorType.equals(EventValidationError.class.getSimpleName())
                        && errorCode == DUPLICATE_EVENT_VALUE;
        if (duplicateEvent) {
            EventEnvelope asEvent = (EventEnvelope) handledSignal;
            onDuplicateEvent(asEvent);
        }
        return duplicateEvent;
    }

    private void postHandlerFailed(MessageId handledSignal, Error error) {
        HandlerFailedUnexpectedly systemEvent = HandlerFailedUnexpectedly
                .newBuilder()
                .setEntity(entityId)
                .setHandledSignal(handledSignal)
                .setError(error)
                .vBuild();
        postEvent(systemEvent);
    }

    /**
     * Posts a system event with the specified origin.
     *
     * @param event
     *         an event to post
     * @param explicitOrigin
     *         the event origin
     * @return an instance of posted {@code Event} if it was actually posted and an empty
     *         {@code Optional} if the event was intercepted by the {@link #eventFilter}
     */
    @CanIgnoreReturnValue
    protected Optional<Event> postEvent(EventMessage event, Origin explicitOrigin) {
        Optional<? extends EventMessage> filtered = eventFilter.filter(event);
        Optional<Event> result =
                filtered.map(systemEvent -> systemWriteSide.postEvent(systemEvent, explicitOrigin));
        return result;
    }

    /**
     * Posts an event to a system write side.
     *
     * @param event
     *         an event to post
     * @return an instance of posted {@code Event} if it was actually posted and an empty
     *         {@code Optional} if the event was intercepted by the {@link #eventFilter}
     */
    @CanIgnoreReturnValue
    protected Optional<Event> postEvent(EventMessage event) {
        Optional<? extends EventMessage> filtered = eventFilter.filter(event);
        Optional<Event> result = filtered.map(systemWriteSide::postEvent);
        return result;
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
        private EntityClass<?> entityType;
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

        Builder setEntityType(EntityClass<?> entityType) {
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
