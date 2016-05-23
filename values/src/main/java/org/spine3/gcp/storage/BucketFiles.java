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

/**
 * Utility class for working with {@link Bucket},
 * {@link BucketContent} and {@link File}.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class BucketFiles {

    private static final String PATH_SEPARATOR = "/";

    private BucketFiles() {
    }

    public static File ofPath(String path) {
        if (!path.contains(PATH_SEPARATOR)) {
            throw new IllegalArgumentException("Illegal file path");
        }

        final int firstSeparatorIndex = path.indexOf(PATH_SEPARATOR);
        final String bucketName = path.substring(0, firstSeparatorIndex);
        final String imagePath = path.substring(firstSeparatorIndex + 1);

        final Bucket bucket = Bucket.newBuilder()
                                    .setValue(bucketName)
                                    .build();
        final File result = File.newBuilder()
                              .setBucket(bucket)
                              .setName(imagePath)
                              .build();
        return result;
    }
}
