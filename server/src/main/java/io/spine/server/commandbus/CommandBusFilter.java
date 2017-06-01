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

package io.spine.server.commandbus;

import io.grpc.stub.StreamObserver;
import io.spine.annotation.SPI;
import io.spine.base.Command;
import io.spine.base.Response;
import io.spine.envelope.CommandEnvelope;
import io.spine.server.commandbus.CommandBus;

/**
 * A {@code CommandBus} can have several filters that can prevent a command to be
 * {@linkplain CommandBus#post(Command, StreamObserver) posted}.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public interface CommandBusFilter {

    /**
     * Accepts or rejects a passed command.
     *
     * <p>A filter can:
     * <ul>
     *     <li>Accept the command (by returning {@code true}).
     *     <li>Reject the command and {@linkplain StreamObserver#onError(Throwable) notify} the
     *         response observer with an error. An example of this case would be an invalid command.
     *     <li>Reject a command but {@linkplain StreamObserver#onCompleted()} acknowledge it to the
     *         response observer. For example, a scheduled command would not pass a filter, but
     *         is acknowledged to the observer.
     * </ul>
     *
     * @param envelope         the envelope with the command to filter
     * @param responseObserver the observer to be {@linkplain StreamObserver#onError(Throwable)
     *                         notified} about the error which caused not accepting the passed
     *                         command
     *
     * @return {@code true} if the command passes the filter, {@code false} otherwise
     */
    boolean accept(CommandEnvelope envelope, StreamObserver<Response> responseObserver);

    void onClose(CommandBus commandBus);
}
