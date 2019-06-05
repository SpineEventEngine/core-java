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

import io.grpc.stub.StreamObserver;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.client.CommandFactory;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.MessageId;
import io.spine.core.MessageWithContext;
import io.spine.core.Status;
import io.spine.core.Status.StatusCase;
import io.spine.core.UserId;
import io.spine.logging.Logging;
import io.spine.type.TypeName;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * The default implementation of {@link SystemWriteSide}.
 */
final class DefaultSystemWriteSide implements SystemWriteSide {

    /**
     * The ID of the user which is used for generating system commands and events.
     */
    static final UserId SYSTEM_USER = UserId
            .newBuilder()
            .setValue("SYSTEM")
            .build();

    private final SystemContext system;

    DefaultSystemWriteSide(SystemContext system) {
        this.system = system;
    }

    @Override
    public void postCommand(CommandMessage systemCommand) {
        checkNotNull(systemCommand);
        CommandFactory commandFactory = SystemCommandFactory.newInstance(system.isMultitenant());
        Command command = commandFactory.create(systemCommand);
        system.commandBus()
              .post(command, LoggingObserver.ofResultsOf(command));
    }

    @Override
    public void postEvent(EventMessage systemEvent) {
        checkNotNull(systemEvent);
        SystemEventFactory factory = SystemEventFactory.forMessage(systemEvent,
                                                                   system.isMultitenant());
        Event event = factory.createEvent(systemEvent, null);
        system.importBus()
              .post(event, LoggingObserver.ofResultsOf(event));
    }

    private static final class LoggingObserver implements StreamObserver<Ack> {

        private final TypeName messageType;
        private final MessageId messageId;
        private final Logger logger;

        private LoggingObserver(TypeName type, MessageId id, Logger logger) {
            this.messageType = checkNotNull(type);
            this.messageId = checkNotNull(id);
            this.logger = checkNotNull(logger);
        }

        private static LoggingObserver ofResultsOf(MessageWithContext<?, ?, ?> message) {
            MessageId id = message.id();
            TypeName name = message.typeUrl()
                                   .toTypeName();
            return new LoggingObserver(name, id, Logging.get(SystemWriteSide.class));
        }

        @SuppressWarnings("EnumSwitchStatementWhichMissesCases")
            // Intentionally covered by the "default" branch.
        @Override
        public void onNext(Ack ack) {
            Status status = ack.getStatus();
            StatusCase statusCase = status.getStatusCase();
            switch (statusCase) {
                case OK:
                    logger.debug("Message {} acknowledged.", messageInfo());
                    break;
                case ERROR:
                    logger.error("Message {} handled with an error:{}{}",
                                 messageInfo(),
                                 System.lineSeparator(),
                                 status.getError());
                    break;
                case REJECTION:
                    logger.warn("Message {} rejected:{}{}",
                                messageInfo(),
                                System.lineSeparator(),
                                status.getRejection().enclosedMessage());
                    break;
                default:
                    logger.error("Message {} handled with an unexpected status:{}{}",
                                 messageInfo(),
                                 System.lineSeparator(),
                                 ack);
            }
        }

        @Override
        public void onError(Throwable t) {
            logger.error(format("Error while posting a system message %s.", messageInfo()), t);
        }

        @Override
        public void onCompleted() {
            logger.debug("System message {} posted successfully.", messageInfo());
        }

        private String messageInfo() {
            return format("%s[%s: %s]",
                          messageType,
                          messageId.getClass().getSimpleName(),
                          messageId.value());
        }
    }
}
