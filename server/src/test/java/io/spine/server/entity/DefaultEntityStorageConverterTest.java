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

package io.spine.server.entity;

import com.google.common.testing.SerializableTester;
import com.google.protobuf.FieldMask;
import com.google.protobuf.StringValue;
import io.spine.server.BoundedContext;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.entity.DefaultEntityStorageConverter.forAllFields;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("DefaultEntityStorageConverter should")
class DefaultEntityStorageConverterTest {

    private EntityStorageConverter<Long, TestEntity, StringValue> converter;

    @BeforeEach
    void setUp() {
        BoundedContext bc = BoundedContext.newBuilder()
                                          .build();
        RecordBasedRepository<Long, TestEntity, StringValue> repository = new TestRepository();
        bc.register(repository);

        TypeUrl stateType = repository.entityClass()
                                      .getStateType();
        converter = forAllFields(stateType, repository.entityFactory());
    }

    @Test
    @DisplayName("create instance for all fields")
    void createForAllFields() throws Exception {
        assertEquals(FieldMask.getDefaultInstance(), converter.getFieldMask());
    }

    @Test
    @DisplayName("create instance with FieldMask")
    void createWithFieldMask() throws Exception {
        FieldMask fieldMask = FieldMask.newBuilder()
                                       .addPaths("foo.bar")
                                       .build();

        EntityStorageConverter<Long, TestEntity, StringValue> withMasks =
                converter.withFieldMask(fieldMask);

        assertEquals(fieldMask, withMasks.getFieldMask());
    }

    private static TestEntity createEntity(Long id, StringValue state) {
        TestEntity result = new TestEntity(id);
        result.setState(state);
        return result;
    }

    @Test
    @DisplayName("convert forward and backward")
    void convertForwardAndBackward() throws Exception {
        StringValue entityState = StringValue.of("back and forth");
        TestEntity entity = createEntity(100L, entityState);

        EntityRecord out = converter.convert(entity);
        TestEntity back = converter.reverse()
                                   .convert(out);
        assertEquals(entity, back);
    }

    @Test
    @DisplayName("be serializable")
    void beSerializable() {
        SerializableTester.reserializeAndAssert(converter);
    }

    /**
     * A test entity class which is not versionable.
     */
    private static class TestEntity extends AbstractEntity<Long, StringValue> {
        private TestEntity(Long id) {
            super(id);
        }
    }

    /**
     * A test repository.
     */
    private static class TestRepository
            extends DefaultRecordBasedRepository<Long, TestEntity, StringValue> {
    }
}
