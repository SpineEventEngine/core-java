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

package io.spine.server.route;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandStatus;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.ProcessingStatus;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.rout.given.switchman.LogState;
import io.spine.server.rout.given.switchman.SwitchId;
import io.spine.server.rout.given.switchman.SwitchPosition;
import io.spine.server.route.given.switchman.Log;
import io.spine.server.route.given.switchman.Switchman;
import io.spine.server.route.given.switchman.SwitchmanBureau;
import io.spine.server.route.given.switchman.command.SetSwitch;
import io.spine.server.route.given.switchman.rejection.Rejections;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.base.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that a command can be rejected within a {@linkplain CommandRoute routing function}.
 *
 * @author Alexander Yevsyukov
 */
@DisplayName("CommandRouting rejection should")
class CommandRoutingRejectionTest {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());
    private final StreamObserver<Ack> observer = noOpObserver();

    private BoundedContext boundedContext;
    private CommandBus commandBus;
    private CommandStore commandStore;
    private Log.Repository logRepository;

    @BeforeEach
    void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .setName("Railway Station")
                                       .setMultitenant(false)
                                       .build();
        boundedContext.register(new SwitchmanBureau());
        logRepository = new Log.Repository();
        boundedContext.register(logRepository);
        commandBus = boundedContext.getCommandBus();
        commandStore = commandBus.commandStore();
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
        commandBus.post(command, observer);
        assertEquals(CommandStatus.OK, commandStore.getStatus(command).getCode());

        // Post a command with the argument which causes rejection in routing.
        Command commandToReject = requestFactory.createCommand(
                SetSwitch.newBuilder()
                         .setSwitchmanName(SwitchmanBureau.MISSING_SWITCHMAN_NAME)
                         .setSwitchId(generateSwitchId())
                         .setPosition(SwitchPosition.LEFT)
                         .build()
        );

        commandBus.post(commandToReject, observer);
        ProcessingStatus status = commandStore.getStatus(commandToReject);

        // Check that the command is rejected.
        assertEquals(CommandStatus.REJECTED, status.getCode());
        Message rejectionMessage = AnyPacker.unpack(status.getRejection()
                                                                .getMessage());
        assertTrue(rejectionMessage instanceof Rejections.SwitchmanUnavailable);

        // Check that the event and the rejection were dispatched.
        Optional<Log> optional = logRepository.find(Log.ID);
        assertTrue(optional.isPresent());
        LogState log = optional.get()
                                     .getState();
        assertTrue(log.containsCounters(switchmanName));
        assertTrue(log.getMissingSwitchmanList()
                      .contains(SwitchmanBureau.MISSING_SWITCHMAN_NAME));
    }

    private static SwitchId generateSwitchId() {
        return SwitchId.newBuilder()
                       .setId(newUuid())
                       .build();
    }
}
