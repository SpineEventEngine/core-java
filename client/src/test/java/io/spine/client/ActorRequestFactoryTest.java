/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.test.client.TestEntity;
import io.spine.time.Temporals;
import io.spine.time.ZoneId;
import io.spine.time.ZoneIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.given.ActorRequestFactoryTestEnv.ACTOR;
import static io.spine.client.given.ActorRequestFactoryTestEnv.ZONE_ID;
import static io.spine.client.given.ActorRequestFactoryTestEnv.requestFactory;
import static io.spine.client.given.ActorRequestFactoryTestEnv.requestFactoryBuilder;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Base tests for the {@linkplain ActorRequestFactory} descendants.
 */
@DisplayName("`ActorRequestFactory` should")
class ActorRequestFactoryTest {

    private ActorRequestFactory factory;

    @BeforeEach
    void createFactory() {
        factory = requestFactory();
    }

    @SuppressWarnings({"SerializableNonStaticInnerClassWithoutSerialVersionUID",
                       "SerializableInnerClassWithNonSerializableOuterClass"}) // for TypeToken
    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        var tester = new NullPointerTester();
        tester.setDefault(Message.class, TestEntity.getDefaultInstance())
                .setDefault(new TypeToken<Class<? extends Message>>() {}.getRawType(),
                            TestEntity.class)
                .setDefault(new TypeToken<Set<? extends Message>>() {}.getRawType(),
                            newHashSet(Any.getDefaultInstance()))
                .setDefault(ZoneId.class, ZoneIds.systemDefault());
        setDeprecatedDefaultsToo(tester);
        tester.testInstanceMethods(factory, NullPointerTester.Visibility.PUBLIC);
    }

    @SuppressWarnings("deprecation")
    private static void setDeprecatedDefaultsToo(NullPointerTester tester) {
        tester.setDefault(
                io.spine.time.ZoneOffset.class,
                io.spine.time.ZoneOffset.getDefaultInstance()
        );
    }

    @Test
    @DisplayName("require actor in `Builder`")
    void requireActorInBuilder() {
        assertThrows(NullPointerException.class,
                     () -> requestFactoryBuilder()
                             .setZoneId(ZoneIds.systemDefault())
                             .build()
        );
    }

    @Test
    @DisplayName("return values set in `Builder`")
    void returnValuesSetInBuilder() {
        var builder = requestFactoryBuilder()
                .setActor(ACTOR)
                .setZoneId(ZONE_ID);

        assertEquals(ACTOR, builder.getActor());
        assertEquals(ZONE_ID, builder.getZoneId());
    }

    @Test
    @DisplayName("be single tenant by default")
    void beSingleTenant() {
        assertNull(factory.tenantId());
    }

    @Nested
    @DisplayName("when created, store")
    class Store {

        @Test
        @DisplayName("given user")
        void givenUser() {
            var aFactory = requestFactoryBuilder()
                    .setActor(ACTOR)
                    .build();

            assertEquals(ACTOR, aFactory.actor());
        }

        @Test
        @DisplayName("given user and timezone")
        void givenUserAndTimezone() {
            assertEquals(ACTOR, factory.actor());
            assertEquals(ZONE_ID, factory.zoneId());
        }
    }

    @Nested
    @DisplayName("Support moving between timezones")
    class TimeZoneMove {

        @Test
        @DisplayName("by `ZoneId`")
        void byOffsetAndId() {
            var id = java.time.ZoneId.of("Australia/Darwin");
            var newZone = ZoneIds.of(id);

            var movedFactory = factory.switchTimeZone(newZone);

            assertThat(movedFactory.zoneId())
                    .isNotEqualTo(factory.zoneId());
            assertThat(movedFactory.zoneId())
                    .isEqualTo(newZone);
        }

        @Test
        @DisplayName("by ZoneId")
        void byZoneId() {
            var id = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

            var zoneId = ZoneIds.of(id);
            var movedFactory = factory.switchTimeZone(zoneId);

            assertNotEquals(factory.zoneId(), movedFactory.zoneId());
            assertEquals(zoneId, movedFactory.zoneId());
        }
    }

    @Nested
    @DisplayName("obtain time-zone and offset from current `Time.Provider`")
    class UsingTimeProvider {

        private final Time.Provider provider = new CustomTimeProvider();

        @BeforeEach
        void setCustomProvider() {
            Time.setProvider(provider);
        }

        @AfterEach
        void clearProvider() {
            Time.resetProvider();
        }

        @Test
        @DisplayName("when no set directly")
        void zoneIdAndOffset() {
            var factory = ActorRequestFactory.newBuilder()
                    .setActor(ACTOR)
                    .build();

            var expected = ZoneIds.of(CustomTimeProvider.ZONE);
            assertThat(factory.zoneId())
                    .isEqualTo(expected);
        }
    }

    private static class CustomTimeProvider implements Time.Provider {

        private static final java.time.ZoneId ZONE = java.time.ZoneId.of("Pacific/Galapagos");

        @Override
        public Timestamp currentTime() {
            var nowThere = ZonedDateTime.now(ZONE).toInstant();
            return Temporals.from(nowThere)
                            .toTimestamp();
        }

        @Override
        public java.time.ZoneId currentZone() {
            return ZONE;
        }
    }
}
