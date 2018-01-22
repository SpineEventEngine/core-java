/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.core.React;
import io.spine.core.RejectionContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.rout.given.switchman.LogState;
import io.spine.server.rout.given.switchman.LogStateVBuilder;
import io.spine.server.route.EventRoute;
import io.spine.server.route.RejectionRoute;
import io.spine.server.route.given.switchman.event.SwitchPositionConfirmed;
import io.spine.server.route.given.switchman.event.SwitchWorkRecorded;
import io.spine.server.route.given.switchman.event.SwitchmanAbsenceRecorded;
import io.spine.server.route.given.switchman.rejection.Rejections;
import io.spine.time.Time;

import java.util.Set;

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
        final String switchmanName = event.getSwitchmanName();
        final Integer currentCount = getState().getCountersMap()
                                               .get(switchmanName);
        getBuilder().putCounters(switchmanName,
                                 currentCount == null ? 1 : currentCount + 1);
    }

    /**
     * The repository with default routing functions that route to the singleton aggregate.
     */
    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    public static final class Repository extends AggregateRepository<Long, Log> {

        private static final Set<Long> SINGLETON_ID_SET = ImmutableSet.of(ID);

        public Repository() {
            super();
            getEventRouting().replaceDefault(new EventRoute<Long, Message>() {
                private static final long serialVersionUID = 0L;

                @Override
                public Set<Long> apply(Message message, EventContext context) {
                    return SINGLETON_ID_SET;
                }
            });
            getRejectionRouting().replaceDefault(new RejectionRoute<Long, Message>() {
                private static final long serialVersionUID = 0L;

                @Override
                public Set<Long> apply(Message message, RejectionContext context) {
                    return SINGLETON_ID_SET;
                }
            });
        }
    }
}
