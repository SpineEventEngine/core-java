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

package io.spine.server.route.given.switchman;

import com.google.common.collect.ImmutableSet;
import io.spine.base.Time;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.event.React;
import io.spine.server.rout.given.switchman.LogState;
import io.spine.server.rout.given.switchman.LogStateVBuilder;
import io.spine.server.route.given.switchman.event.SwitchPositionConfirmed;
import io.spine.server.route.given.switchman.event.SwitchWorkRecorded;
import io.spine.server.route.given.switchman.event.SwitchmanAbsenceRecorded;
import io.spine.server.route.given.switchman.rejection.Rejections;

import static com.google.common.collect.ImmutableSet.of;

/**
 * The aggregate that accumulates information about switchman work and absence.
 *
 * <p>There's only one log per system.
 *
 * @author Alexander Yevsyukov
 */
public final class Log extends Aggregate<Long, LogState, LogStateVBuilder> {

    /** The ID of the singleton log. */
    public static final long ID = 42L;

    private Log(Long id) {
        super(id);
    }

    @React
    SwitchmanAbsenceRecorded on(Rejections.SwitchmanUnavailable rejection) {
        return SwitchmanAbsenceRecorded.newBuilder()
                                       .setSwitchmanName(rejection.getSwitchmanName())
                                       .setTimestamp(Time.getCurrentTime())
                                       .build();
    }

    @Apply
    void event(SwitchmanAbsenceRecorded event) {
        getBuilder().addMissingSwitchman(event.getSwitchmanName());
    }

    @React
    SwitchWorkRecorded on(SwitchPositionConfirmed event) {
        return SwitchWorkRecorded.newBuilder()
                                 .setSwitchId(event.getSwitchId())
                                 .setSwitchmanName(event.getSwitchmanName())
                                 .build();
    }

    @Apply
    void event(SwitchWorkRecorded event) {
        String switchmanName = event.getSwitchmanName();
        Integer currentCount = getState().getCountersMap()
                                         .get(switchmanName);
        getBuilder().putCounters(switchmanName,
                                 currentCount == null ? 1 : currentCount + 1);
    }

    /**
     * The repository with default routing functions that route to the singleton aggregate.
     */
    @SuppressWarnings({
            "SerializableInnerClassWithNonSerializableOuterClass",
            "AssignmentOrReturnOfFieldWithMutableType" // Returning immutable impl.
    })
    public static final class Repository extends AggregateRepository<Long, Log> {

        private static final ImmutableSet<Long> SINGLETON_ID_SET = of(ID);

        public Repository() {
            super();
        }

        @Override
        public void onRegistered() {
            super.onRegistered();
            getEventRouting().replaceDefault((message, context) -> SINGLETON_ID_SET);
        }
    }
}
