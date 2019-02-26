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

package io.spine.server.route;

import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Subscribe;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.route.given.switchman.Log;
import io.spine.server.route.given.switchman.LogState;
import io.spine.server.route.given.switchman.SwitchId;
import io.spine.server.route.given.switchman.SwitchPosition;
import io.spine.server.route.given.switchman.Switchman;
import io.spine.server.route.given.switchman.SwitchmanBureau;
import io.spine.server.route.given.switchman.command.SetSwitch;
import io.spine.server.route.given.switchman.event.SwitchPositionConfirmed;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.base.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that a command can be rejected within a {@linkplain CommandRoute routing function}.
 */
@DisplayName("CommandRouting rejection should")
class CommandRoutingRejectionTest {

    private final TestActorRequestFactory requestFactory = new TestActorRequestFactory(getClass());
    private final StreamObserver<Ack> observer = noOpObserver();

    private BoundedContext boundedContext;
    private CommandBus commandBus;
    private Log.Repository logRepository;
    private SwitchmanObserver switchmanObserver;

    @BeforeEach
    void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .setName("Railway Station")
                                       .setMultitenant(false)
                                       .build();
        boundedContext.register(new SwitchmanBureau());
        switchmanObserver = new SwitchmanObserver();
        boundedContext.eventBus()
                      .register(switchmanObserver);
        logRepository = new Log.Repository();
        boundedContext.register(logRepository);
        commandBus = boundedContext.commandBus();
    }

    @AfterEach
    void tearDown() throws Exception {
        boundedContext.close();
    }

    /**
     * Verifies that a command rejected during routing gets rejected status.
     *
     * <p>The test initially posts a command which should be processed correctly. This is done to
     * ensure that basic processing works.
     *
     * <p>Then the test posts a command with the argument that should cause rejection in the
     * routing function.
     */
    @Test
    @DisplayName("result in rejected command")
    void resultInRejectedCommand() {
        // Post a successful command to make sure general case works.
        String switchmanName = Switchman.class.getName();
        Command command = requestFactory.createCommand(
                SetSwitch.newBuilder()
                         .setSwitchId(generateSwitchId())
                         .setSwitchmanName(switchmanName)
                         .setPosition(SwitchPosition.RIGHT)
                         .build()
        );
        assertEquals(0, switchmanObserver.events.size());
        commandBus.post(command, observer);
        assertEquals(1, switchmanObserver.events.size());

        // Post a command with the argument which causes rejection in routing.
        Command commandToReject = requestFactory.createCommand(
                SetSwitch.newBuilder()
                         .setSwitchmanName(SwitchmanBureau.MISSING_SWITCHMAN_NAME)
                         .setSwitchId(generateSwitchId())
                         .setPosition(SwitchPosition.LEFT)
                         .build()
        );

        commandBus.post(commandToReject, observer);
        Optional<Log> foundLog = logRepository.find(Log.ID);
        assertTrue(foundLog.isPresent());
        LogState log = foundLog.get().state();
        assertTrue(log.containsCounters(switchmanName));
        assertTrue(log.getMissingSwitchmanList()
                      .contains(SwitchmanBureau.MISSING_SWITCHMAN_NAME));
    }

    private static SwitchId generateSwitchId() {
        return SwitchId.newBuilder()
                       .setId(newUuid())
                       .build();
    }

    private static class SwitchmanObserver extends AbstractEventSubscriber {

        private final List<SwitchPositionConfirmed> events = newLinkedList();

        @Subscribe
        public void to(SwitchPositionConfirmed event) {
            events.add(event);
        }
    }
}
