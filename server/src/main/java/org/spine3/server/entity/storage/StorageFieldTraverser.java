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

package org.spine3.server.entity.storage;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.annotations.SPI;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.entity.StorageFields;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A helper type for iterating over the {@link StorageFields} objects regarding the type
 * of the entry.
 *
 * <p>This class has a set of methods {@code hitSmth}, where {@code Smth} is the type of the entry.
 * To handle the types, implement those methods. They take a {@code String} param containing
 * the name of the storage field and a {@linkplain Nullable nullable} value of the field.
 *
 * @author Dmytro Dashenkov
 */
@SPI
public abstract class StorageFieldTraverser {

    /**
     * Iterates over the given {@link StorageFields}.
     *
     * <p>The order of iteration is as follows:
     * <ol>
     *     <li>{@link Message} entries
     *     <li>{@link Integer} entries
     *     <li>{@link Long} entries
     *     <li>{@link String} entries
     *     <li>{@link Boolean} entries
     *     <li>{@link Float} entries
     *     <li>{@link Double} entries
     * </ol>
     *
     * @param fields the {@link StorageFields} to traverse
     */
    public void traverse(StorageFields fields) {
        checkNotNull(fields);

        traverseMessages(fields);
        traverseIntegers(fields);
        traverseLongs(fields);
        traverseStrings(fields);
        traverseBooleans(fields);
        traverseFloats(fields);
        traverseDoubles(fields);
    }

    protected abstract void hitMessage(String fieldName, @Nullable Message value);

    protected abstract void hitInteger(String fieldName, @Nullable Integer value);

    protected abstract void hitLong(String fieldName, @Nullable Long value);

    protected abstract void hitString(String fieldName, @Nullable String value);

    protected abstract void hitBoolean(String fieldName, @Nullable Boolean value);

    protected abstract void hitFloat(String fieldName, @Nullable Float value);

    protected abstract void hitDouble(String fieldName, @Nullable Double value);

    private void traverseMessages(StorageFields fields) {
        for (Map.Entry<String, Any> entry : fields.getAnyFieldMap().entrySet()) {
            final Message unpacked = AnyPacker.unpack(entry.getValue());
            hitMessage(entry.getKey(), unpacked);
        }
    }

    private void traverseIntegers(StorageFields fields) {
        for (Map.Entry<String, Integer> entry : fields.getIntegerFieldMap().entrySet()) {
            hitInteger(entry.getKey(), entry.getValue());
        }
    }

    private void traverseLongs(StorageFields fields) {
        for (Map.Entry<String, Long> entry : fields.getLongFieldMap().entrySet()) {
            hitLong(entry.getKey(), entry.getValue());
        }
    }

    private void traverseStrings(StorageFields fields) {
        for (Map.Entry<String, String> entry : fields.getStringFieldMap().entrySet()) {
            hitString(entry.getKey(), entry.getValue());
        }
    }

    private void traverseBooleans(StorageFields fields) {
        for (Map.Entry<String, Boolean> entry : fields.getBooleanFieldMap().entrySet()) {
            hitBoolean(entry.getKey(), entry.getValue());
        }
    }

    private void traverseFloats(StorageFields fields) {
        for (Map.Entry<String, Float> entry : fields.getFloatFieldMap().entrySet()) {
            hitFloat(entry.getKey(), entry.getValue());
        }
    }

    private void traverseDoubles(StorageFields fields) {
        for (Map.Entry<String, Double> entry : fields.getDoubleFieldMap().entrySet()) {
            hitDouble(entry.getKey(), entry.getValue());
        }
    }
}
