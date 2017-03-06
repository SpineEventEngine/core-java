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

package org.spine3.server.entity;

import com.google.protobuf.FieldMask;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.BoundedContext;

import static org.junit.Assert.assertEquals;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.server.entity.DefaultEntityStorageConverter.forAllFields;

/**
 * @author Alexander Yevsyukov
 */
public class DefaultEntityStorageConverterShould {

    private Repository<Long, TestEntity> repository;

    @Before
    public void setUp() {
        final BoundedContext bc = BoundedContext.newBuilder().build();
        repository = new TestRepository(bc);
        bc.register(repository);

    }

    @Test
    public void create_instance_for_for_all_fields() throws Exception {
        final EntityStorageConverter<Long, TestEntity, StringValue> converter = forAllFields(
                repository);

        assertEquals(FieldMask.getDefaultInstance(), converter.getFieldMask());
    }

    @Test
    public void create_instance_with_FieldMask() throws Exception {
        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addPaths("foo.bar")
                                             .build();

        final EntityStorageConverter<Long, TestEntity, StringValue> converter = forAllFields(
                repository).withFieldMask(fieldMask);

        assertEquals(fieldMask, converter.getFieldMask());
    }

    private static TestEntity createEntity(Long id, StringValue state) {
        final TestEntity result = new TestEntity(id);
        result.injectState(state);
        return result;
    }

    @Test
    public void convert_forward_and_backward() throws Exception {
        final TestEntity entity = createEntity(100L, newStringValue("back and forth"));

        final EntityStorageConverter<Long, TestEntity, StringValue> converter = forAllFields(
                repository);

        final EntityStorageConverter.Tuple<Long> out = converter.convert(entity);
        final TestEntity back = converter.reverse()
                                         .convert(out);
        assertEquals(entity, back);
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
        private TestRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }
}
