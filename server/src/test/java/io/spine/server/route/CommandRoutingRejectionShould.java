/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandStatus;
import io.spine.grpc.StreamObservers;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.ProcessingStatus;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.rout.given.switchman.SwitchId;
import io.spine.server.route.given.switchman.Switchman;
import io.spine.server.route.given.switchman.SwitchmanBureau;
import io.spine.server.route.given.switchman.command.SetSwitch;
import io.spine.server.route.given.switchman.rejection.Rejections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.spine.Identifier.newUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandRoutingRejectionShould {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());
    private final StreamObserver<Ack> observer = StreamObservers.noOpObserver();

    private BoundedContext boundedContext;
    private CommandBus commandBus;
    private CommandStore commandStore;

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .setName("Railway Station")
                                       .setMultitenant(false)
                                       .build();
        boundedContext.register(new SwitchmanBureau());
        commandBus = boundedContext.getCommandBus();
        commandStore = commandBus.commandStore();
    }

    @After
    public void tearDown() throws Exception {
        boundedContext.close();
    }

    @Test
    public void result_in_rejected_command() {
        // Post a successful command to make sure general case works.
        final Command command =
                requestFactory.createCommand(SetSwitch.newBuilder()
                                                      .setSwitchId(generateSwitchId())
                                                      .setSwitchmanName(Switchman.class.getName())
                                                      .build());
        commandBus.post(command, observer);
        assertEquals(CommandStatus.OK, commandStore.getStatus(command).getCode());

        // Post a command with the argument which causes rejection in routing.
        final Command commandToReject =
                requestFactory.createCommand(SetSwitch.newBuilder()
                .setSwitchId(generateSwitchId())
                .setSwitchmanName(SwitchmanBureau.MISSING_SWITCHMAN_NAME)
                .build());

        commandBus.post(commandToReject, observer);
        final ProcessingStatus status = commandStore.getStatus(commandToReject);

        assertEquals(CommandStatus.REJECTED, status.getCode());
        final Message rejectionMessage = AnyPacker.unpack(status.getRejection()
                                                                .getMessage());
        assertTrue(rejectionMessage instanceof Rejections.SwitchmanUnavailable);
    }

    private static SwitchId generateSwitchId() {
        return SwitchId.newBuilder()
                       .setId(newUuid())
                       .build();
    }
}
