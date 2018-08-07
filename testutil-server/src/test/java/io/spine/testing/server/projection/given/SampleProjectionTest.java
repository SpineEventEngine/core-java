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

package io.spine.testing.server.projection.given;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.server.entity.Repository;
import io.spine.testing.server.expected.EventSubscriberExpected;
import io.spine.testing.server.projection.ProjectionTest;
import io.spine.testing.server.projection.given.prj.TuProjection;
import io.spine.testing.server.projection.given.prj.TuProjectionRepository;
import org.junit.jupiter.api.BeforeEach;

/**
 * The test class for the {@code StringValue} event handler in {@code TestProjection}.
 */
public class SampleProjectionTest
        extends ProjectionTest<Long, StringValue, StringValue, TuProjection> {

    public static final StringValue TEST_EVENT = StringValue.newBuilder()
                                                            .setValue("test projection event")
                                                            .build();

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
    }

    @Override
    protected Long newId() {
        return TuProjection.ID;
    }

    @Override
    protected StringValue createMessage() {
        return TEST_EVENT;
    }

    @Override
    public EventSubscriberExpected<StringValue> expectThat(TuProjection entity) {
        return super.expectThat(entity);
    }

    @Override
    protected Repository<Long, TuProjection> createEntityRepository() {
        return new TuProjectionRepository();
    }

    public Message storedMessage() {
        return message();
    }

    /**
     * Exposes internal configuration method.
     */
    public void init() {
        configureBoundedContext();
    }
}
