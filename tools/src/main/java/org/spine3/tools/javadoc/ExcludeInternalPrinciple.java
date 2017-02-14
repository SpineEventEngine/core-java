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

package org.spine3.tools.javadoc;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import org.spine3.Internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class ExcludeInternalPrinciple implements ExcludePrinciple {

    private final Collection<PackageDoc> exclusions = new HashSet<>();

    ExcludeInternalPrinciple(RootDoc root) {
        exclusions.addAll(getExclusions(root.specifiedPackages()));
        exclusions.addAll(getExclusions(root.specifiedClasses()));
    }

    @Override
    public boolean exclude(Doc doc) {
        if (doc instanceof ProgramElementDoc) {
            final ProgramElementDoc programElement = (ProgramElementDoc) doc;
            return inExclusionPackage(programElement) || hasInternalAnnotation(programElement);
        }

        return false;
    }

    private boolean inExclusionPackage(ProgramElementDoc doc) {
        final String docPackageName = doc.containingPackage().name();

        for (PackageDoc exclusion : exclusions) {
            if (docPackageName.equals(exclusion.name())) {
                return true;
            }
        }

        return false;
    }

    private static Collection<PackageDoc> getExclusions(ClassDoc[] forClasses) {
        final Set<PackageDoc> exclusions = getInternalPackages(forClasses);
        final Set<PackageDoc> internalPackages = getInternalPackages(forClasses);

        for (ClassDoc classDoc : forClasses) {
            if (isInternalSubpackage(classDoc.containingPackage(), internalPackages)) {
                exclusions.add(classDoc.containingPackage());
            }
        }

        return exclusions;
    }

    private static Collection<PackageDoc> getExclusions(PackageDoc[] forPackages) {
        final Set<PackageDoc> exclusions = getInternalPackages(forPackages);
        final Set<PackageDoc> internalPackages = getInternalPackages(forPackages);

        for (PackageDoc packageDoc : forPackages) {
            if (isInternalSubpackage(packageDoc, internalPackages)) {
                exclusions.add(packageDoc);
            }
        }

        return exclusions;
    }

    private static Set<PackageDoc> getInternalPackages(PackageDoc[] forPackages) {
        final Set<PackageDoc> internalPackages = new HashSet<>();

        for (PackageDoc packageDoc : forPackages) {
            if (isInternalAnnotationPresent(packageDoc.annotations())) {
                internalPackages.add(packageDoc);
            }
        }

        return internalPackages;
    }

    private static Set<PackageDoc> getInternalPackages(ClassDoc[] forClasses) {
        final Set<PackageDoc> internalPackages = new HashSet<>();

        for (ClassDoc classDoc : forClasses) {
            if (isInternalAnnotationPresent(classDoc.containingPackage().annotations())) {
                internalPackages.add(classDoc.containingPackage());
            }
        }

        return internalPackages;
    }

    private static boolean isInternalSubpackage(PackageDoc packageDoc, Iterable<PackageDoc> internalPackages) {
        for (PackageDoc internalPackage : internalPackages) {
            if (packageDoc.name().startsWith(internalPackage.name())) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasInternalAnnotation(ProgramElementDoc doc) {
        return isInternalAnnotationPresent(doc.annotations());
    }

    private static boolean isInternalAnnotationPresent(AnnotationDesc[] annotations) {
        for (AnnotationDesc annotation : annotations) {
            if (isInternalAnnotation(annotation)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isInternalAnnotation(AnnotationDesc annotation) {
        return annotation.annotationType().qualifiedTypeName().equals(Internal.class.getName());
    }
}
