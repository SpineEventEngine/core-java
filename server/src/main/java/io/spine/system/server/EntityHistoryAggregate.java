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

package io.spine.system.server;

import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.entity.LifecycleFlags;
import io.spine.system.server.command.DispatchCommandToHandler;
import io.spine.system.server.command.DispatchEventToReactor;
import io.spine.system.server.command.DispatchEventToSubscriber;
import io.spine.system.server.event.CommandDispatchedToHandler;
import io.spine.system.server.event.EntityArchived;
import io.spine.system.server.event.EntityCreated;
import io.spine.system.server.event.EntityDeleted;
import io.spine.system.server.event.EntityRestored;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.system.server.event.EntityUnarchived;
import io.spine.system.server.event.EventDispatchedToReactor;
import io.spine.system.server.event.EventDispatchedToSubscriber;
import io.spine.system.server.event.EventImported;
import io.spine.system.server.rejection.CannotDispatchCommandTwice;
import io.spine.system.server.rejection.CannotDispatchEventTwice;

import java.util.function.UnaryOperator;

import static com.google.protobuf.util.Timestamps.compare;

/**
 * The aggregate which manages the history of a single entity.
 *
 * <p>Each {@link Aggregate}, {@link io.spine.server.projection.Projection Projection},
 * and {@link io.spine.server.procman.ProcessManager ProcessManager} in the system has
 * a corresponding entity history.
 *
 * <p>The history of this aggregate has the knowledge about all the messages ever dispatched to
 * the associated entity.
 *
 * <p>An {@code EntityHistory} gives the record-based entities (such as {@code Projection}s and
 * {@code ProcessManager}s) traits of an event-sourced entity. For instance, with the help
 * of {@code EntityHistory} the history of a record-based entity can be investigated and
 * manipulated. The major use case for this facility is implementing idempotent message handlers.
 *
 * <p>This aggregate belongs to the {@code System} bounded context. This aggregate doesn't have
 * an entity history of its own.
 */
