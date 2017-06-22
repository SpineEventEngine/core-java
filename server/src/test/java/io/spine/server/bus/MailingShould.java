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

package io.spine.server.bus;

import com.google.common.testing.NullPointerTester;
import io.spine.base.Error;
import io.spine.base.Event;
import io.spine.base.Failure;
import io.spine.base.IsSent;
import io.spine.base.Status;
import io.spine.base.Status.StatusCase;
import io.spine.envelope.EventEnvelope;
import io.spine.test.TestEventFactory;
import io.spine.test.event.ProjectCreated;
import org.junit.Test;

import static io.spine.base.Status.StatusCase.OK;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.test.TestEventFactory.newInstance;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;

/**
 * @author Dmytro Dashenkov
 */
public class MailingShould {

    private static final TestEventFactory eventFactory = newInstance(MailingShould.class);

    private static final Error ERROR = Error.getDefaultInstance();

    private static final Failure FAILURE = Failure.getDefaultInstance();

    @Test
    public void have_private_util_ctor() {
        assertHasPrivateParameterlessCtor(Mailing.class);
    }

    @Test
    public void not_accept_nulls() {
        new NullPointerTester()
                .setDefault(MessageWithIdEnvelope.class,
                            envelope())
                .setDefault(Status.class, Status.getDefaultInstance())
                .testAllPublicStaticMethods(Mailing.class);
    }

    @Test
    public void acknowledge_envelope() {
        final MessageWithIdEnvelope envelope = envelope();
        final IsSent ack = Mailing.checkIn(envelope);
        assertEquals(envelope.getId(), unpack(ack.getMessageId()));
        assertEquals(OK, ack.getStatus()
                            .getStatusCase());
    }

    @Test
    public void check_in_error_envelope() {
        final MessageWithIdEnvelope envelope = envelope();
        final Status status = Status.newBuilder()
                                    .setError(ERROR)
                                    .build();
        final IsSent result = Mailing.checkIn(envelope, status);
        assertEquals(envelope.getId(), unpack(result.getMessageId()));
        assertEquals(StatusCase.ERROR, result.getStatus().getStatusCase());
        assertEquals(result.getStatus().getError(), ERROR);
    }

    @Test
    public void check_in_envelope_with_failure() {
        final MessageWithIdEnvelope envelope = envelope();
        final Status status = Status.newBuilder()
                                    .setFailure(FAILURE)
                                    .build();
        final IsSent result = Mailing.checkIn(envelope, status);
        assertEquals(envelope.getId(), unpack(result.getMessageId()));
        assertEquals(StatusCase.FAILURE, result.getStatus().getStatusCase());
        assertEquals(result.getStatus().getFailure(), FAILURE);
    }

    private static MessageWithIdEnvelope<?, ?> envelope() {
        final ProjectCreated msg = ProjectCreated.getDefaultInstance();
        final Event event = eventFactory.createEvent(msg);
        final EventEnvelope envelope = EventEnvelope.of(event);
        return envelope;
    }
}
