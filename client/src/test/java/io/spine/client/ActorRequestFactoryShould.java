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
package io.spine.client;

import com.google.common.reflect.TypeToken;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.client.ActorRequestFactory;
import org.junit.Test;
import io.spine.base.ActorContext;
import io.spine.test.Tests;
import io.spine.test.client.TestEntity;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;
import io.spine.users.UserId;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static io.spine.base.Identifiers.newUuid;
import static io.spine.test.Tests.newUserId;
import static io.spine.time.Timestamps2.isLaterThan;

/**
 * Base tests for the {@linkplain ActorRequestFactory} descendants.
 *
 * @author Alex Tymchenko
 */
public abstract class ActorRequestFactoryShould {

    private final UserId actor = newUserId(newUuid());
    private final ZoneOffset zoneOffset = ZoneOffsets.UTC;

    protected ActorRequestFactory.Builder builder() {
        return ActorRequestFactory.newBuilder();
    }

    protected UserId getActor() {
        return actor;
    }

    protected ZoneOffset getZoneOffset() {
        return zoneOffset;
    }

    protected ActorRequestFactory factory() {
        return builder().setZoneOffset(zoneOffset)
                        .setActor(actor)
                        .build();
    }

    ActorContext actorContext() {
        return factory().actorContext();
    }

    @Test(expected = NullPointerException.class)
    public void require_actor_in_Builder() {
        builder().setZoneOffset(zoneOffset)
                 .build();
    }

    @Test
    public void return_set_values_in_Builder() {
        final ActorRequestFactory.Builder builder = builder()
                .setActor(actor)
                .setZoneOffset(zoneOffset);
        assertNotNull(builder.getActor());
        assertNotNull(builder.getZoneOffset());
        assertNull(builder.getTenantId());
    }

    @Test
    public void create_instance_by_user() {
        final int currentOffset = ZoneOffsets.getDefault()
                                             .getAmountSeconds();
        final ActorRequestFactory aFactory = builder()
                .setActor(actor)
                .build();

        assertEquals(actor, aFactory.getActor());
        assertEquals(currentOffset, aFactory.getZoneOffset()
                                            .getAmountSeconds());
    }

    @Test
    public void create_instance_by_user_and_timezone() {
        assertEquals(actor, factory().getActor());
        assertEquals(zoneOffset, factory().getZoneOffset());
    }

    @Test
    public void be_single_tenant_by_default() {
        assertNull(factory().getTenantId());
    }

    @Test
    public void support_moving_between_timezones() {
        final ActorRequestFactory factoryInAnotherTimezone =
                factory().switchTimezone(ZoneOffsets.ofHours(-8));
        assertNotEquals(factory().getZoneOffset(), factoryInAnotherTimezone.getZoneOffset());
    }

    @SuppressWarnings({"SerializableNonStaticInnerClassWithoutSerialVersionUID",
            "SerializableInnerClassWithNonSerializableOuterClass"})
    @Test
    public void not_accept_nulls_as_public_method_arguments() {
        new NullPointerTester()
                .setDefault(Message.class, TestEntity.getDefaultInstance())
                .setDefault((new TypeToken<Class<? extends Message>>() {
                            }).getRawType(),
                            TestEntity.class)
                .setDefault((new TypeToken<Set<? extends Message>>() {
                            }).getRawType(),
                            newHashSet(Tests.newUuidValue()))
                .testInstanceMethods(factory(), NullPointerTester.Visibility.PUBLIC);
    }

    void verifyContext(ActorContext actualContext) {
        final ActorContext expectedContext = actorContext();

        assertEquals(expectedContext.getTenantId(), actualContext.getTenantId());
        assertEquals(expectedContext.getActor(), actualContext.getActor());
        assertEquals(expectedContext.getLanguage(), actualContext.getLanguage());
        assertEquals(expectedContext.getZoneOffset(), actualContext.getZoneOffset());

        // It's impossible to get the same creation time for the `expected` value,
        //    so checking that the `actual` value is not later than `expected`.
        assertTrue(actualContext.getTimestamp()
                                .equals(expectedContext.getTimestamp())
                           ||
                           isLaterThan(expectedContext.getTimestamp(),
                                       actualContext.getTimestamp()));
    }
}
