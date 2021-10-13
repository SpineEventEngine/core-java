/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.entity.given;

import com.google.common.collect.ImmutableList;
import io.spine.base.Error;
import io.spine.core.Event;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.EntityRecordChange;
import io.spine.server.entity.Phase;
import io.spine.server.entity.TransactionListener;
import io.spine.validate.NonValidated;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;

/**
 * An implementation of {@link TransactionListener} which memoizes all arguments received in the
 * callbacks.
 *
 * @param <I>
 *         ID type of the entity under transaction
 */
public final class MemoizingTransactionListener<I> implements TransactionListener<I> {

    private final List<Phase<I>> phasesOnBefore = newLinkedList();
    private final List<Phase<I>> phasesOnAfter = newLinkedList();

    private final List<@Nullable EntityRecord> recordsBeforeCommit = newLinkedList();
    private final List<@Nullable EntityRecordChange> recordsAfterCommit = newLinkedList();

    private Error lastError;
    private EntityRecord lastErroredRecord;

    private Event lastRejection;
    private EntityRecord lastRejectedRecord;

    @Override
    public void onBeforePhase(Phase<I> phase) {
        phasesOnBefore.add(phase);
    }

    @Override
    public void onAfterPhase(Phase<I> phase) {
        phasesOnAfter.add(phase);
    }

    @Override
    public void onBeforeCommit(@NonValidated EntityRecord entityRecord) {
        recordsBeforeCommit.add(entityRecord);
    }

    @Override
    public void onTransactionFailed(Error cause, @NonValidated EntityRecord entityRecord) {
        lastError = cause;
        lastErroredRecord = entityRecord;
    }

    @Override
    public void onTransactionFailed(Event cause, @NonValidated EntityRecord entityRecord) {
        lastRejection = cause;
        lastRejectedRecord = entityRecord;
    }

    @Override
    public void onAfterCommit(EntityRecordChange change) {
        recordsAfterCommit.add(change);
    }

    public ImmutableList<Phase<I>> phasesOnBefore() {
        return ImmutableList.copyOf(phasesOnBefore);
    }

    public ImmutableList<Phase<I>> phasesOnAfter() {
        return ImmutableList.copyOf(phasesOnAfter);
    }

    public ImmutableList<EntityRecord> recordsBeforeCommit() {
        return ImmutableList.copyOf(recordsBeforeCommit);
    }

    public ImmutableList<EntityRecordChange> recordsAfterCommit() {
        return ImmutableList.copyOf(recordsAfterCommit);
    }

    public Error lastError() {
        return lastError;
    }

    public EntityRecord lastErroredRecord() {
        return lastErroredRecord;
    }

    public Event getLastRejection() {
        return lastRejection;
    }

    public EntityRecord getLastRejectedRecord() {
        return lastRejectedRecord;
    }
}
