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

package io.spine.server.entity;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.StringValue;
import io.spine.protobuf.AnyPacker;
import io.spine.system.server.EntityHistoryId;
import io.spine.testing.UtilityClassTest;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("EntityHistoryIds utility should")
class EntityHistoryIdsTest extends UtilityClassTest<EntityHistoryIds> {

    private static final TypeUrl TYPE_URL = TypeUrl.of(StringValue.class);

    private EntityHistoryIdsTest() {
        super(EntityHistoryIds.class);
    }

    @Override
    protected void configure(NullPointerTester tester) {
        super.configure(tester);
        tester.setDefault(TypeUrl.class, TYPE_URL)
              .setDefault(EntityHistoryId.class, EntityHistoryId.getDefaultInstance());
    }

    @DisplayName("wrap an entity ID")
    @Test
    void wrapEntityId() {
        StringValue entityId = StringValue.of("id");
        EntityHistoryId historyId = EntityHistoryIds.wrap(entityId, TYPE_URL);
        assertEquals(TYPE_URL.value(), historyId.getTypeUrl());
        assertEquals(entityId, AnyPacker.unpack(historyId.getEntityId()
                                                         .getId()));
    }

    @DisplayName("unwrap an entity ID")
    @Test
    void unwrapEntityId() {
        String entityId = "id";
        EntityHistoryId historyId = EntityHistoryIds.wrap(entityId, TYPE_URL);
        String entityIdFromHistory = EntityHistoryIds.unwrap(historyId, entityId.getClass());
        assertEquals(entityId, entityIdFromHistory);
    }
}
