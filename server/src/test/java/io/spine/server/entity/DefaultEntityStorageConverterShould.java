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

package io.spine.server.entity;

import com.google.common.testing.SerializableTester;
import com.google.protobuf.FieldMask;
import com.google.protobuf.StringValue;
import io.spine.protobuf.Wrapper;
import io.spine.server.BoundedContext;
import org.junit.Before;
import org.junit.Test;

import static io.spine.server.entity.DefaultEntityStorageConverter.forAllFields;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
public class DefaultEntityStorageConverterShould {

    private EntityStorageConverter<Long, TestEntity, StringValue> converter;

    @Before
    public void setUp() {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .build();
        RecordBasedRepository<Long, TestEntity, StringValue> repository = new TestRepository();
        bc.register(repository);

        converter = forAllFields(repository.getEntityStateType(), repository.entityFactory());
    }

    @Test
    public void create_instance_for_for_all_fields() throws Exception {
        assertEquals(FieldMask.getDefaultInstance(), converter.getFieldMask());
    }

    @Test
    public void create_instance_with_FieldMask() throws Exception {
        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addPaths("foo.bar")
                                             .build();

        final EntityStorageConverter<Long, TestEntity, StringValue> withMasks =
                converter.withFieldMask(fieldMask);

        assertEquals(fieldMask, withMasks.getFieldMask());
    }

    private static TestEntity createEntity(Long id, StringValue state) {
        final TestEntity result = new TestEntity(id);
        result.setState(state);
        return result;
    }

    @Test
    public void convert_forward_and_backward() throws Exception {
        final TestEntity entity = createEntity(100L, Wrapper.forString("back and forth"));

        final EntityRecord out = converter.convert(entity);
        final TestEntity back = converter.reverse()
                                         .convert(out);
        assertEquals(entity, back);
    }

    @Test
    public void serialize() {
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
