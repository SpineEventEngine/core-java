/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.commandbus;

import io.spine.core.Ack;
import io.spine.core.Subscribe;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.bus.BusFilter;
import io.spine.server.command.Assign;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.commandbus.CmdBusTeaOrder;
import io.spine.test.commandbus.CmdBusTeaOrderId;
import io.spine.test.commandbus.CmdBusTeaOrderView;
import io.spine.test.commandbus.command.CmdBusServeTea;
import io.spine.test.commandbus.command.CmdBusTeaOrderDenied;
import io.spine.test.commandbus.command.TeaHouseRejections;
import io.spine.test.commandbus.event.CmdBusTeaOrderCreated;
import io.spine.testing.server.blackbox.BlackBoxContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.test.commandbus.CmdBusTeaType.BLACK;
import static io.spine.test.commandbus.CmdBusTeaType.GREEN;

@SuppressWarnings("unused") // Reflective access.
@DisplayName("Command bus, when a `Rejection` is thrown from a filter, should")
class RejectionInFilterTest {

    @Test
    @DisplayName("post this rejection to `EventBus`")
    void postRejection() {
        BlackBoxContext context = BlackBoxContext.from(
                BoundedContextBuilder.assumingTests()
                                     .add(OrderAggregate.class)
                                     .add(new OrderProjectionRepository())
                                     .addCommandFilter(new TeaFilter())
        );
        CmdBusTeaOrderId idAllowed = CmdBusTeaOrderId.generate();
        CmdBusTeaOrderId idDenied = CmdBusTeaOrderId.generate();
        context.receivesCommand(
                CmdBusServeTea
                        .newBuilder()
                        .setId(idAllowed)
                        .setType(BLACK)
                        .setVerifiedUser(true)
                        .build())
               .receivesCommand(
                       CmdBusServeTea
                               .newBuilder()
                               .setId(idDenied)
                               .setType(GREEN)
                               .setVerifiedUser(false)
                               .build()
               );
        context.assertEntity(idAllowed, OrderProjection.class)
               .hasStateThat()
               .comparingExpectedFieldsOnly()
               .isEqualTo(
                       CmdBusTeaOrderView
                               .newBuilder()
                               .setType(BLACK)
                               .setDenied(false)
                               .build()
               );
        context.assertEntity(idDenied, OrderProjection.class)
               .hasStateThat()
               .comparingExpectedFieldsOnly()
               .isEqualTo(
                       CmdBusTeaOrderView
                               .newBuilder()
                               .setDenied(true)
                               .build()
               );
    }

    private static class OrderAggregate
            extends Aggregate<CmdBusTeaOrderId, CmdBusTeaOrder, CmdBusTeaOrder.Builder> {

        @Assign
        CmdBusTeaOrderCreated handle(CmdBusServeTea cmd) {
            return CmdBusTeaOrderCreated
                    .newBuilder()
                    .setId(cmd.getId())
                    .setType(cmd.getType())
                    .build();
        }

        @Apply
        private void on(CmdBusTeaOrderCreated event) {
            builder().setTeaType(event.getType());
        }
    }

    private static class OrderProjection
            extends Projection<CmdBusTeaOrderId, CmdBusTeaOrderView, CmdBusTeaOrderView.Builder> {

        @Subscribe
        void on(CmdBusTeaOrderCreated event) {
            builder().setType(event.getType());
        }

        @Subscribe
        void on(TeaHouseRejections.CmdBusTeaOrderDenied rejection) {
            builder().setDenied(true);
        }
    }

    private static class OrderProjectionRepository
            extends ProjectionRepository<CmdBusTeaOrderId, OrderProjection, CmdBusTeaOrderView> {

        @Override
        protected void setupEventRouting(EventRouting<CmdBusTeaOrderId> routing) {
            super.setupEventRouting(routing);
            routing.route(TeaHouseRejections.CmdBusTeaOrderDenied.class,
                          EventRoute.byFirstMessageField(idClass()));
        }
    }

    private static class TeaFilter implements BusFilter<CommandEnvelope> {

        @Override
        public Optional<Ack> doFilter(CommandEnvelope envelope) {
            CmdBusServeTea command =
                    unpack(envelope.command()
                                   .getMessage(), CmdBusServeTea.class);
            boolean verifiedUser = command.getVerifiedUser();
            if (verifiedUser) {
                return accept();
            }
            CmdBusTeaOrderDenied rejection = CmdBusTeaOrderDenied
                    .newBuilder()
                    .setId(command.getId())
                    .build();
            RejectionEnvelope cause = RejectionEnvelope.from(envelope, rejection);
            return reject(envelope, cause);
        }
    }
}
