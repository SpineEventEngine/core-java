/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.gcp.storage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings("InstanceMethodNamingConvention")
public class BucketFilesShould {

    private static final String BUCKET_NAME = "bucket7";
    private static final String FILE_PATH = "folder4/file_name.proto";
    private static final String FULL_PATH = BUCKET_NAME + '/' + FILE_PATH;

    @Test
    public void parse_file_path() {
        final File file = BucketFiles.ofPath(FULL_PATH);
        assertEquals(BUCKET_NAME, file.getBucket()
                                      .getValue());
        assertEquals(FILE_PATH, file.getName());
    }

    @Test
    public void fail_on_wrong_path() {
        final String fullPath = "file-path";
        try {
            BucketFiles.ofPath(fullPath);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void get_full_path_properly() {
        final Bucket bucket = Bucket.newBuilder()
                                    .setValue(BUCKET_NAME)
                                    .build();
        final File file = File.newBuilder()
                              .setBucket(bucket)
                              .setName(FILE_PATH)
                              .build();

        assertEquals(FULL_PATH, BucketFiles.getFullPath(file));
    }

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(BucketFiles.class));
    }
}
