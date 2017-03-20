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

package org.spine3.server.entity.storagefield;

import com.google.protobuf.Message;
import org.junit.Test;

import javax.annotation.Nullable;

/**
 * @author Dmytro Dashenkov.
 */
public class StorageFieldTraverserShould {

    @Test
    public void traverse_over_storage_fields() {
//        final StorageFields fields = StorageFields.newBuilder()
//                .putBooleanField("foo", true)
//                .putStringField("bar", )
    }



    private static class Traverser extends StorageFieldTraverser {

        @Override
        protected void hitMessage(String fieldName, @Nullable Message value) {

        }

        @Override
        protected void hitInteger(String fieldName, @Nullable Integer value) {

        }

        @Override
        protected void hitLong(String fieldName, @Nullable Long value) {

        }

        @Override
        protected void hitString(String fieldName, @Nullable String value) {

        }

        @Override
        protected void hitBoolean(String fieldName, @Nullable Boolean value) {

        }

        @Override
        protected void hitFloat(String fieldName, @Nullable Float value) {

        }

        @Override
        protected void hitDouble(String fieldName, @Nullable Double value) {

        }
    }
}
