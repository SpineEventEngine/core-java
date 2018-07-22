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

package io.spine.model.assemble;

import io.spine.annotation.SPI;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singleton;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;

/**
 * An abstract base for the Spine
 * {@linkplain javax.annotation.processing.Processor annotation processors}.
 *
 * <p>This class provides the handy lifecycle for the processors basing on their round-oriented
 * nature.
 *
 * <p>Be sure to add the fully qualified name of your implementation of this class to
 * {@code resources/META_INF/services/javax.annotation.processing.Processor} to make it visible
 * to the compiler.
 *
 * <p>Warning: do not use standard Java {@linkplain Throwable throwables} in the methods of this
 * class to intentionally break the compilation. It may cause unreadable error messages. Use
 * {@link #error(String) error()} method instead with the error message. Use throwables only
 * to state the programming mistakes in the processor itself.
 *
 * @author Dmytro Dashenkov
 */
@SPI
public abstract class SpineAnnotationProcessor extends AbstractProcessor {

    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
        // Initialized in the synchronized `init` method.
    private Messager messager;

    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
        // Initialized in the synchronized `init` method.
    private Map<String, String> options;

    /**
     * Retrieves the supported by this processor annotation type.
     *
     * <p>Basic implementation example is:
     * <pre>
     *    {@code @Override
     *     protected Class getAnnotationType() {
     *         return Assign.class;
     *     }}
     * </pre>
     *
     * <p>Note: it's required that this method returns the same value on any invocation.
     *
     * @return the {@link Class} of the target annotation
     */
    protected abstract Class<? extends Annotation> getAnnotationType();

    /**
     * Performs the processing of the given annotated {@linkplain Element code element}.
     *
     * <p>It's guaranteed that the argument is annotated with
     * the {@linkplain #getAnnotationType() processor target annotation}.
     *
     * <p>The processing may include validation, code generation, etc.
     */
    protected abstract void processElement(Element element);

    /**
     * States if the processing of the annotation is finished or not.
     *
     * @return {@code true} if the processing of the annotation is finished, {@code false} otherwise
     * @see javax.annotation.processing.Processor#process return section for the usage
     */
    protected abstract boolean isFinished();

    /**
     * A lifecycle method called when a processing round is started.
     *
     * <p>Does nothing by default. Override this method to change the processing flow.
     */
    @SuppressWarnings("NoopMethodInAbstractClass")
    protected void onRoundStarted() {
        // NoOp
    }

    /**
     * A lifecycle method called when a processing round is finished.
     *
     * <p>Does nothing by default. Override this method to change the processing flow.
     */
    @SuppressWarnings("NoopMethodInAbstractClass")
    protected void onRoundFinished() {
        // NoOp
    }

    @Override
    public final synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.options = processingEnv.getOptions();
    }

    @Override
    public final Set<String> getSupportedAnnotationTypes() {
        Set<String> names = singleton(getAnnotationType().getName());
        return names;
    }

    @Override
    public final SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public final boolean process(Set<? extends TypeElement> annotations,
                                 RoundEnvironment roundEnv) {
        if (roundEnv.errorRaised() || roundEnv.processingOver()) {
            return true;
        }
        onRoundStarted();
        processAnnotation(getAnnotationType(), roundEnv);
        onRoundFinished();
        boolean result = isFinished();
        return result;
    }

    private void processAnnotation(Class<? extends Annotation> annotation,
                                   RoundEnvironment roundEnv) {
        Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith(annotation);
        for (Element element : annotated) {
            processElement(element);
        }
    }

    /**
     * Prints an error message.
     *
     * <p>A call to this method causes eventual compilation failure.
     *
     * @param message the error message to print
     * @see Messager#printMessage for more details
     */
    protected final void error(String message) {
        messager.printMessage(ERROR, message);
    }

    /**
     * Prints a compiler warning.
     *
     * @param message the error message to print
     * @see Messager#printMessage for more details
     */
    protected final void warn(String message) {
        messager.printMessage(WARNING, message);
    }

    /**
     * Retrieves the value of the given annotation processor option.
     *
     * <p>The options are passed to the compiler as follows: {@code javac -Akey=value [...]}.
     *
     * @param optName the name of the desired option
     * @return the value of the desired option or {@link Optional#empty() Optional.empty()} if
     *         the option is not present in the processor or has no value (i.e. is a flag option)
     */
    protected final Optional<String> getOption(String optName) {
        String optValue = options.get(optName);
        return Optional.ofNullable(optValue);
    }
}
