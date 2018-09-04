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
package io.spine.client;

import com.google.common.reflect.TypeToken;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.ActorContext;
import io.spine.core.UserId;
import io.spine.test.client.TestEntity;
import io.spine.time.ZoneId;
import io.spine.time.ZoneIds;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.base.Identifier.newUuid;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.core.given.GivenUserId.of;
import static io.spine.time.Timestamps2.isLaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base tests for the {@linkplain ActorRequestFactory} descendants.
 *
 * @author Alex Tymchenko
 */
@SuppressWarnings("unused") /* We're suppressing this warning since IDEA does not recognize
    nested JUnit classes in an abstract test base class. They are used via reflection by JUnit. */
@DisplayName("Actor request factory should")
abstract class ActorRequestFactoryTest {

    private final UserId actor = of(newUuid());
    private final ZoneOffset zoneOffset = ZoneOffsets.getDefault();
    private final ZoneId zoneId = ZoneIds.systemDefault();

    private ActorRequestFactory factory;

    protected ActorRequestFactory.Builder builder() {
        return ActorRequestFactory.newBuilder();
    }

    protected UserId actor() {
        return actor;
    }

    ZoneOffset zoneOffset() {
        return zoneOffset;
    }

    protected ActorRequestFactory factory() {
        return factory;
    }

    private ActorContext actorContext() {
        return factory().newActorContext();
    }

    @BeforeEach
    void createFactory() {
        factory = builder().setZoneOffset(zoneOffset)
                           .setZoneId(zoneId)
                           .setActor(actor)
                           .build();
    }

    @SuppressWarnings({"SerializableNonStaticInnerClassWithoutSerialVersionUID",
                       "SerializableInnerClassWithNonSerializableOuterClass"})
    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(Message.class, TestEntity.getDefaultInstance())
                .setDefault((new TypeToken<Class<? extends Message>>() {
                            }).getRawType(),
                            TestEntity.class)
                .setDefault((new TypeToken<Set<? extends Message>>() {
                            }).getRawType(),
                            newHashSet(Any.getDefaultInstance()))
                .setDefault(ZoneId.class, ZoneIds.systemDefault())
                .setDefault(ZoneOffset.class, ZoneOffsets.getDefault())
                .testInstanceMethods(factory(), NullPointerTester.Visibility.PUBLIC);
    }

    @Test
    @DisplayName("require actor in Builder")
    void requireActorInBuilder() {
        assertThrows(NullPointerException.class,
                     () -> builder().setZoneOffset(zoneOffset)
                                    .build()
        );
    }

    @Test
    @DisplayName("return values set in Builder")
    void returnValuesSetInBuilder() {
        ActorRequestFactory.Builder builder = builder()
                .setActor(actor)
                .setZoneOffset(zoneOffset)
                .setZoneId(zoneId);

        assertEquals(actor, builder.getActor());
        assertEquals(zoneOffset, builder.getZoneOffset());
        assertEquals(zoneId, builder.getZoneId());
    }

    @Test
    @DisplayName("be single tenant by default")
    void beSingleTenant() {
        assertNull(factory().getTenantId());
    }

    @Nested
    @DisplayName("when created, store")
    class Store {

        @Test
        @DisplayName("given user")
        void givenUser() {
            int currentOffset = ZoneOffsets.getDefault()
                                           .getAmountSeconds();
            ActorRequestFactory aFactory = builder()
                    .setActor(actor)
                    .build();

            assertEquals(actor, aFactory.getActor());
            assertEquals(currentOffset, aFactory.getZoneOffset()
                                                .getAmountSeconds());
        }

        @Test
        @DisplayName("given user and timezone")
        void givenUserAndTimezone() {
            ActorRequestFactory factory = factory();
            assertEquals(actor, factory.getActor());
            assertEquals(zoneOffset, factory.getZoneOffset());
            assertEquals(zoneId, factory.getZoneId());
        }
    }

    @Nested
    @DisplayName("Support moving between timezones")
    class TimeZoneMove {

        @Test
        @DisplayName("by ZoneOffset and ZoneId")
        void byOffsetAndId() {
            java.time.ZoneId id = java.time.ZoneId.of("Australia/Darwin");
            java.time.ZoneOffset offset = java.time.OffsetTime.now(id)
                                                              .getOffset();

            ZoneOffset zoneOffset = ZoneOffsets.of(offset);
            ZoneId zoneId = ZoneIds.of(id);

            ActorRequestFactory movedFactory = factory().switchTimeZone(zoneOffset, zoneId);

            assertNotEquals(factory().getZoneOffset(), movedFactory.getZoneOffset());
            assertEquals(zoneOffset, movedFactory.getZoneOffset());

            assertNotEquals(factory().getZoneId(), movedFactory.getZoneId());
            assertEquals(zoneId, movedFactory.getZoneId());
        }

        @Test
        @DisplayName("by ZoneId")
        void byZoneId() {
            java.time.ZoneId id = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

            ZoneId zoneId = ZoneIds.of(id);
            ActorRequestFactory movedFactory = factory().switchTimeZone(zoneId);

            assertNotEquals(factory().getZoneOffset(), movedFactory.getZoneOffset());
            assertNotEquals(factory().getZoneId(), movedFactory.getZoneId());
            assertEquals(zoneId, movedFactory.getZoneId());
        }
    }

    void verifyContext(ActorContext actual) {
        ActorContext expected = actorContext();

        assertEquals(expected.getTenantId(), actual.getTenantId());
        assertEquals(expected.getActor(), actual.getActor());
        assertEquals(expected.getLanguage(), actual.getLanguage());
        assertEquals(expected.getZoneOffset(), actual.getZoneOffset());
        assertEquals(expected.getZoneId(), actual.getZoneId());

        // It's impossible to get the same creation time for the `expected` value,
        //    so checking that the `actual` value is not later than `expected`.
        assertTrue(actual.getTimestamp()
                         .equals(expected.getTimestamp())
                           ||
                           isLaterThan(expected.getTimestamp(),
                                       actual.getTimestamp()));
    }
}
