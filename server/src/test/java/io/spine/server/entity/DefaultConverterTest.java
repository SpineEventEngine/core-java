/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.given.organizations.Organization;
import io.spine.server.given.organizations.OrganizationId;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.entity.DefaultConverter.forAllFields;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DefaultConverter should")
class DefaultConverterTest {

    private StorageConverter<OrganizationId, TestEntity, Organization> converter;

    @BeforeEach
    void setUp() {
        BoundedContext context =
                BoundedContextBuilder.assumingTests()
                                     .build();
        RecordBasedRepository<OrganizationId, TestEntity, Organization> repo = new TestRepository();
        context.internalAccess()
               .register(repo);

        TypeUrl stateType = repo.entityModelClass()
                                .stateTypeUrl();
        converter = forAllFields(stateType, repo.entityFactory());
    }

    @Test
    @DisplayName("create instance for all fields")
    void createForAllFields() {
        assertEquals(FieldMask.getDefaultInstance(), converter.fieldMask());
    }

    @Test
    @DisplayName("create instance with FieldMask")
    void createWithFieldMask() {
        FieldMask fieldMask = FieldMask.newBuilder()
                                       .addPaths("foo.bar")
                                       .build();

        StorageConverter<OrganizationId, TestEntity, Organization> withMasks =
                converter.withFieldMask(fieldMask);

        assertEquals(fieldMask, withMasks.fieldMask());
    }

    private static TestEntity createEntity(OrganizationId id, Organization state) {
        TestEntity result = new TestEntity(id);
        result.setState(state);
        return result;
    }

    @Test
    @DisplayName("convert forward and backward")
    void convertForwardAndBackward() {
        OrganizationId id = OrganizationId.generate();
        Organization entityState = Organization
                .newBuilder()
                .setName("back and forth")
                .setId(id)
                .vBuild();
        TestEntity entity = createEntity(id, entityState);

        EntityRecord out = converter.convert(entity);
        TestEntity back = converter.reverse()
                                   .convert(out);
        assertEquals(entity, back);
    }

    @Test
    @DisplayName("be serializable")
    void beSerializable() {
        SerializableTester.reserialize(converter);
    }

    /**
     * A test entity class which is not versionable.
     */
    private static class TestEntity extends AbstractEntity<OrganizationId, Organization> {

        private TestEntity(OrganizationId id) {
            super(id);
        }
    }

    /**
     * A test repository.
     */
    private static class TestRepository
            extends DefaultRecordBasedRepository<OrganizationId, TestEntity, Organization> {
    }
}