@SuppressWarnings({"OverlyCoupledClass", "ClassWithTooManyMethods"}) // OK for an Aggregate class.
final class EntityHistoryAggregate
        extends Aggregate<EntityHistoryId, EntityHistory, EntityHistory.Builder> {

    @Assign
    EventDispatchedToSubscriber handle(DispatchEventToSubscriber command)
            throws CannotDispatchEventTwice {
        Event event = command.getEvent();
        checkNotDuplicate(event);
        return EventDispatchedToSubscriber
                .newBuilder()
                .setReceiver(command.getReceiver())
                .setPayload(event)
                .setWhenDispatched(now())
                .vBuild();
    }

    @Assign
    EventDispatchedToReactor handle(DispatchEventToReactor command)
            throws CannotDispatchEventTwice {
        Event event = command.getEvent();
        checkNotDuplicate(event);
        return EventDispatchedToReactor
                .newBuilder()
                .setReceiver(command.getReceiver())
                .setPayload(event)
                .setWhenDispatched(now())
                .vBuild();
    }

    @Assign
    CommandDispatchedToHandler handle(DispatchCommandToHandler command)
            throws CannotDispatchCommandTwice {
        Command domainCommand = command.getCommand();
        checkNotDuplicate(domainCommand);
        return CommandDispatchedToHandler
                .newBuilder()
                .setReceiver(command.getReceiver())
                .setPayload(domainCommand)
                .setWhenDispatched(now())
                .vBuild();
    }

    @Apply(allowImport = true)
    private void on(EntityCreated event) {
        builder().setId(event.getId());
    }

    @Apply
    private void on(EventDispatchedToSubscriber event) {
        builder().setId(event.getReceiver());
        updateLastEventTime(event.getWhenDispatched());
    }

    @Apply
    private void on(EventDispatchedToReactor event) {
        builder().setId(event.getReceiver());
        updateLastEventTime(event.getWhenDispatched());
    }

    @Apply
    private void on(CommandDispatchedToHandler event) {
        builder().setId(event.getReceiver());
        updateLastCommandTime(event.getWhenDispatched());
    }

    @Apply(allowImport = true)
    private void on(EntityStateChanged event) {
        builder().setId(event.getId())
                 .setLastStateChange(event.getWhen());
    }

    @Apply(allowImport = true)
    private void on(EntityArchived event) {
        updateLifecycleFlags(builder -> builder.setArchived(true));
        Timestamp whenOccurred = event.getWhen();
        updateLifecycleTimestamp(builder -> builder.setWhenArchived(whenOccurred));
    }

    @Apply(allowImport = true)
    private void on(EntityDeleted event) {
        updateLifecycleFlags(builder -> builder.setDeleted(true));
        Timestamp whenOccurred = event.getWhen();
        updateLifecycleTimestamp(builder -> builder.setWhenDeleted(whenOccurred));
    }

    @Apply(allowImport = true)
    private void on(EntityUnarchived event) {
        updateLifecycleFlags(builder -> builder.setArchived(false));
        Timestamp whenOccurred = event.getWhen();
        updateLifecycleTimestamp(builder -> builder.setWhenExtractedFromArchive(whenOccurred));
    }

    @Apply(allowImport = true)
    private void on(EntityRestored event) {
        updateLifecycleFlags(builder -> builder.setDeleted(false));
        Timestamp whenOccurred = event.getWhen();
        updateLifecycleTimestamp(builder -> builder.setWhenRestored(whenOccurred));
    }

    @Apply(allowImport = true)
    private void on(EventImported event) {
        updateLastEventTime(event.getWhenImported());
    }

    @Apply(allowImport = true)
    private void on(ConstraintViolated event) {}

    private void checkNotDuplicate(Event event) throws CannotDispatchEventTwice {
        DuplicateLookup lookup = DuplicateLookup.through(recentHistory());
        boolean duplicate = lookup.isDuplicate(event);
        if (duplicate) {
            throw CannotDispatchEventTwice
                    .newBuilder()
                    .setReceiver(id())
                    .setPayload(event)
                    .setWhenDispatched(now())
                    .build();
        }
    }

    private void checkNotDuplicate(Command command) throws CannotDispatchCommandTwice {
        DuplicateLookup lookup = DuplicateLookup.through(recentHistory());
        boolean duplicate = lookup.isDuplicate(command);
        if (duplicate) {
            throw CannotDispatchCommandTwice
                    .newBuilder()
                    .setReceiver(id())
                    .setPayload(command)
                    .setWhenDispatched(now())
                    .build();
        }
    }

    private void updateLifecycleFlags(UnaryOperator<LifecycleFlags.Builder> mutation) {
        LifecycleHistory oldLifecycleHistory = builder().getLifecycle();
        LifecycleFlags.Builder flagsBuilder =
                oldLifecycleHistory.getLifecycleFlags()
                                   .toBuilder();
        LifecycleFlags newFlags = mutation.apply(flagsBuilder)
                                          .build();
        LifecycleHistory newLifecycleHistory =
                oldLifecycleHistory.toBuilder()
                                   .setLifecycleFlags(newFlags)
                                   .build();
        builder().setLifecycle(newLifecycleHistory);
    }

    private void updateLifecycleTimestamp(UnaryOperator<LifecycleHistory.Builder> mutation) {
        EntityHistory.Builder builder = builder();
        LifecycleHistory.Builder history =
                builder.getLifecycle()
                       .toBuilder();
        LifecycleHistory newHistory =
                mutation.apply(history)
                        .build();
        builder.setLifecycle(newHistory);
    }

    private void updateLastEventTime(Timestamp newEvent) {
        Timestamp lastEvent = builder().getDispatching()
                                       .getWhenEvent();
        if (compare(newEvent, lastEvent) > 0) {
            updateDispatchingHistory(builder -> builder.setWhenEvent(newEvent));
        }
    }

    private void updateLastCommandTime(Timestamp newCommand) {
        Timestamp lastCommand = builder().getDispatching()
                                         .getWhenCommand();
        if (compare(newCommand, lastCommand) > 0) {
            updateDispatchingHistory(builder -> builder.setWhenCommand(newCommand));
        }
    }

    private void updateDispatchingHistory(UnaryOperator<DispatchingHistory.Builder> mutation) {
        EntityHistory.Builder builder = builder();
        DispatchingHistory.Builder history =
                builder.getDispatching()
                       .toBuilder();
        DispatchingHistory newHistory =
                mutation.apply(history)
                        .build();
        builder.setDispatching(newHistory);
    }

    private static Timestamp now() {
        return Time.currentTime();
    }
}
