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

package io.spine.server.event;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.server.command.TestEventFactory;
import io.spine.test.event.ProjectCreated;
import io.spine.testdata.Sample;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.MessageValidator;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
public class EventValidatorShould {

    private static final TestEventFactory eventFactory =
            TestEventFactory.newInstance(EventValidatorShould.class);

    @Test
    public void validate_event_messages() {
        final MessageValidator messageValidator = mock(MessageValidator.class);
        when(messageValidator.validate(any(Message.class)))
                .thenReturn(newArrayList(ConstraintViolation.getDefaultInstance(),
                                         ConstraintViolation.getDefaultInstance()));
        final Event event = eventFactory.createEvent(Sample.messageOfType(ProjectCreated.class));

        final EventValidator eventValidator = new EventValidator(messageValidator);

        final Optional<Error> error = eventValidator.validate(EventEnvelope.of(event));
        assertTrue(error.isPresent());
        final Error actualError = error.get();
        assertEquals(InvalidEventException.class.getCanonicalName(),
                     actualError.getType());
    }
}
